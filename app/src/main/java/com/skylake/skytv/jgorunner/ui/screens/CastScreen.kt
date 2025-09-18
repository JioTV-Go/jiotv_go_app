package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.core.execution.castMediaPlayer
import com.skylake.skytv.jgorunner.core.execution.crosscode
import com.skylake.skytv.jgorunner.data.SkySharedPref
import org.json.JSONException
import org.json.JSONObject
import java.net.Inet4Address

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CastScreen(context: Context, viewURL: String = "http://localhost:5350") {
    val isSessionConnected = remember { mutableStateOf(false) }
    val castContext = CastContext.getSharedInstance(context)
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))
    val prefManager = SkySharedPref.getInstance(context)
    val isProcessing = remember { mutableStateOf(false) }

    val sessionManagerListener = remember {
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) {
                isSessionConnected.value = true
            }

            override fun onSessionEnded(session: CastSession, error: Int) {
                isSessionConnected.value = false
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                isSessionConnected.value = true
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionStartFailed(session: CastSession, error: Int) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
        }
    }

    DisposableEffect(castContext) {
        val sessionManager = castContext.sessionManager
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        onDispose {
            sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        }
    }

    LaunchedEffect(Unit) {
        isSessionConnected.value = castContext.sessionManager.currentCastSession?.isConnected == true
    }

    if (isProcessing.value) {
        Dialog(onDismissRequest = { /* Prevent dismissing */ }) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentSize()
                    .background(Color.White, shape = MaterialTheme.shapes.medium)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularWavyProgressIndicator()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Processing Stream", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
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
                    style = if (isSessionConnected.value) {
                        TextStyle(
                            shadow = Shadow(
                                color = Color.Green,
                                blurRadius = 30f,
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f)
                            )
                        )

                    } else {
                        TextStyle.Default
                    },
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
                    text = if (isSessionConnected.value) "Connected" else "Not Connected",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                if (isSessionConnected.value) {
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
                        webViewClient = CustomWebViewClient(
                            context,
                            prefManager = prefManager,
                            isProcessing = isProcessing,
                            isSessionConnected = isSessionConnected
                        )
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

private class CustomWebViewClient(
    val context: Context,
    val isSessionConnected: MutableState<Boolean>,
    private val prefManager: SkySharedPref,
    private val isProcessing: MutableState<Boolean>,
) : WebViewClient() {
    private val TAG = "CustomWebViewClient"
    private val TAG2 = "CastScreen-DIX"
    private var initURL: String? = null
    private var currentPlayId: String? = null
    private var currentLogoUrl: String? = null
    private var currentChannelName: String? = null

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String, ): Boolean {
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
            Log.d(TAG2, "Modified URL for intent: $modifiedUrl")

            val newPlayerURL = formatVideoUrl(modifiedUrl)

            if (newPlayerURL != null) {
                Log.d(TAG2, newPlayerURL)
                // Cast Session Skipper
//                 if (true) {
                if (isSessionConnected.value) {
                    // Skipping CrossCode [FFMPEGKIT]
                    crosscode(
                        context = context,
                        videoUrl = newPlayerURL,
                        onProcessingStart = { isProcessing.value = true },
                        onProcessingEnd = { isProcessing.value = false }
                    )

                    // Direct Streaming - only few channels are working

//                     val ipAddress = getPublicJTVServerURL(context)
//                     fun ensureM3U8Suffix(url: String) = url.takeIf { it.endsWith(".m3u8") } ?: "$url.m3u8"
//                     val updatedUrl = ensureM3U8Suffix(newPlayerURL).replace("localhost", ipAddress)
//                     Log.d(TAG2, updatedUrl)
//                     castMediaPlayer(context, updatedUrl)


                 } else {
                    Log.d(TAG,"Not connected to any device")
                    Toast.makeText(
                        context,
                        "Not connected",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }

            return true
        } else if (!url.contains("/play/") && !url.contains("/player/")) {
            initURL = url
            return false
        }
        return false
    }

    private fun saveRecentChannel( playId: String?, logoUrl: String?, channelName: String?) {
        prefManager.myPrefs.castChannelName = channelName
        prefManager.myPrefs.castChannelLogo = logoUrl
        Log.d(TAG,"$playId")
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

        videoUrl = videoUrl.replace("//.m3u8", ".m3u8")

        return videoUrl
    }

    override fun onPageFinished(view: WebView, url: String) {
        val script = "document.querySelector('.navbar').style.display = 'none';\n" +
                "document.body.style.paddingTop = '5px';\n" +
                "document.getElementsByTagName('html')[0].setAttribute('data-theme', 'dark');\n" +
                "localStorage.setItem('theme', 'dark');"
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
}
