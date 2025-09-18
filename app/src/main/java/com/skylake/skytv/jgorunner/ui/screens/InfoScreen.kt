package com.skylake.skytv.jgorunner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InfoScreen(context: Context) {
    val preferenceManager = SkySharedPref.getInstance(context)
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val ipAddress = getIpAddress(wifiManager)
    val appVersion = getAppVersion(context)
    val binVersion = preferenceManager.myPrefs.jtvGoBinaryVersion
    val androidVersion = Build.VERSION.RELEASE
    val sdkVersion = Build.VERSION.SDK_INT
    val architecture = System.getProperty("os.arch") ?: "Unknown"
    val totalStorage = getTotalStorageSpace(context)
    val dateTime = getCurrentDateTime()
    val logContent = remember { mutableStateOf(readLogFile(context)) }
    val showLogBox = remember { mutableStateOf(false) }


    val deviceInfo = """
        JTV-GO Version: $appVersion
        Binary Version: $binVersion
        -------------------------------
        IP Address: $ipAddress
        Android Version: $androidVersion
        SDK Version: $sdkVersion
        Architecture: $architecture
        Storage Space: ${totalStorage} GB
        Date and Time: $dateTime
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "JTV-GO Server",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (!showLogBox.value) {
            // Device Info Section
            Text(
                text = "Device Info",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = deviceInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp)
                )
            }

            Button(
                onClick = {
                    copy2ToClipboard(context, deviceInfo)
                    Toast.makeText(context, "Information copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Device Info")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Toggle Button
            Button(
                onClick = { showLogBox.value = !showLogBox.value },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = if (showLogBox.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showLogBox.value) "Hide Log" else "Show Log"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showLogBox.value) "Hide Log" else "Show Log")
            }

        } else {
            // Log Section (in place of device info)
            Text(
                text = "Log File Content:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logContent.value,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        val logDir = File(context.filesDir.parent, "files")
                        val logFile = File(logDir, "jiotv_go.log")
                        if (logFile.exists()) {
                            shareLogFile(context, logFile)
                        } else {
                            Toast.makeText(context, "Log file not found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Logs")
                }

                Button(
                    onClick = {
                        val cleared = clearLogFile(context)
                        if (cleared) {
                            Toast.makeText(context, "Log file cleared", Toast.LENGTH_SHORT).show()
                            logContent.value = readLogFile(context)
                        } else {
                            Toast.makeText(context, "Failed to clear log file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Log")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Toggle Button
            Button(
                onClick = { showLogBox.value = !showLogBox.value },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = if (showLogBox.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showLogBox.value) "Hide Log" else "Show Log"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showLogBox.value) "Hide Log" else "Show Log")
            }

        }
    }
}


fun shareLogFile(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share log file via"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show()
    }
}

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unavailable"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unavailable"
    }
}

fun getIpAddress(wifiManager: WifiManager): String {
    @Suppress("deprecation")
    val ip = wifiManager.connectionInfo.ipAddress
    return if (ip != 0) {
        val bytes = byteArrayOf(
            (ip and 0xff).toByte(),
            (ip shr 8 and 0xff).toByte(),
            (ip shr 16 and 0xff).toByte(),
            (ip shr 24 and 0xff).toByte()
        )
        InetAddress.getByAddress(bytes).hostAddress ?: "Unavailable"
    } else {
        "Unavailable"
    }
}

fun getTotalStorageSpace(context: Context): Long {
    val stat = android.os.StatFs(context.filesDir.absolutePath)
    val bytesAvailable = stat.blockSizeLong * stat.blockCountLong
    return bytesAvailable / (1024 * 1024 * 1024)
}

fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

fun copy2ToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Device Info", text)
    clipboard.setPrimaryClip(clip)
}

//fun readLogFile(context: Context): String {
//    return try {
//        val logDir = File(context.filesDir.parent, "files")
//        val logFile = File(logDir, "jiotv_go.log")
//        if (logFile.exists()) {
//            logFile.readText()
//        } else {
//            "Log file not found."
//        }
//    } catch (e: Exception) {
//        "Error reading log file: ${e.localizedMessage}"
//    }
//}

fun readLogFile(context: Context): String = buildString {
    val logDir = File(context.filesDir.parent, "files")
    val logFile = File(logDir, "jiotv_go.log")
    val maxLines = 200

    try {
        when {
            logFile.exists() && logFile.length() > 0 -> {
                val lineCount = logFile.useLines { it.count() }
                val linesToShow = minOf(maxLines, lineCount)

                val queue = ArrayDeque<String>(maxLines)
                logFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    for (line in lines) {
                        if (queue.size == maxLines) queue.removeFirst()
                        queue.addLast(line)
                    }
                }
                queue.forEach { append("$it\n") }


                append("\n--- Log Preview: ")
                append("Showing last $linesToShow of $lineCount lines ")
                append("(Path: ${logFile.absolutePath})")
            }
            logFile.exists() -> append("Log file is empty")
            else -> append("Log file not found.")
        }
    } catch (e: Exception) {
        append("Error reading log (${e.javaClass.simpleName}): ")
        append(e.message?.take(200) ?: "Unknown error")
        append("\nStack trace: ${e.stackTraceToString().lines().take(5).joinToString("\n")}")
    }
}.trimEnd('\n')


fun clearLogFile(context: Context): Boolean {
    return try {
        val logDir = File(context.filesDir.parent, "files")
        val logFile = File(logDir, "jiotv_go.log")
        logFile.exists() && logFile.delete()
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
