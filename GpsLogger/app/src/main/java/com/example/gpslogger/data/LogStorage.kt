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
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        altitude = obj.optDouble("altitude", 0.0),
                        accuracy = obj.optDouble("accuracy", 0.0).toFloat(),
                        speed = obj.optDouble("speed", 0.0).toFloat(),
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
                put("latitude", entry.latitude)
                put("longitude", entry.longitude)
                put("altitude", entry.altitude)
                put("accuracy", entry.accuracy.toDouble())
                put("speed", entry.speed.toDouble())
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
