package com.skylake.skytv.jgorunner.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.skylake.skytv.jgorunner.BuildConfig
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.core.execution.BinaryExecutor
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.File

class BinaryService : Service() {
    companion object {
        // Constants
        private const val CHANNEL_ID = "BinaryServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP_BINARY: String =
            "${BuildConfig.APPLICATION_ID}.action.STOP_BINARY"
        const val ACTION_BINARY_STOPPED: String =
            "${BuildConfig.APPLICATION_ID}.action.BINARY_STOPPED"

        // Singleton instance of the BinaryService
        var instance: BinaryService? = null
            private set

        val isRunning: Boolean
            get() = instance != null

    }

    // LiveData to observe the output of the ran binary
    val binaryOutput: MutableLiveData<String> = MutableLiveData("")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Create the notification for the service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (intent.action == ACTION_STOP_BINARY) {
            stopService()
            return START_NOT_STICKY
        }

        // Prevents the service from being running again if it's already running
        if (isRunning) return START_STICKY

        // Set the singleton instance to this
        instance = this

        // Get the arguments from the intent
        var binaryFileLocation = intent.getStringExtra("binaryFileLocation")
        val arguments = intent.getStringArrayExtra("arguments")

        // Default binary file location
        if (binaryFileLocation == null) {
            val binaryFileName = SkySharedPref.getInstance(this).myPrefs.jtvGoBinaryName
            if (binaryFileName.isNullOrEmpty()) {
                Log.e("BinaryService", "Binary file location not provided.")
                stopService()
                return START_NOT_STICKY
            }
            binaryFileLocation =
                File(filesDir, binaryFileName).absolutePath
        }

        val binaryFile = File(binaryFileLocation!!)

        // Execute the binary
        BinaryExecutor.executeBinary(
            this, binaryFile, arguments,
            onOutput = { output ->
                var temp = binaryOutput.value
                if (temp == null) {
                    binaryOutput.postValue(output)
                    return@executeBinary
                }
                temp += output
                if (temp.length > 2000)
                    temp = temp.substring(2000)
                binaryOutput.postValue(temp.toString())
            },
            onError = {
                Log.e("BinaryService", "Error executing binary: ${it.errorType.name}", it)
                stopService()
            }
        )

        Log.d(
            "BinaryService",
            "BinaryService started in the background. ? " + arguments.contentToString()
        )

        return START_STICKY
    }

    private fun stopService() {
        // Stop the binary
        BinaryExecutor.stopBinary()
        Log.d("BinaryService", "Binary stopping...")

        // Broadcast that the binary has stopped
        val broadcastIntent = Intent(ACTION_BINARY_STOPPED)

        // Ensure the broadcast is internal-only
        broadcastIntent.setPackage(packageName)

        // Send the broadcast
        sendBroadcast(broadcastIntent)

        // Set the singleton instance to null
        instance = null

        @Suppress("deprecation")
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(): Notification {
        // Create the Stop Binary intent and PendingIntent
        val stopIntent = Intent(this, BinaryService::class.java)
        stopIntent.setAction(ACTION_STOP_BINARY)

        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification

        // Check for Android O and above for notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationBuilder = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("JTV-GO Service Running")
                .setContentText("The server is running in the background.")
                .setSmallIcon(R.drawable.notifications_24px)
                .setOngoing(true)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.cancel_24px),
                        "Stop Server",
                        stopPendingIntent
                    ).build()
                )
            notification = notificationBuilder.build()
        } else {
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("JTV-GO Service Running")
                .setContentText("The server is running in the background.")
                .setSmallIcon(R.drawable.notifications_24px)
                .setOngoing(true)
                .addAction(R.drawable.cancel_24px, "Stop Server", stopPendingIntent)
            notification = notificationBuilder.build()
        }

        return notification
    }


    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(
                NotificationManager::class.java
            )
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Binary Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(serviceChannel)
            }
        }
    }
}