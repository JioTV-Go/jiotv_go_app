package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastContext
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.castVideo
import com.skylake.skytv.jgorunner.activities.crosscode
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CastScreen(context: Context, viewURL: String = "http://localhost:5350") {
    val isSessionConnected = remember { mutableStateOf(false) }
    val castContext = CastContext.getSharedInstance(context)
    val session = castContext.sessionManager.currentCastSession
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))

    LaunchedEffect(Unit) {
        Log.d("DIX", "CASTSCREEN")
        isSessionConnected.value = session != null && session.isConnected
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAST",
                    fontSize = 27.sp,
                    fontFamily = customFontFamily,
                    color = Color.White,
                    modifier = Modifier.padding(top = 0.dp, bottom = 5.dp)
                )

                AndroidView(
                    factory = { context ->
                        MediaRouteButton(context).apply {
                            val mediaRouteSelector = castContext.mergedSelector
                            if (mediaRouteSelector != null) {
                                setRouteSelector(mediaRouteSelector)
                            }
                        }
                    },
                    modifier = Modifier.padding(0.dp)
                )

                Text(
                    text = if (session != null && session.isConnected) "Connected" else "Not Connected",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                if (session != null && session.isConnected) {
                    Button(
                        onClick = {
                            castContext.sessionManager.endCurrentSession(true)
                        },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Stop Casting"
                        )
                    }
                }
            }

            AndroidView(
                factory = {
                    WebView(it).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = CustomWebViewClient(context)
                        loadUrl(viewURL)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            )
        }
    }
}

private class CustomWebViewClient(val context: Context) : WebViewClient() {
    private val TAG = "CustomWebViewClient"
    private var initURL: String? = null
    private var currentPlayId: String? = null
    private var currentLogoUrl: String? = null
    private var currentChannelName: String? = null

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (url.contains("/play/")) {
            initURL = view.url
            Log.d(TAG, "Saving initURL: $initURL")

            val playId = if (url.matches(".*\\/play\\/([^\\/]+).*".toRegex())) url.replace(
                ".*\\/play\\/([^\\/]+).*".toRegex(), "$1"
            ) else null

            Log.d(TAG, playId ?: "Play ID not found")

            view.evaluateJavascript(
                "(function() { " +
                        "try { " +
                        "    var channelCard = document.querySelector('a[href*=\"/play/$playId\"]'); " +
                        "    if (channelCard) { " +
                        "        var logoElement = channelCard.querySelector('img'); " +
                        "        var nameElement = channelCard.querySelector('span'); " +
                        "        var logoUrl = logoElement ? logoElement.getAttribute('src') : null; " +
                        "        var channelName = nameElement ? nameElement.innerText : null; " +
                        "        return JSON.stringify({playId: '$playId', logoUrl: logoUrl, channelName: channelName}); " +
                        "    } else { " +
                        "        return null; " +
                        "    } " +
                        "} catch (error) { " +
                        "    return null; " +
                        "} " +
                        "})();"
            ) { result: String? ->
                if (result != null && result != "null") {
                    try {
                        val jsonString = result.replace("^\"|\"$".toRegex(), "").replace("\\\"", "\"")
                        val jsonResult = JSONObject(jsonString)
                        currentPlayId = jsonResult.getString("playId")
                        currentLogoUrl = jsonResult.getString("logoUrl")
                        currentChannelName = jsonResult.getString("channelName")

                        Log.d(TAG, "Channel Clicked: $currentChannelName (Play ID: $currentPlayId)")

                        saveRecentChannel(currentPlayId, currentLogoUrl, currentChannelName)
                    } catch (e: JSONException) {
                        Log.d(TAG, "JSON parsing error: ${e.message}")
                    }
                } else {
                    Log.d(TAG, "No channel data extracted.")
                }
            }

            val modifiedUrl = url.replace("/play/", "/live/") + ".m3u8"
            Log.d(TAG, "Modified URL for intent: $modifiedUrl")

            val newPlayerURL = formatVideoUrl(modifiedUrl)

            if (newPlayerURL != null) {
                Log.d("DIXXXXX2", newPlayerURL)
                crosscode(context, newPlayerURL)
//                castVideo(context, "http://192
            //
            //
            //
            //                .168.1.19:5350/live/high/144.m3u8")
//                castVideo(context, "http://192.168.1.10:8080")
            }

            return true
        } else if (!url.contains("/play/") && !url.contains("/player/")) {
            initURL = url
            return false
        }
        return false
    }

    private fun saveRecentChannel(playId: String?, logoUrl: String?, channelName: String?) {
        // Logic to save the recent channel data
    }

    private fun formatVideoUrl(videoUrlbase: String): String? {
        var videoUrl: String? = videoUrlbase
        if (videoUrl.isNullOrEmpty()) {
            return null
        }

        if (videoUrl.contains("q=low")) {
            videoUrl = videoUrl.replace("/live/", "/live/low/")
        } else if (videoUrl.contains("q=high")) {
            videoUrl = videoUrl.replace("/live/", "/live/high/")
        } else if (videoUrl.contains("q=medium")) {
            videoUrl = videoUrl.replace("/live/", "/live/medium/")
        }

        if (videoUrl.contains(".m3u8")) {
            val questionMarkIndex = videoUrl.indexOf("?")
            if (questionMarkIndex != -1) {
                videoUrl = videoUrl.substring(0, questionMarkIndex)
            }
        }

        videoUrl = videoUrl.replace("//.m3u8", "/.m3u8")

        Log.d("DIXr", videoUrl)

        return videoUrl
    }

    override fun onPageFinished(view: WebView, url: String) {
        val script = "document.querySelector('.navbar').style.display = 'none';"
        view.evaluateJavascript(script, null)

        view.loadUrl(
            "javascript:(function() { " +
                    "var searchButton = document.getElementById('portexe-search-button'); " +
                    "var searchInput = document.getElementById('portexe-search-input'); " +
                    "if (searchButton && searchInput) { " +
                    "  searchButton.parentNode.insertBefore(searchInput, searchButton.nextSibling); " +
                    "} " +
                    "})()"
        )
    }
}
