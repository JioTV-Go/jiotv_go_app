package com.skylake.skytv.jgorunner.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min
import kotlin.math.pow

suspend fun checkServerStatus(
    port: Int = 5350,
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit,
    onServerDown: () -> Unit,
    baseDelay: Long = 150L,
    maxDelay: Long = 2000L,
    maxAttempts: Int = 12
) {
    val url = URL("http://localhost:$port/live/143.m3u8")
    var lastResponseCode: Int? = null
    var retry500Count = 0 // Counter for 500 retries

    fun calculateDelay(attempt: Int): Long {
        val delay = baseDelay * (1.5.pow(attempt.toDouble())).toLong()
        return min(delay, maxDelay)
    }

    for (attempt in 0 until maxAttempts) {
        try {
            Log.d("ServerLoginCheck", "Attempt #${attempt + 1}: Checking server status at $url")
            val connection = (withContext(Dispatchers.IO) {
                url.openConnection()
            } as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 5000 // 5 seconds timeout
                readTimeout = 5000 // 5 seconds timeout
            }

            lastResponseCode = connection.responseCode
            connection.disconnect()

            when (lastResponseCode) {
                HttpURLConnection.HTTP_OK -> {
                    Log.d("ServerLoginCheck", "Server is up (Response Code: $lastResponseCode).")
                    onLoginSuccess()
                    return
                }
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    retry500Count++
                    Log.w(
                        "ServerLoginCheck",
                        "Server error (500) encountered. Retry count: $retry500Count"
                    )
                    if (retry500Count >= 4) {
                        Log.e(
                            "ServerLoginCheck",
                            "Max retries for 500 errors reached. Triggering login failure."
                        )
                        onLoginFailure()
                        return
                    }
                    val delayDuration = calculateDelay(retry500Count)
                    Log.d(
                        "ServerLoginCheck",
                        "Delaying for $delayDuration ms due to 500 error before the next retry."
                    )
                    delay(delayDuration) // Add delay for 500 retries
                }
                else -> {
                    Log.w("ServerLoginCheck", "Unexpected Response Code: $lastResponseCode")
                }
            }
        } catch (er: Exception) {
            Log.e(
                "ServerLoginCheck",
                "Attempt #${attempt + 1}: Error occurred while checking server status.",
                er
            )
        }

        if (attempt < maxAttempts - 1) {
            val delayDuration = calculateDelay(attempt)
            Log.d("ServerLoginCheck", "Delaying for $delayDuration ms before the next attempt.")
            delay(delayDuration)
        }
    }

    Log.e("ServerLoginCheck", "Max attempts reached. Last Response Code: $lastResponseCode")
    onServerDown()
}
