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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
            // Batch process entries in chunks of 50 to prevent Google Apps Script execution timeouts
            val chunkSize = 50
            val chunks = entries.chunked(chunkSize)

            for (chunk in chunks) {
                val uploadResult = uploadChunk(webAppUrl, chunk)
                if (uploadResult.isFailure) {
                    return@withContext uploadResult
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uploadChunk(webAppUrl: String, entries: List<GpsLogEntry>): Result<Unit> {
        try {
            val jsonArray = JSONArray()
            for (entry in entries) {
                val obj = JSONObject().apply {
                    put("timestamp", entry.timestamp)
                    put("provider", entry.provider)
                    put("latitude", entry.latitude)
                    put("longitude", entry.longitude)
                    put("altitude", entry.altitude)
                    put("accuracy", entry.accuracy)
                    put("speed", entry.speed)
                    put("bearing", entry.bearing)
                    put("speedAccuracy", entry.speedAccuracy)
                    put("bearingAccuracy", entry.bearingAccuracy)
                    put("verticalAccuracy", entry.verticalAccuracy)
                    put("elapsedRealtimeNanos", entry.elapsedRealtimeNanos)
                    put("totalSatellites", entry.totalSatellites)
                    put("satellitesInFix", entry.satellitesInFix)
                    put("constellations", entry.constellations)
                    put("rawDetails", entry.rawDetails)
                }
                jsonArray.put(obj)
            }

            val jsonString = jsonArray.toString()

            // 1. Send as form-data parameter `data` (avoiding CORS/preflight & auth issues with Apps Script)
            val formBody = FormBody.Builder()
                .add("data", jsonString)
                .build()

            val formRequest = Request.Builder()
                .url(webAppUrl)
                .post(formBody)
                .build()

            val isFormSuccess = client.newCall(formRequest).execute().use { response ->
                response.isSuccessful || response.code == 302 || response.code == 301
            }

            if (isFormSuccess) {
                return Result.success(Unit)
            }

            // 2. Fallback to raw json body if form submission returned non-200/302
            val jsonBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
            val jsonRequest = Request.Builder()
                .url(webAppUrl)
                .post(jsonBody)
                .build()

            return client.newCall(jsonRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 302 || response.code == 301) {
                    Result.success(Unit)
                } else if (response.code == 401) {
                    Result.failure(Exception("HTTP 401: Please deploy Google Apps Script with 'Who has access' set to 'Anyone'"))
                } else {
                    Result.failure(Exception("Upload failed with HTTP code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
