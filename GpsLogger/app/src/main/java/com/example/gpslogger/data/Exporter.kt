package com.example.gpslogger.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object Exporter {

    fun generateCsv(logs: List<GpsLogEntry>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Timestamp,Provider,Latitude,Longitude,Altitude (m),Accuracy (m),Speed (m/s),Bearing (deg),Speed Accuracy (m/s),Bearing Accuracy (deg),Vertical Accuracy (m),Elapsed Realtime Nanos,Satellites Total,Satellites In Fix,Constellations\n")
        
        for (entry in logs) {
            sb.append("\"").append(entry.timestamp).append("\",")
                .append("\"").append(entry.provider).append("\",")
                .append(entry.latitude).append(",")
                .append(entry.longitude).append(",")
                .append(entry.altitude).append(",")
                .append(entry.accuracy).append(",")
                .append(entry.speed).append(",")
                .append(entry.bearing).append(",")
                .append(entry.speedAccuracy).append(",")
                .append(entry.bearingAccuracy).append(",")
                .append(entry.verticalAccuracy).append(",")
                .append(entry.elapsedRealtimeNanos).append(",")
                .append(entry.totalSatellites).append(",")
                .append(entry.satellitesInFix).append(",")
                .append("\"").append(entry.constellations.replace("\"", "\"\"")).append("\"\n")
        }
        return sb.toString()
    }

    fun generateKml(logs: List<GpsLogEntry>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        sb.append("  <Document>\n")
        sb.append("    <name>GPS Raw Logger Export</name>\n")
        sb.append("    <description>GPS Log Track with detailed attribute callouts</description>\n")
        
        // Define Placemark Style using standard https icon URL
        sb.append("    <Style id=\"gpsPointStyle\">\n")
        sb.append("      <IconStyle>\n")
        sb.append("        <scale>0.8</scale>\n")
        sb.append("        <Icon>\n")
        sb.append("          <href>https://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n")
        sb.append("        </Icon>\n")
        sb.append("      </IconStyle>\n")
        sb.append("      <BalloonStyle>\n")
        sb.append("        <text><![CDATA[$[description]]]></text>\n")
        sb.append("      </BalloonStyle>\n")
        sb.append("    </Style>\n")

        sb.append("    <Style id=\"gpsLineStyle\">\n")
        sb.append("      <LineStyle>\n")
        sb.append("        <color>ff0000ff</color>\n") // Opaque Red Line (AABBGGRR)
        sb.append("        <width>4</width>\n")
        sb.append("      </LineStyle>\n")
        sb.append("    </Style>\n")

        // 1. LineString track path
        if (logs.isNotEmpty()) {
            sb.append("    <Placemark>\n")
            sb.append("      <name>GPS Track Path</name>\n")
            sb.append("      <styleUrl>#gpsLineStyle</styleUrl>\n")
            sb.append("      <LineString>\n")
            sb.append("        <tessellate>1</tessellate>\n")
            sb.append("        <altitudeMode>clampToGround</altitudeMode>\n")
            sb.append("        <coordinates>\n")
            for (entry in logs.reversed()) {
                sb.append("          ${entry.longitude},${entry.latitude},${entry.altitude}\n")
            }
            sb.append("        </coordinates>\n")
            sb.append("      </LineString>\n")
            sb.append("    </Placemark>\n")
        }

        // 2. Individual Point Placemarks with ExtendedData and HTML Balloon
        for ((index, entry) in logs.reversed().withIndex()) {
            val safeTime = entry.timestamp.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val safeConst = entry.constellations.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

            sb.append("    <Placemark>\n")
            sb.append("      <name>Point #${index + 1}</name>\n")
            sb.append("      <styleUrl>#gpsPointStyle</styleUrl>\n")
            
            // Structured ExtendedData for GIS / Google Earth
            sb.append("      <ExtendedData>\n")
            sb.append("        <Data name=\"Timestamp\"><value>${safeTime}</value></Data>\n")
            sb.append("        <Data name=\"Provider\"><value>${entry.provider}</value></Data>\n")
            sb.append("        <Data name=\"Latitude\"><value>${entry.latitude}</value></Data>\n")
            sb.append("        <Data name=\"Longitude\"><value>${entry.longitude}</value></Data>\n")
            sb.append("        <Data name=\"Altitude (m)\"><value>${entry.altitude}</value></Data>\n")
            sb.append("        <Data name=\"Accuracy (m)\"><value>${entry.accuracy}</value></Data>\n")
            sb.append("        <Data name=\"Speed (m/s)\"><value>${entry.speed}</value></Data>\n")
            sb.append("        <Data name=\"Bearing (deg)\"><value>${entry.bearing}</value></Data>\n")
            sb.append("        <Data name=\"Satellites Fix/Total\"><value>${entry.satellitesInFix}/${entry.totalSatellites}</value></Data>\n")
            sb.append("        <Data name=\"Constellations\"><value>${safeConst}</value></Data>\n")
            sb.append("      </ExtendedData>\n")

            // HTML Display Bubble Description
            sb.append("      <description><![CDATA[\n")
            sb.append("        <div style=\"font-family:sans-serif;\">\n")
            sb.append("          <h3>Point #${index + 1}</h3>\n")
            sb.append("          <p><b>Time:</b> ${safeTime}</p>\n")
            sb.append("          <table border=\"1\" cellspacing=\"0\" cellpadding=\"4\" style=\"border-collapse:collapse; font-size:12px;\">\n")
            sb.append("            <tr><td><b>Latitude</b></td><td>${entry.latitude}</td></tr>\n")
            sb.append("            <tr><td><b>Longitude</b></td><td>${entry.longitude}</td></tr>\n")
            sb.append("            <tr><td><b>Altitude</b></td><td>${entry.altitude} m</td></tr>\n")
            sb.append("            <tr><td><b>Accuracy</b></td><td>${entry.accuracy} m</td></tr>\n")
            sb.append("            <tr><td><b>Speed</b></td><td>${entry.speed} m/s (Acc: ${entry.speedAccuracy} m/s)</td></tr>\n")
            sb.append("            <tr><td><b>Bearing</b></td><td>${entry.bearing}&deg; (Acc: ${entry.bearingAccuracy}&deg;)</td></tr>\n")
            sb.append("            <tr><td><b>Vertical Accuracy</b></td><td>${entry.verticalAccuracy} m</td></tr>\n")
            sb.append("            <tr><td><b>Provider</b></td><td>${entry.provider}</td></tr>\n")
            sb.append("            <tr><td><b>Satellites (Fix/Total)</b></td><td>${entry.satellitesInFix} / ${entry.totalSatellites}</td></tr>\n")
            sb.append("            <tr><td><b>Constellations</b></td><td>${safeConst}</td></tr>\n")
            sb.append("          </table>\n")
            sb.append("        </div>\n")
            sb.append("      ]]></description>\n")
            
            sb.append("      <Point>\n")
            sb.append("        <altitudeMode>clampToGround</altitudeMode>\n")
            sb.append("        <coordinates>${entry.longitude},${entry.latitude},${entry.altitude}</coordinates>\n")
            sb.append("      </Point>\n")
            sb.append("    </Placemark>\n")
        }

        sb.append("  </Document>\n")
        sb.append("</kml>")
        return sb.toString()
    }

    fun shareFile(context: Context, fileName: String, content: String, mimeType: String) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export GPS Data ($fileName)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
