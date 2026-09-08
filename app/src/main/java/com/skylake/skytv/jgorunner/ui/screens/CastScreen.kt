package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.core.execution.castMediaPlayer
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
    val isProcessing = remember { mutableStateOf(false) }

    val sessionManagerListener = remember {
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) { isSessionConnected.value = true }
            override fun onSessionEnded(session: CastSession, error: Int) { isSessionConnected.value = false }
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) { isSessionConnected.value = true }
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
                    factory = { ctx ->
                        MediaRouteButton(ctx).apply {
                            CastButtonFactory.setUpMediaRouteButton(ctx, this)
                        }
                    }
                )

                Text(
                    text = if (isSessionConnected.value) "Connected" else "Not Connected",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                if (isSessionConnected.value) {
                    Button(
                        onClick = { castContext.sessionManager.endCurrentSession(true) },
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
                            context = context,
                            isSessionConnected = { isSessionConnected.value },
                            onProcessingChange = { isProcessing.value = it }
                        )
                        loadUrl(viewURL)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private class CustomWebViewClient(
    val context: Context,
    private val isSessionConnected: () -> Boolean,
    private val onProcessingChange: (Boolean) -> Unit,
) : WebViewClient() {
    private val TAG = "CustomWebViewClient"
    private val TAG2 = "CastScreen-JGX"
    private var initURL: String? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return handleUrlLoading(view, request.url.toString())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return handleUrlLoading(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleUrlLoading(view: WebView, url: String): Boolean {
        if (url.contains("/play/")) {
            initURL = url

            val playId = if (url.matches(".*\\/play\\/([^\\/]+).*".toRegex())) {
                url.replace(".*\\/play\\/([^\\/]+).*".toRegex(), "$1")
            } else null

            val modifiedUrl = url.replace("/play/", "/live/mpd/").substringBefore("?")
            val newPlayerURL = formatVideoUrl(modifiedUrl)

            if (newPlayerURL != null) {
                if (isSessionConnected()) {
                    val ipAddress = getPublicJTVServerURL(context)
                    val updatedUrl = newPlayerURL.replace("localhost", ipAddress).replace(".m3u8", ".mpd")

                    
                    extractChannelDataAndCast(view, playId, updatedUrl)
                } else {
                    Toast.makeText(context, "Not connected to a Cast device", Toast.LENGTH_LONG).show()

                    val ipAddress = getPublicJTVServerURL(context)
                    val updatedUrl = newPlayerURL.replace("localhost", ipAddress).replace(".m3u8", ".mpd")

                    extractChannelDataAndCast(view, playId, updatedUrl)
                    Toast.makeText(context, "Not connected to a Cast device", Toast.LENGTH_LONG).show()
                }
            }
            return true
        } else if (!url.contains("/play/") && !url.contains("/player/")) {
            initURL = url
            return false
        }
        return false
    }

    private fun extractChannelDataAndCast(view: WebView, playId: String?, castUrl: String) {
        if (playId == null) {
            
            castMediaPlayer(context, castUrl, null, null)
            return
        }

        val jsScript = """
            (function() { 
                try { 
                    var channelCard = document.querySelector('a[href*="/play/$playId"]'); 
                    if (channelCard) { 
                        var logoElement = channelCard.querySelector('img'); 
                        var nameElement = channelCard.querySelector('span'); 
                        var logoUrl = logoElement ? logoElement.getAttribute('src') : null; 
                        var channelName = nameElement ? nameElement.innerText : null; 
                        return JSON.stringify({playId: '$playId', logoUrl: logoUrl, channelName: channelName}); 
                    } 
                    return null; 
                } catch (error) { 
                    return null; 
                } 
            })();
        """.trimIndent()

        view.evaluateJavascript(jsScript) { result: String? ->
            var extractedName: String? = null
            var extractedLogo: String? = null

            if (result != null && result != "null") {
                try {
                    val jsonString = result.replace("^\"|\"$".toRegex(), "").replace("\\\"", "\"")
                    val jsonResult = JSONObject(jsonString)
                    extractedName = jsonResult.getString("channelName")
                    extractedLogo = jsonResult.getString("logoUrl")

                    Log.d(TAG, "Channel Clicked: $extractedName (Play ID: ${jsonResult.getString("playId")})")
                } catch (e: JSONException) {
                    Log.e(TAG, "JSON parsing error: ${e.message}")
                }
            }

            
            castMediaPlayer(context, castUrl, extractedName, extractedLogo)
        }
    }

    private fun formatVideoUrl(videoUrlbase: String?): String? {
        if (videoUrlbase.isNullOrEmpty()) return null
        return when {
            videoUrlbase.contains("q=low") -> videoUrlbase.replace("/live/", "/live/low/")
            videoUrlbase.contains("q=high") -> videoUrlbase.replace("/live/", "/live/high/")
            videoUrlbase.contains("q=medium") -> videoUrlbase.replace("/live/", "/live/medium/")
            else -> videoUrlbase
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        val uiScript = """
            document.querySelector('.navbar').style.display = 'none';
            document.body.style.paddingTop = '5px';
            document.getElementsByTagName('html')[0].setAttribute('data-theme', 'dark');
            localStorage.setItem('theme', 'dark');
            
            var searchButton = document.getElementById('portexe-search-button'); 
            var searchInput = document.getElementById('portexe-search-input'); 
            if (searchButton && searchInput) { 
                searchButton.parentNode.insertBefore(searchInput, searchButton.nextSibling); 
            }
        """.trimIndent()

        view.evaluateJavascript(uiScript, null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getPublicJTVServerURL(context: Context): String {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork

        if (activeNetwork != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            if (networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            ) {
                val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(activeNetwork)
                val ipAddress = linkProperties?.linkAddresses
                    ?.firstOrNull { it.address is Inet4Address }
                    ?.address?.hostAddress

                if (ipAddress != null) return ipAddress
            }
        }
        return "0.0.0.0"
    }
}