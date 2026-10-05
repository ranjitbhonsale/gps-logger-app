package com.example.gpslogger.ui.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpslogger.data.GpsLogEntry
import com.example.gpslogger.data.GpsStatusDetails
import com.example.gpslogger.service.LocationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val isLogging by viewModel.isLogging.collectAsStateWithLifecycle()
    val currentStatus by viewModel.currentStatusDetails.collectAsStateWithLifecycle()
    val sheetUrl by viewModel.sheetUrl.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocation || coarseLocation) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            Toast.makeText(context, "Location permission is required to log GPS data", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is UploadState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.dismissUploadMessage()
            }
            is UploadState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.dismissUploadMessage()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Raw Logger", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV / KML")
                    }
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Google Sheet Setup")
                    }
                    IconButton(
                        onClick = { viewModel.uploadAllLogs() },
                        enabled = uploadState !is UploadState.Uploading
                    ) {
                        if (uploadState is UploadState.Uploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload to Google Sheet")
                        }
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isLogging) {
                        val intent = Intent(context, LocationService::class.java).apply {
                            action = LocationService.ACTION_STOP
                        }
                        context.startService(intent)
                    } else {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                },
                icon = {
                    Icon(
                        if (isLogging) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isLogging) "Stop Logging" else "Start Logging"
                    )
                },
                text = { Text(if (isLogging) "STOP LOGGING" else "START LOGGING") },
                containerColor = if (isLogging) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isLogging) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // Live Satellite & GPS Status Card at Top
            SatelliteStatusCard(
                isLogging = isLogging,
                details = currentStatus,
                totalLogsCount = logs.size,
                sheetUrl = sheetUrl
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "LOG ENTRIES (${logs.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No GPS logs recorded yet.\nTap START LOGGING to begin capturing raw GPS data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(logs, key = { it.id }) { logEntry ->
                        CollapsedLogCard(entry = logEntry)
                    }
                }
            }
        }
    }

    if (showConfigDialog) {
        GoogleSheetConfigDialog(
            currentUrl = sheetUrl,
            onDismiss = { showConfigDialog = false },
            onSave = { newUrl ->
                viewModel.updateSheetUrl(newUrl)
                showConfigDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportOptionsDialog(
            logs = logs,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun SatelliteStatusCard(
    isLogging: Boolean,
    details: GpsStatusDetails,
    totalLogsCount: Int,
    sheetUrl: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLogging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SatelliteAlt,
                        contentDescription = "Satellite Details",
                        tint = if (isLogging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLogging) "GPS LOGGING ACTIVE" else "GPS LOGGING IDLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                if (sheetUrl.isNotBlank()) {
                    Text(
                        text = "Sheets Sync: ON",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(10.dp))

            // Satellite count details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Satellites in Fix / Total",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${details.satellites.satellitesInFix} / ${details.satellites.totalSatellites}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = "Constellations",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = details.satellites.constellations,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Coordinates & Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Lat: ${String.format(java.util.Locale.US, "%.6f", details.latitude)} | Lon: ${String.format(java.util.Locale.US, "%.6f", details.longitude)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Alt: ${String.format(java.util.Locale.US, "%.1f", details.altitude)}m | Speed: ${String.format(java.util.Locale.US, "%.1f", details.speed)}m/s | Acc: ${details.accuracy}m",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CollapsedLogCard(entry: GpsLogEntry) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.timestamp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lat: ${entry.latitude}, Lon: ${entry.longitude} (Acc: ${entry.accuracy}m)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Column Grid of captured metrics
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetricRow("Provider", entry.provider)
                        MetricRow("Altitude", "${entry.altitude} m")
                        MetricRow("Speed", "${entry.speed} m/s (Acc: ${entry.speedAccuracy} m/s)")
                        MetricRow("Bearing", "${entry.bearing}° (Acc: ${entry.bearingAccuracy}°)")
                        MetricRow("Vertical Acc", "${entry.verticalAccuracy} m")
                        MetricRow("Satellites (Fix/Total)", "${entry.satellitesInFix} / ${entry.totalSatellites}")
                        MetricRow("Constellations", entry.constellations)
                        MetricRow("Elapsed Realtime Nanos", "${entry.elapsedRealtimeNanos}")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ExportOptionsDialog(
    logs: List<GpsLogEntry>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export GPS Data") },
        text = {
            Column {
                Text(
                    text = "Select format to export ${logs.size} log entries:",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // CSV Export Option
                OutlinedButton(
                    onClick = {
                        if (logs.isEmpty()) {
                            Toast.makeText(context, "No log entries to export", Toast.LENGTH_SHORT).show()
                        } else {
                            val csvData = com.example.gpslogger.data.Exporter.generateCsv(logs)
                            com.example.gpslogger.data.Exporter.shareFile(
                                context,
                                "gps_logs_${System.currentTimeMillis()}.csv",
                                csvData,
                                "text/csv"
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "CSV Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Export as CSV", fontWeight = FontWeight.Bold)
                        Text("All metrics formatted in spreadsheet columns", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Single KML Export Option
                OutlinedButton(
                    onClick = {
                        if (logs.isEmpty()) {
                            Toast.makeText(context, "No log entries to export", Toast.LENGTH_SHORT).show()
                        } else {
                            val kmlData = com.example.gpslogger.data.Exporter.generateKml(logs)
                            com.example.gpslogger.data.Exporter.shareFile(
                                context,
                                "gps_track_${System.currentTimeMillis()}.kml",
                                kmlData,
                                "application/vnd.google-earth.kml+xml"
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = "KML Single Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Export Single KML File", fontWeight = FontWeight.Bold)
                        Text("Single file with full track line & downsampled point callouts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Multi-Part Split KML Export Option (100% Points)
                OutlinedButton(
                    onClick = {
                        if (logs.isEmpty()) {
                            Toast.makeText(context, "No log entries to export", Toast.LENGTH_SHORT).show()
                        } else {
                            val kmlParts = com.example.gpslogger.data.Exporter.generateKmlParts(logs, pointsPerPart = 2000)
                            com.example.gpslogger.data.Exporter.shareMultipleFiles(
                                context,
                                kmlParts,
                                "application/vnd.google-earth.kml+xml"
                            )
                            Toast.makeText(context, "Exported ${kmlParts.size} KML split parts (2000 points each)", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = "KML Multi-Part Split Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Export Split KML Parts (100% Points)", fontWeight = FontWeight.Bold)
                        Text("Splits large logs into multiple KML files (2,000 points each) to import sequentially", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun GoogleSheetConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Sheet Web App URL") },
        text = {
            Column {
                Text(
                    text = "Enter your Google Apps Script Web App URL to receive live raw GPS logs:",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: Google Apps Script can be published as a Web App (Access: Anyone) that appends JSON payloads directly into your Google Sheet.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(urlText.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
