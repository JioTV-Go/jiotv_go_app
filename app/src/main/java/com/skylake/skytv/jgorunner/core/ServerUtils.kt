package com.skylake.skytv.jgorunner.core

import android.util.Log
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL

suspend fun checkServerStatus(
    port: Int = 5350,
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit,
    onServerDown: () -> Unit,
) {
    val url = URL("http://localhost:$port/live/144.m3u8")

    repeat(5) { attempt ->
        try {
            Log.d("ServerLoginCheck", "Attempt #${attempt + 1}: Checking server status...")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000 // 5 seconds timeout
            connection.readTimeout = 5000 // 5 seconds timeout
            val responseCode = connection.responseCode
            connection.disconnect()

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    Log.d("ServerLoginCheck", "Response Code: $responseCode - Server is up!")
                    onLoginSuccess()
                    return
                }

                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    Log.d("ServerLoginCheck", "Response Code: $responseCode - Server error!")
                    onLoginFailure()
                    return
                }
                else -> {
                    Log.d("ServerLoginCheck", "Unexpected Response Code: $responseCode")
                }
            }
        } catch (ex: Exception) {
            Log.d("ServerLoginCheck", "Attempt #${attempt + 1}: Error occurred")
            Log.e(
                "ServerLoginCheck",
                "Error checking server status (attempt ${attempt + 1})",
                ex
            )
        }

        if (attempt < 2) {
            Log.d("ServerLoginCheck", "Delaying before next attempt...")
            delay(1000)
        }
    }
    Log.d("ServerLoginCheck", "Reached maximum consecutive failures.")
    onServerDown()
}