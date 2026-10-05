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

    fun generateKml(logs: List<GpsLogEntry>, maxPlacemarkPoints: Int = 1000): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        sb.append("  <Document>\n")
        sb.append("    <name>GPS Raw Logger Export</name>\n")
        sb.append("    <description>GPS Log Track path with attribute callouts</description>\n")
        
        sb.append("    <Style id=\"gpsPointStyle\">\n")
        sb.append("      <IconStyle>\n")
        sb.append("        <scale>0.7</scale>\n")
        sb.append("        <Icon>\n")
        sb.append("          <href>https://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n")
        sb.append("        </Icon>\n")
        sb.append("      </IconStyle>\n")
        sb.append("    </Style>\n")

        sb.append("    <Style id=\"gpsLineStyle\">\n")
        sb.append("      <LineStyle>\n")
        sb.append("        <color>ff0000ff</color>\n") // Red Line
        sb.append("        <width>4</width>\n")
        sb.append("      </LineStyle>\n")
        sb.append("    </Style>\n")

        val reversedLogs = logs.reversed()

        // 1. LineString track path with 100% FULL UN-DOWNSAMPLED resolution
        if (reversedLogs.isNotEmpty()) {
            sb.append("    <Placemark>\n")
            sb.append("      <name>Full Track Line Path (${logs.size} points)</name>\n")
            sb.append("      <styleUrl>#gpsLineStyle</styleUrl>\n")
            sb.append("      <LineString>\n")
            sb.append("        <tessellate>1</tessellate>\n")
            sb.append("        <altitudeMode>clampToGround</altitudeMode>\n")
            sb.append("        <coordinates>\n")
            val coords = reversedLogs.joinToString(" ") { "${it.longitude},${it.latitude},${it.altitude}" }
            sb.append("          ").append(coords).append("\n")
            sb.append("        </coordinates>\n")
            sb.append("      </LineString>\n")
            sb.append("    </Placemark>\n")
        }

        // 2. Downsample point placemarks to avoid Google Earth 10,000 feature limit error
        val step = if (reversedLogs.size > maxPlacemarkPoints) {
            Math.ceil(reversedLogs.size.toDouble() / maxPlacemarkPoints).toInt()
        } else {
            1
        }

        for (i in reversedLogs.indices step step) {
            val entry = reversedLogs[i]
            val pointNumber = i + 1
            val safeTime = escapeXml(entry.timestamp)
            val safeConst = escapeXml(entry.constellations)
            val safeProv = escapeXml(entry.provider)

            val descHtml = buildString {
                append("<div style=\"font-family:sans-serif;\">")
                append("<h3>Point #").append(pointNumber).append("</h3>")
                append("<p><b>Time:</b> ").append(safeTime).append("</p>")
                append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"4\" style=\"border-collapse:collapse; font-size:12px;\">")
                append("<tr><td><b>Latitude</b></td><td>").append(entry.latitude).append("</td></tr>")
                append("<tr><td><b>Longitude</b></td><td>").append(entry.longitude).append("</td></tr>")
                append("<tr><td><b>Altitude</b></td><td>").append(entry.altitude).append(" m</td></tr>")
                append("<tr><td><b>Accuracy</b></td><td>").append(entry.accuracy).append(" m</td></tr>")
                append("<tr><td><b>Speed</b></td><td>").append(entry.speed).append(" m/s</td></tr>")
                append("<tr><td><b>Bearing</b></td><td>").append(entry.bearing).append(" deg</td></tr>")
                append("<tr><td><b>Provider</b></td><td>").append(safeProv).append("</td></tr>")
                append("<tr><td><b>Satellites (Fix/Total)</b></td><td>").append(entry.satellitesInFix).append("/").append(entry.totalSatellites).append("</td></tr>")
                append("<tr><td><b>Constellations</b></td><td>").append(safeConst).append("</td></tr>")
                append("</table>")
                append("</div>")
            }.replace("]]>", "]]&gt;")

            sb.append("    <Placemark>\n")
            sb.append("      <name>Point ").append(pointNumber).append("</name>\n")
            sb.append("      <styleUrl>#gpsPointStyle</styleUrl>\n")

            sb.append("      <ExtendedData>\n")
            sb.append("        <Data name=\"Timestamp\"><value>").append(safeTime).append("</value></Data>\n")
            sb.append("        <Data name=\"Latitude\"><value>").append(entry.latitude).append("</value></Data>\n")
            sb.append("        <Data name=\"Longitude\"><value>").append(entry.longitude).append("</value></Data>\n")
            sb.append("        <Data name=\"Altitude (m)\"><value>").append(entry.altitude).append("</value></Data>\n")
            sb.append("        <Data name=\"Accuracy (m)\"><value>").append(entry.accuracy).append("</value></Data>\n")
            sb.append("        <Data name=\"Speed (m/s)\"><value>").append(entry.speed).append("</value></Data>\n")
            sb.append("        <Data name=\"Bearing (deg)\"><value>").append(entry.bearing).append("</value></Data>\n")
            sb.append("        <Data name=\"Provider\"><value>").append(safeProv).append("</value></Data>\n")
            sb.append("        <Data name=\"Satellites Fix/Total\"><value>").append(entry.satellitesInFix).append("/").append(entry.totalSatellites).append("</value></Data>\n")
            sb.append("        <Data name=\"Constellations\"><value>").append(safeConst).append("</value></Data>\n")
            sb.append("      </ExtendedData>\n")

            sb.append("      <description><![CDATA[").append(descHtml).append("]]></description>\n")
            sb.append("      <Point>\n")
            sb.append("        <altitudeMode>clampToGround</altitudeMode>\n")
            sb.append("        <coordinates>").append(entry.longitude).append(",").append(entry.latitude).append(",").append(entry.altitude).append("</coordinates>\n")
            sb.append("      </Point>\n")
            sb.append("    </Placemark>\n")
        }

        sb.append("  </Document>\n")
        sb.append("</kml>\n")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
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
