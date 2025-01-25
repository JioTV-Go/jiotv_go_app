package com.skylake.skytv.jgorunner.core.execution

import android.app.Activity.CONNECTIVITY_SERVICE
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.Inet4Address

fun crosscode(
    context: Context,
    videoUrl: String,
    onProcessingStart: () -> Unit,
    onProcessingEnd: () -> Unit
) {
    val TAG = "LiveHandler-DIX"
    val ipadd = getPublicJTVServerURL(context)

    fun ensureM3U8Suffix(videoUrl: String) =
        if (videoUrl.endsWith(".m3u8")) videoUrl else "$videoUrl.m3u8"

    var updatedUrl = ensureM3U8Suffix(videoUrl)
    updatedUrl = updatedUrl.replace("localhost", ipadd)

    val chromecastport = "5349"

    val ipadd2 = "http://$ipadd:$chromecastport/"
    Log.d(TAG, ipadd2)

    var vFinisher = true

    fun executeFFmpegSession() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onProcessingStart()
                var logsStopped = false
                var lastLogTime = System.currentTimeMillis()

                FFmpegKit.executeAsync(
                    "-i $updatedUrl -c copy -f mp4 -movflags frag_keyframe+empty_moov -listen 1 -bsf:a aac_adtstoasc $ipadd2",

                    { ffmpegSession ->
                        launch(Dispatchers.Main) {
                            when {
                                ReturnCode.isSuccess(ffmpegSession.returnCode) -> {
                                    Log.d(TAG, "--SUCCESS")
                                }
                                ReturnCode.isCancel(ffmpegSession.returnCode) -> {
                                    Log.d(TAG, "--CANCEL")
                                }
                                else -> {
                                    Log.d(
                                        TAG,
                                        "Command failed with state ${ffmpegSession.state} and rc ${ffmpegSession.returnCode}.${ffmpegSession.failStackTrace}"
                                    )
                                }
                            }
                        }
                    },
                    { log ->
                        Log.d("FFmpeg Log", log.message)

                        if (log.message.contains("Address already in use", ignoreCase = true)) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Port address already in use. Restarting session.",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.d(TAG, "Address already in use error detected")
                            }

                            FFmpegKit.cancel()
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(500)
                                vFinisher = false
                                executeFFmpegSession()
                            }
                            logsStopped = true
                            lastLogTime = System.currentTimeMillis()
                        }

                        logsStopped = false
                        lastLogTime = System.currentTimeMillis()
                    },
                    {}
                )

                if (vFinisher) {
                    while (true) {
                        delay(10)

                        if (!logsStopped && System.currentTimeMillis() - lastLogTime > 3000) {
                            logsStopped = true
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Playing Live Stream",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.d(TAG, "Video processing finished")
                                castMediaPlayer(context, ipadd2)
                                Log.d(TAG, "SENT CASTVIDEO")
                                onProcessingEnd()
                            }
                            break
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error executing FFmpeg command", e)
                onProcessingEnd()
            }
        }
    }

    executeFFmpegSession()
}


private fun getPublicJTVServerURL(context: Context): String {
    val connectivityManager =
        context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivityManager.activeNetwork
    } else {
        @Suppress("deprecation")
        val networks = connectivityManager.allNetworks
        if (networks.isNotEmpty()) networks[0] else null
    }

    if (activeNetwork != null) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // Check if the network is Wi-Fi or Ethernet
        if (networkCapabilities != null &&
            (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {

            val linkProperties: LinkProperties? =
                connectivityManager.getLinkProperties(activeNetwork)
            val ipAddresses = linkProperties?.linkAddresses
                ?.filter { it.address is Inet4Address } // Filter for IPv4 addresses
                ?.map { it.address.hostAddress }
            val ipAddress = ipAddresses?.firstOrNull() // Get the first IPv4 address

            if (ipAddress != null)
                return ipAddress
        }

    }

    // No active network
    return "0.0.0.0"
}

