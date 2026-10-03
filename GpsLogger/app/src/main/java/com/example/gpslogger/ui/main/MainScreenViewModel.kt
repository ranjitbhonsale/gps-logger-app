package com.example.gpslogger.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpslogger.data.GoogleSheetsUploader
import com.example.gpslogger.data.GpsLogEntry
import com.example.gpslogger.data.LogStorage
import com.example.gpslogger.service.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UploadState {
    object Idle : UploadState
    object Uploading : UploadState
    data class Success(val message: String) : UploadState
    data class Error(val message: String) : UploadState
}

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val logStorage = LogStorage(application)
    private val uploader = GoogleSheetsUploader()

    val logs: StateFlow<List<GpsLogEntry>> = logStorage.logs
    val isLogging: StateFlow<Boolean> = LocationService.isLogging

    private val _sheetUrl = MutableStateFlow(logStorage.getGoogleSheetUrl())
    val sheetUrl: StateFlow<String> = _sheetUrl.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun updateSheetUrl(url: String) {
        _sheetUrl.value = url
        logStorage.saveGoogleSheetUrl(url)
    }

    fun clearLogs() {
        logStorage.clearLogs()
    }

    fun uploadAllLogs() {
        val currentLogs = logs.value
        val url = sheetUrl.value
        if (url.isBlank()) {
            _uploadState.value = UploadState.Error("Please enter a Google Apps Script Web App URL first.")
            return
        }
        if (currentLogs.isEmpty()) {
            _uploadState.value = UploadState.Error("No GPS logs available to send.")
            return
        }

        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading
            val result = uploader.uploadEntries(url, currentLogs)
            if (result.isSuccess) {
                _uploadState.value = UploadState.Success("Successfully sent ${currentLogs.size} logs to Google Sheet!")
            } else {
                _uploadState.value = UploadState.Error(result.exceptionOrNull()?.message ?: "Upload failed.")
            }
        }
    }

    fun dismissUploadMessage() {
        _uploadState.value = UploadState.Idle
    }
}
