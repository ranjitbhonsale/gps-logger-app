package com.example.gpslogger.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleSheetsUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun uploadEntry(webAppUrl: String, entry: GpsLogEntry): Result<Unit> = withContext(Dispatchers.IO) {
        uploadEntries(webAppUrl, listOf(entry))
    }

    suspend fun uploadEntries(webAppUrl: String, entries: List<GpsLogEntry>): Result<Unit> = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Google Script Web App URL is empty"))
        }

        try {
            val jsonArray = JSONArray()
            for (entry in entries) {
                val obj = JSONObject().apply {
                    put("timestamp", entry.timestamp)
                    put("latitude", entry.latitude)
                    put("longitude", entry.longitude)
                    put("altitude", entry.altitude)
                    put("accuracy", entry.accuracy)
                    put("speed", entry.speed)
                    put("rawDetails", entry.rawDetails)
                }
                jsonArray.put(obj)
            }

            val body = jsonArray.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Upload failed with HTTP code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
