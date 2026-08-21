package com.skylake.skytv.jgorunner.utils

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.skylake.skytv.jgorunner.data.SkySharedPref
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun RememberBackPressManager(
    timeoutMs: Long = 2000L,
    onExit: () -> Unit,
    showHint: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var lastPress by remember { mutableLongStateOf(0L) }
    var active by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        val now = System.currentTimeMillis()
        if (active && now - lastPress < timeoutMs) {
            onExit()
        } else {
            active = true
            lastPress = now
            scope.launch { showHint() }
            scope.launch {
                delay(timeoutMs)
                if (System.currentTimeMillis() - lastPress >= timeoutMs) {
                    active = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { active = false }
}

@Composable
fun HandleTvBackKey(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK &&
                    keyEvent.type == KeyEventType.KeyUp
                ) {
                    onBack()
                    true
                } else {
                    false
                }
            }
    )
}

object DeviceUtils {
    fun isTvDevice(context: Context): Boolean {
        val pm: PackageManager = context.packageManager
        return try {
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        } catch (_: Exception) {
            false
        }
    }

    fun pendingIntentFlags(baseFlags: Int = 0): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            baseFlags or PendingIntent.FLAG_IMMUTABLE
        } else baseFlags
    }
}


fun withQuality(context: Context, chURL: String, logIT: Boolean = false): String {
    val skyPREF = SkySharedPref.getInstance(context).myPrefs
    var videoUrl = chURL

    logIT.takeIf { it }?.let { Log.d("wQTY", "input = $videoUrl") }
    val u = videoUrl.lowercase()
    if (u.contains("/mpd/") || u.contains(".mpd") || u.contains("/play/")) {
        return videoUrl
    }
    when (skyPREF.filterQX?.lowercase()) {
        "low" -> videoUrl = videoUrl.replace("/live/", "/live/low/")
        "high" -> videoUrl = videoUrl.replace("/live/", "/live/high/")
        "medium" -> videoUrl = videoUrl.replace("/live/", "/live/medium/")
    }
    logIT.takeIf { it }?.let { Log.d("wQTY", "output = $videoUrl") }

    return videoUrl
}

fun normalizePlaybackUrl(
    context: Context,
    inputUrl: String,
    keepPlayEndpoint: Boolean = false,
    applyQuality: Boolean = true
): String {
    val skyPref = SkySharedPref.getInstance(context).myPrefs
    var url = inputUrl.trim().trim('`', '"', '\'')

    if (url.isEmpty()) return url

    val parsedForScheme = runCatching { Uri.parse(url) }.getOrNull()
    if (parsedForScheme != null && parsedForScheme.scheme.isNullOrEmpty()) {
        val base = "http://localhost:${skyPref.jtvGoServerPort}"
        url = if (url.startsWith("/")) "$base$url" else "$base/$url"
    }

    val parsedForPlay = runCatching { Uri.parse(url) }.getOrNull()
    val playPath = parsedForPlay?.encodedPath.orEmpty()
    val playMatch = Regex(""".*/play/(\d+)$""").find(playPath)
    if (playMatch != null && !keepPlayEndpoint) {
        val id = playMatch.groupValues[1]
        val livePath = "/live/$id.m3u8"
        url = parsedForPlay?.buildUpon()?.encodedPath(livePath)?.build()?.toString() ?: url
    }

    val parsed = runCatching { Uri.parse(url) }.getOrNull()
    val effectiveQuality = if (applyQuality) {
        val qFromPref = skyPref.filterQX?.trim()?.lowercase()
        when (qFromPref) {
            "low", "medium", "high" -> qFromPref
            else -> null
        }
    } else {
        null
    }

    if (parsed != null && parsed.query?.isNotEmpty() == true &&
        (url.contains("jio.com", ignoreCase = true) || url.contains("jio.dev", ignoreCase = true) || url.contains("jiotv", ignoreCase = true) || url.contains("webplay", ignoreCase = true))
    ) {
        val builder = parsed.buildUpon().clearQuery()
        val names = runCatching { parsed.queryParameterNames }.getOrNull().orEmpty()
        for (name in names) {
            if (name.equals("q", ignoreCase = true)) continue
            val values = parsed.getQueryParameters(name)
            if (values.isEmpty()) {
                builder.appendQueryParameter(name, null)
            } else {
                values.forEach { v -> builder.appendQueryParameter(name, v) }
            }
        }
        url = builder.build().toString()
    }

    if (!effectiveQuality.isNullOrEmpty()) {
        if (url.contains("/bpk-tv/", ignoreCase = true)) {
            val qValue = when (effectiveQuality) {
                "low" -> "180"
                "medium" -> "480"
                "high" -> "1080"
                else -> null
            }
            if (qValue != null) {
                val parsedQ = Uri.parse(url)
                url = parsedQ.buildUpon().appendQueryParameter("q", qValue).build().toString()
            }
        } else {
            val u = url.lowercase()
            if (!u.contains("/mpd/") && !u.contains(".mpd") && !u.contains("/play/")) {
                val normalizedBase = url
                    .replace("/live/low/", "/live/", ignoreCase = true)
                    .replace("/live/medium/", "/live/", ignoreCase = true)
                    .replace("/live/high/", "/live/", ignoreCase = true)
                url = when (effectiveQuality) {
                    "low" -> normalizedBase.replace("/live/", "/live/low/", ignoreCase = true)
                    "high" -> normalizedBase.replace("/live/", "/live/high/", ignoreCase = true)
                    "medium" -> normalizedBase.replace("/live/", "/live/medium/", ignoreCase = true)
                    else -> normalizedBase
                }
            }
        }
    }

    val parsedAfterQuality = runCatching { Uri.parse(url) }.getOrNull()
    val path = parsedAfterQuality?.encodedPath.orEmpty()
    val isKnownProgressiveHost = url.contains("skyfilex.fun", ignoreCase = true) ||
            url.contains("datahub11.com", ignoreCase = true) ||
            url.contains("starshare", ignoreCase = true)

    // Fixed: Don't append .m3u8 if it's already an MPD or DASH stream, or a raw progressive media file, or a known progressive host.
    // Also skip /live/mpd/ paths — these are DASH manifest endpoints on the jiotv_go binary.
    if (path.contains("/live/", ignoreCase = true) &&
        !path.contains("/mpd/", ignoreCase = true) &&
        !path.endsWith(".m3u8", ignoreCase = true) &&
        !path.endsWith(".m3u", ignoreCase = true) &&
        !path.endsWith(".mpd", ignoreCase = true) &&
        !path.endsWith(".dash", ignoreCase = true) &&
        !path.endsWith(".ts", ignoreCase = true) &&
        !path.endsWith(".mp4", ignoreCase = true) &&
        !path.endsWith(".aac", ignoreCase = true) &&
        !path.endsWith(".mp3", ignoreCase = true) &&
        !path.endsWith(".mkv", ignoreCase = true) &&
        !path.endsWith(".avi", ignoreCase = true) &&
        !isKnownProgressiveHost
    ) {
        val newPath = if (path.endsWith("/")) path.dropLast(1) + ".m3u8" else "$path.m3u8"
        url = parsedAfterQuality?.buildUpon()?.encodedPath(newPath)?.build()?.toString() ?: url
    }

    url = url.replace(".m3u8.m3u8", ".m3u8", ignoreCase = true)
    url = url.replace("/.m3u8", ".m3u8", ignoreCase = true)
    return url
}

