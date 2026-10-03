package com.example.gpslogger.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class LogStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gps_logs_prefs", Context.MODE_PRIVATE)
    private val _logs = MutableStateFlow<List<GpsLogEntry>>(emptyList())
    val logs: StateFlow<List<GpsLogEntry>> = _logs.asStateFlow()

    init {
        loadLogs()
    }

    @Synchronized
    private fun loadLogs() {
        val jsonString = prefs.getString("logs_json", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<GpsLogEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    GpsLogEntry(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        timestamp = obj.getString("timestamp"),
                        provider = obj.optString("provider", "gps"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        altitude = obj.optDouble("altitude", 0.0),
                        accuracy = obj.optDouble("accuracy", 0.0).toFloat(),
                        speed = obj.optDouble("speed", 0.0).toFloat(),
                        bearing = obj.optDouble("bearing", 0.0).toFloat(),
                        speedAccuracy = obj.optDouble("speedAccuracy", 0.0).toFloat(),
                        bearingAccuracy = obj.optDouble("bearingAccuracy", 0.0).toFloat(),
                        verticalAccuracy = obj.optDouble("verticalAccuracy", 0.0).toFloat(),
                        elapsedRealtimeNanos = obj.optLong("elapsedRealtimeNanos", 0L),
                        totalSatellites = obj.optInt("totalSatellites", 0),
                        satellitesInFix = obj.optInt("satellitesInFix", 0),
                        constellations = obj.optString("constellations", "None"),
                        rawDetails = obj.optString("rawDetails", "")
                    )
                )
            }
            _logs.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            _logs.value = emptyList()
        }
    }

    @Synchronized
    fun addLog(entry: GpsLogEntry) {
        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry) // Newest first
        _logs.value = currentList
        saveLogs(currentList)
    }

    @Synchronized
    fun clearLogs() {
        _logs.value = emptyList()
        prefs.edit().remove("logs_json").apply()
    }

    private fun saveLogs(list: List<GpsLogEntry>) {
        val jsonArray = JSONArray()
        for (entry in list) {
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("timestamp", entry.timestamp)
                put("provider", entry.provider)
                put("latitude", entry.latitude)
                put("longitude", entry.longitude)
                put("altitude", entry.altitude)
                put("accuracy", entry.accuracy.toDouble())
                put("speed", entry.speed.toDouble())
                put("bearing", entry.bearing.toDouble())
                put("speedAccuracy", entry.speedAccuracy.toDouble())
                put("bearingAccuracy", entry.bearingAccuracy.toDouble())
                put("verticalAccuracy", entry.verticalAccuracy.toDouble())
                put("elapsedRealtimeNanos", entry.elapsedRealtimeNanos)
                put("totalSatellites", entry.totalSatellites)
                put("satellitesInFix", entry.satellitesInFix)
                put("constellations", entry.constellations)
                put("rawDetails", entry.rawDetails)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("logs_json", jsonArray.toString()).apply()
    }

    fun getGoogleSheetUrl(): String {
        return prefs.getString("google_sheet_url", "") ?: ""
    }

    fun saveGoogleSheetUrl(url: String) {
        prefs.edit().putString("google_sheet_url", url).apply()
    }
}
