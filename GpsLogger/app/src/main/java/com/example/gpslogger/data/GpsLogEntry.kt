package com.example.gpslogger.data

data class GpsLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speed: Float,
    val rawDetails: String
)
