package com.example.gpslogger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.gpslogger.MainActivity
import com.example.gpslogger.R
import com.example.gpslogger.data.GoogleSheetsUploader
import com.example.gpslogger.data.GpsLogEntry
import com.example.gpslogger.data.LogStorage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var logStorage: LogStorage
    private val uploader = GoogleSheetsUploader()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.getDefault())

    companion object {
        private const val CHANNEL_ID = "GpsLoggerChannel"
        private const val NOTIFICATION_ID = 1001

        private val _isLogging = MutableStateFlow(false)
        val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        logStorage = LogStorage(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    processLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLogging()
            ACTION_STOP -> stopLogging()
        }
        return START_STICKY
    }

    private fun startLogging() {
        if (_isLogging.value) return

        createNotificationChannel()
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isLogging.value = true

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopLogging()
        }
    }

    private fun processLocation(location: Location) {
        val timeString = dateFormat.format(Date(location.time))
        val rawDetails = buildString {
            append("Provider: ").append(location.provider).append("\n")
            append("Lat: ").append(location.latitude).append("\n")
            append("Lon: ").append(location.longitude).append("\n")
            append("Alt: ").append(location.altitude).append(" m\n")
            append("Accuracy: ").append(location.accuracy).append(" m\n")
            append("Speed: ").append(location.speed).append(" m/s\n")
            append("Bearing: ").append(location.bearing).append(" deg\n")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                append("SpeedAcc: ").append(location.speedAccuracyMetersPerSecond).append(" m/s\n")
                append("BearingAcc: ").append(location.bearingAccuracyDegrees).append(" deg\n")
                append("VertAcc: ").append(location.verticalAccuracyMeters).append(" m\n")
            }
            append("Elapsed Realtime Nanos: ").append(location.elapsedRealtimeNanos)
        }

        val entry = GpsLogEntry(
            timestamp = timeString,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            speed = location.speed,
            rawDetails = rawDetails
        )

        // Store log locally
        logStorage.addLog(entry)

        // If Google Sheet URL is set, auto sync new log entry
        val sheetUrl = logStorage.getGoogleSheetUrl()
        if (sheetUrl.isNotBlank()) {
            serviceScope.launch {
                uploader.uploadEntry(sheetUrl, entry)
            }
        }
    }

    private fun stopLogging() {
        if (!_isLogging.value) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isLogging.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Logger Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notification while GPS logging is active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Logging Active")
            .setContentText("Recording raw GPS coordinates and timestamp data...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        _isLogging.value = false
    }
}
