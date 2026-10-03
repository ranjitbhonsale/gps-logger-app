package com.example.gpslogger.data

data class SatelliteInfo(
    val totalSatellites: Int = 0,
    val satellitesInFix: Int = 0,
    val constellations: String = "None"
)

data class GpsStatusDetails(
    val provider: String = "Unknown",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val speed: Float = 0.0f,
    val bearing: Float = 0.0f,
    val verticalAccuracy: Float = 0.0f,
    val speedAccuracy: Float = 0.0f,
    val bearingAccuracy: Float = 0.0f,
    val satellites: SatelliteInfo = SatelliteInfo()
)

data class GpsLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val provider: String = "gps",
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float = 0.0f,
    val speedAccuracy: Float = 0.0f,
    val bearingAccuracy: Float = 0.0f,
    val verticalAccuracy: Float = 0.0f,
    val elapsedRealtimeNanos: Long = 0L,
    val totalSatellites: Int = 0,
    val satellitesInFix: Int = 0,
    val constellations: String = "None",
    val rawDetails: String = ""
)
