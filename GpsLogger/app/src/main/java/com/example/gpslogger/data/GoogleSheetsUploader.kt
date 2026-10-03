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
                return@withContext Result.success(Unit)
            }

            // 2. Fallback to raw json body if form submission returned non-200/302
            val jsonBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
            val jsonRequest = Request.Builder()
                .url(webAppUrl)
                .post(jsonBody)
                .build()

            client.newCall(jsonRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 302 || response.code == 301) {
                    Result.success(Unit)
                } else if (response.code == 401) {
                    Result.failure(Exception("HTTP 401: Please deploy Google Apps Script with 'Who has access' set to 'Anyone'"))
                } else {
                    Result.failure(Exception("Upload failed with HTTP code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