object SafeDns : okhttp3.Dns {
    private val bootstrapClient: okhttp3.OkHttpClient by lazy {
        val builder = okhttp3.OkHttpClient.Builder()
            .dns(okhttp3.Dns.SYSTEM)
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                @android.annotation.SuppressLint("CustomX509TrustManager")
                object : javax.net.ssl.X509TrustManager {
                    @android.annotation.SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    @android.annotation.SuppressLint("TrustAllX509TrustManager")
                    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }
            )
            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, java.security.SecureRandom())
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Log.e("SafeDns", "Failed to configure trust-all SSL for bootstrapClient", e)
        }
        builder.build()
    }

    override fun lookup(hostname: String): List<java.net.InetAddress> {
        if (hostname == "localhost" || hostname == "127.0.0.1" || hostname.endsWith(".local") ||
            hostname.contains("jio.com") || hostname.contains("jio.dev") || hostname.contains("jiotv")
        ) {
            return okhttp3.Dns.SYSTEM.lookup(hostname)
        }

        try {
            val addresses = okhttp3.Dns.SYSTEM.lookup(hostname)
            val isHijacked = addresses.any { addr ->
                val ip = addr.hostAddress ?: ""
                ip.startsWith("49.44.") || ip.startsWith("49.45.")
            }
            if (!isHijacked) {
                return addresses
            }
            Log.d("SafeDns", "Detected DNS hijacking (Jio blockpage IP) for $hostname. Falling back to DNS-over-HTTPS.")
        } catch (e: Exception) {
            Log.d("SafeDns", "System DNS lookup failed for $hostname. Falling back to DNS-over-HTTPS.")
        }

        val ips = mutableListOf<String>()

        try {
            val request = okhttp3.Request.Builder()
                .url("https://1.1.1.1/dns-query?name=$hostname&type=A")
                .header("Accept", "application/dns-json")
                .build()
            bootstrapClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string().orEmpty()
                    val regex = Regex("""\"data\"\s*:\s*\"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})\"""")
                    regex.findAll(json).forEach { match ->
                        val ip = match.groupValues[1]
                        if (!ip.startsWith("49.44.") && !ip.startsWith("49.45.")) {
                            ips.add(ip)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SafeDns", "Cloudflare DoH failed for $hostname: ${e.message}")
        }

        if (ips.isEmpty()) {
            try {
                val request = okhttp3.Request.Builder()
                    .url("https://8.8.8.8/resolve?name=$hostname&type=A")
                    .build()
                bootstrapClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = response.body?.string().orEmpty()
                        val regex = Regex("""\"data\"\s*:\s*\"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})\"""")
                        regex.findAll(json).forEach { match ->
                            val ip = match.groupValues[1]
                            if (!ip.startsWith("49.44.") && !ip.startsWith("49.45.")) {
                                ips.add(ip)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SafeDns", "Google DoH failed for $hostname: ${e.message}")
            }
        }

        if (ips.isNotEmpty()) {
            return ips.map { java.net.InetAddress.getByName(it) }
        }

        return try {
            okhttp3.Dns.SYSTEM.lookup(hostname)
        } catch (e: Exception) {
            throw java.net.UnknownHostException("Could not resolve $hostname via System DNS or DNS-over-HTTPS fallback: ${e.message}")
        }
    }
}

