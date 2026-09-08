package com.skylake.skytv.jgorunner.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.Executors

object LogCollector {
    
    private const val MAX_LOGS = 250
    private val logs = ArrayDeque<String>(MAX_LOGS + 1)
    private var logFile: File? = null

    
    private val ioExecutor = Executors.newSingleThreadExecutor()

    fun init(context: Context) {
        if (logFile != null) return
        logFile = File(context.cacheDir, "app_logs.txt")

        
        ioExecutor.execute {
            if (logFile?.exists() == true) {
                try {
                    val recentLogs = ArrayDeque<String>(MAX_LOGS)
                    logFile!!.useLines { sequence ->
                        sequence.filter { it.isNotBlank() }.forEach { line ->
                            if (recentLogs.size == MAX_LOGS) {
                                recentLogs.removeFirst() 
                            }
                            recentLogs.addLast(line)
                        }
                    }

                    synchronized(logs) {
                        logs.clear()
                        logs.addAll(recentLogs)
                    }
                } catch (e: Exception) {
                    Log.e("LogCollector", "Failed to read logs from file", e)
                }
            }
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logError("FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val formattedLog = "[$timestamp] $message"

        synchronized(logs) {
            logs.addFirst(formattedLog)
            if (logs.size > MAX_LOGS) {
                logs.removeLast() 
            }
        }

        
        ioExecutor.execute {
            saveToFile()
        }
    }

    fun logError(message: String, throwable: Throwable?) {
        log("ERROR: $message")
        throwable?.let {
            log("CAUSE: ${it.message}")
            log("STACKTRACE: ${Log.getStackTraceString(it)}")
        }
    }

    private fun saveToFile() {
        val file = logFile ?: return
        try {
            
            val logSnapshot = synchronized(logs) { logs.toList() }
            file.writeText(logSnapshot.joinToString("\n"))
        } catch (e: Exception) {
            Log.e("LogCollector", "Failed to write logs to file", e)
        }
    }

    fun clear() {
        synchronized(logs) {
            logs.clear()
        }
        ioExecutor.execute {
            try {
                logFile?.delete()
            } catch (_: Exception) {}
        }
    }

    fun getLogs(): String {
        return synchronized(logs) {
            logs.joinToString("\n")
        }
    }

    fun copyToClipboard(context: Context) {
        val logText = getLogs()
        if (logText.isEmpty()) {
            Toast.makeText(context, "No logs to copy", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SkyTV Logs", logText)
            clipboard.setPrimaryClip(clip)

            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
                Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}