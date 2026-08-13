package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.skylake.skytv.jgorunner.activities.MainActivity
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.data.OmniFavoritesStore
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import com.skylake.skytv.jgorunner.ui.components.OmniFilterDialog
import com.skylake.skytv.jgorunner.utils.LogCollector
import com.skylake.skytv.jgorunner.utils.DeviceUtils
import com.skylake.skytv.jgorunner.utils.cleanupPlaybackLogic
import com.skylake.skytv.jgorunner.utils.setupCustomPlaybackLogic
import com.skylake.skytv.jgorunner.utils.SafeDns
import com.skylake.skytv.jgorunner.utils.OmniMediaDrmCallback
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private const val OMNI_TAG = "OmniPlayerScreen"

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@Composable
fun OmniPlayerScreen(
    preferenceManager: SkySharedPref,
    channelList: List<OmniChannel>,
    initialIndex: Int,
    serverUrl: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val okHttpClient = remember {
        val androidId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""

        val builder = OkHttpClient.Builder()
            .connectTimeout(35, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .dns(SafeDns)
            .followRedirects(false)
            .followSslRedirects(false)

        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                @SuppressLint("CustomX509TrustManager")
                object : javax.net.ssl.X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    @SuppressLint("TrustAllX509TrustManager")
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
            Log.e("OmniPlayerScreen", "Failed to configure trust-all SSL", e)
        }

        builder.addInterceptor { chain ->
            var request = chain.request()
            val url = request.url.toString()
            val reqBuilder = request.newBuilder()

            val jioUA = "JioTV/7.0.8 (Linux; Android 13; Pixel 7 Pro Build/TQ1A.221205.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/110.0.5481.64 Mobile Safari/537.36"

            if (url.contains("jio.com", true) || url.contains("jio.dev", true) || url.contains("webplay.fun", true) || url.contains("jiotv.jio.com", true)) {
                if (request.header("User-Agent").isNullOrBlank()) reqBuilder.header("User-Agent", jioUA)
                if (request.header("os").isNullOrBlank()) reqBuilder.header("os", "android")
                if (request.header("devicetype").isNullOrBlank()) reqBuilder.header("devicetype", "phone")
                if (request.header("uniqueId").isNullOrBlank()) reqBuilder.header("uniqueId", androidId)
                if (request.header("deviceId").isNullOrBlank()) reqBuilder.header("deviceId", androidId)
                if (request.header("appname").isNullOrBlank()) reqBuilder.header("appname", "com.jio.jiotv")
                if (request.header("versionCode").isNullOrBlank()) reqBuilder.header("versionCode", "323")
                if (request.header("X-Jio-Network-Type").isNullOrBlank()) reqBuilder.header("X-Jio-Network-Type", "WIFI")
                if (request.header("X-Requested-With").isNullOrBlank()) reqBuilder.header("X-Requested-With", "com.jio.jiotv")
                if (request.header("Origin").isNullOrBlank()) reqBuilder.header("Origin", "https://jiotv.jio.com")
                if (request.header("Referer").isNullOrBlank()) reqBuilder.header("Referer", "https://jiotv.jio.com/")
            }

            request = reqBuilder.build()

            val referer = request.header("Referer")
            val origin = request.header("Origin")
            val userAgent = request.header("User-Agent")
            val cookie = request.header("Cookie")
            val xRequestedWith = request.header("X-Requested-With")

            var response = chain.proceed(request)
            var tryCount = 0
            while (response.isRedirect && tryCount < 10) {
                var newUrl = response.header("Location") ?: break
                if (!newUrl.startsWith("http://", ignoreCase = true) && !newUrl.startsWith("https://", ignoreCase = true)) {
                    try {
                        val baseHttpUrl = request.url
                        val resolved = baseHttpUrl.resolve(newUrl)
                        if (resolved != null) {
                            newUrl = resolved.toString()
                        }
                    } catch (_: java.lang.Exception) {}
                }
                response.close()

                val newReqBuilder = request.newBuilder().url(newUrl)
                if (!referer.isNullOrBlank()) newReqBuilder.header("Referer", referer)
                if (!origin.isNullOrBlank()) newReqBuilder.header("Origin", origin)
                if (!userAgent.isNullOrBlank()) newReqBuilder.header("User-Agent", userAgent)
                if (!cookie.isNullOrBlank()) newReqBuilder.header("Cookie", cookie)
                if (!xRequestedWith.isNullOrBlank()) newReqBuilder.header("X-Requested-With", xRequestedWith)
                request = newReqBuilder.build()
                response = chain.proceed(request)
                tryCount++
            }
            response
        }
        .build()
    }

    var activeList by remember { mutableStateOf(channelList) }
    var currentIndex by remember(initialIndex) { mutableIntStateOf(initialIndex) }
    var activeChannel by remember(currentIndex) {
        mutableStateOf(activeList.getOrNull(currentIndex))
    }

    val rootFocusRequester = remember { FocusRequester() }
    val overlayFocusRequester = remember { FocusRequester() }
    val seekBarFocusRequester = remember { FocusRequester() }
    val sidePanelFocusRequester = remember { FocusRequester() }
    val settingsPanelFocusRequester = remember { FocusRequester() }

    var showChannelPanel by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }

    // Selected subtitle label ("Off" when none). Resets per channel (keyed on currentIndex).
    var selectedSubtitleLabel by remember(currentIndex) { mutableStateOf("Off") }
    // Selected audio-track label (null → auto/first). Resets per channel.
    var selectedAudioLabel by remember(currentIndex) { mutableStateOf<String?>(null) }
    // User-selected playback speed (resets to 1.0f on video/channel change).
    var currentPlaybackSpeed by remember(currentIndex) { mutableFloatStateOf(1.0f) }

    // Double-tap / dpad seek indicator (YouTube-style "+10s" / "-10s" flash).
    var seekIndicatorForward by remember { mutableStateOf(true) }
    var seekIndicatorSeconds by remember { mutableIntStateOf(0) }
    var showSeekIndicator by remember { mutableStateOf(false) }
    var seekIndicatorJob by remember { mutableStateOf<Job?>(null) }

    var panelSelectedIndex by remember { mutableIntStateOf(currentIndex) }
    var showChannelOverlay by remember { mutableStateOf(false) }
    var overlayVisibilityTick by remember { mutableLongStateOf(0L) }
    var playerError by remember { mutableStateOf<String?>(null) }

    var numericBuffer by remember { mutableStateOf("") }
    var showNumericOverlay by remember { mutableStateOf(false) }
    var numericJob by remember { mutableStateOf<Job?>(null) }

    var useDrm by remember(currentIndex) { mutableStateOf(true) }
    var playbackTrigger by remember(currentIndex) { mutableIntStateOf(0) }
    var drmRetryCount by remember(currentIndex) { mutableIntStateOf(0) }
    var catchupRetryCount by remember(currentIndex) { mutableIntStateOf(0) }

    val isMovieOrVod = remember(activeChannel) {
        val ch = activeChannel ?: return@remember false
        ch.name?.contains("[Catchup]", ignoreCase = true) == true || ch.url?.contains("/catchup/") == true
    }

    var showController by remember { mutableStateOf(true) }
    var controllerTimeoutJob by remember { mutableStateOf<Job?>(null) }

    // Auto Hide Controller Logic
    fun triggerControllerTimeout() {
        showController = true
        controllerTimeoutJob?.cancel()
        controllerTimeoutJob = scope.launch {
            delay(5000)
            showController = false
        }
    }

    LaunchedEffect(Unit) {
        triggerControllerTimeout()
    }

    DisposableEffect(Unit) {
        onDispose {
            controllerTimeoutJob?.cancel()
        }
    }

    // DRM media builder for Free Jio mechanisms
    fun buildOmniMediaItem(ch: OmniChannel, forceDrm: Boolean): MediaItem {
        val licenseUrl = ch.licenseUrl
        val isMpd = ch.mpdUrl != null || ch.url?.contains(".mpd") == true

        val builder = MediaItem.Builder()

        if (!licenseUrl.isNullOrBlank()) {
            builder.setUri(ch.mpdUrl ?: ch.url ?: "")
            builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(licenseUrl)
                    .apply {
                        ch.headers?.let { setLicenseRequestHeaders(it) }
                    }
                    .build()
            )
            builder.setMimeType(MimeTypes.APPLICATION_MPD)
        } else {
            val targetUrl = if (forceDrm) {
                val base = ch.m3u8Url ?: ch.url ?: ""
                base.replace("/live/mpd/", "/live/mpd/").replace("/live/", "/live/mpd/").replace(".m3u8", ".mpd")
            } else {
                val base = ch.m3u8Url ?: ch.url ?: ""
                base.replace("/live/mpd/", "/live/").replace(".mpd", ".m3u8")
            }
            builder.setUri(targetUrl)
            if (forceDrm || isMpd) {
                val calculatedLicense = targetUrl.replace("/live/mpd/", "/live/key/").replace(".mpd", "")
                builder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(calculatedLicense)
                        .build()
                )
                builder.setMimeType(MimeTypes.APPLICATION_MPD)
            } else {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
        }
        return builder.build()
    }

    var currentResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    val trackSelector = remember { DefaultTrackSelector(context) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(OMNI_TAG, "ExoPlayer error: ${error.message}")

                        val catchupWebUrl = activeChannel?.headers?.get("catchup_web_url")
                        val isCatchupStream = activeChannel?.name?.contains("[Catchup]", ignoreCase = true) == true &&
                                              !catchupWebUrl.isNullOrBlank()

                        if (isCatchupStream) {
                            catchupRetryCount++
                            if (catchupRetryCount >= 3) {
                                Log.w(OMNI_TAG, "Catchup native playback failed 3 times. Redirecting to WebPlayer fallback: $catchupWebUrl")
                                try {
                                    val intent = Intent(context, com.skylake.skytv.jgorunner.activities.WebPlayerActivity::class.java).apply {
                                        putExtra("startup_url", catchupWebUrl)
                                        putExtra("target_channel_id", activeChannel?.id ?: "")
                                    }
                                    context.startActivity(intent)
                                    (context as? Activity)?.finish()
                                } catch (e: Exception) {
                                    Log.e(OMNI_TAG, "Failed to fallback to WebPlayerActivity: ${e.message}", e)
                                }
                                return
                            } else {
                                Log.d(OMNI_TAG, "Catchup failed $catchupRetryCount time(s), retrying...")
                                prepare()
                                play()
                                return
                            }
                        }

                        if (useDrm) {
                            drmRetryCount++
                            if (drmRetryCount >= 2) {
                                Log.w(OMNI_TAG, "DRM failed 2 times, falling back to HLS")
                                useDrm = false
                            } else {
                                Log.d(OMNI_TAG, "DRM failed $drmRetryCount time(s), retrying...")
                                prepare()
                                play()
                            }
                        } else {
                            playerError = error.message ?: "Playback Error"
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            playerError = null
                        }
                    }
                })
            }
    }

    LaunchedEffect(currentIndex, useDrm, playbackTrigger) {
        val ch = activeList.getOrNull(currentIndex) ?: return@LaunchedEffect
        activeChannel = ch
        playerError = null
        try {
            val normalizedHeaders = mutableMapOf<String, String>()
            normalizedHeaders["Accept"] = "*/*"
            normalizedHeaders["Connection"] = "keep-alive"
            
            val jioUA = "JioTV/7.0.8 (Linux; Android 13; Pixel 7 Pro Build/TQ1A.221205.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/110.0.5481.64 Mobile Safari/537.36"
            val androidId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""
            normalizedHeaders["User-Agent"] = jioUA
            normalizedHeaders["Origin"] = "https://jiotv.jio.com"
            normalizedHeaders["Referer"] = "https://jiotv.jio.com/"
            normalizedHeaders["X-Requested-With"] = "com.jio.jiotv"
            
            ch.headers?.forEach { (k, v) ->
                normalizedHeaders[k] = v
            }

            var resolvedLicenseUrl = ch.licenseUrl
            var playbackUrl = ch.mpdUrl ?: ch.m3u8Url ?: ch.url ?: ""
            
            val isMpd = ch.mpdUrl != null || ch.url?.contains(".mpd") == true
            if (resolvedLicenseUrl.isNullOrBlank()) {
                val targetUrl = if (useDrm) {
                    playbackUrl.replace("/live/mpd/", "/live/mpd/").replace("/live/", "/live/mpd/").replace(".m3u8", ".mpd")
                } else {
                    playbackUrl.replace("/live/mpd/", "/live/").replace(".mpd", ".m3u8")
                }
                playbackUrl = targetUrl
                if (useDrm || isMpd) {
                    resolvedLicenseUrl = playbackUrl.replace("/live/mpd/", "/live/key/").replace(".mpd", "")
                }
            }

            val builder = MediaItem.Builder()
                .setUri(playbackUrl.toUri())
                .setMediaId(ch.id ?: "")

            val isDrm = !resolvedLicenseUrl.isNullOrBlank()
            if (isDrm) {
                builder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(resolvedLicenseUrl)
                        .setLicenseRequestHeaders(normalizedHeaders)
                        .setMultiSession(true)
                        .build()
                )
                builder.setMimeType(MimeTypes.APPLICATION_MPD)
            } else {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            val mediaItem = builder.build()

            val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            dataSourceFactory.setDefaultRequestProperties(normalizedHeaders)
            val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, dataSourceFactory)

            var drmSessionManager: DefaultDrmSessionManager? = null
            if (isDrm) {
                val drmCallback = OmniMediaDrmCallback(resolvedLicenseUrl!!, normalizedHeaders, okHttpClient)
                drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .setMultiSession(true)
                    .build(drmCallback)
            }

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(defaultDataSourceFactory)
            if (drmSessionManager != null) {
                mediaSourceFactory.setDrmSessionManagerProvider { drmSessionManager }
            }

            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            exoPlayer.stop()
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            triggerControllerTimeout()
        } catch (e: Exception) {
            Log.e(OMNI_TAG, "Failed to prepare playback", e)
            playerError = e.message ?: "Prepare Failed"
        }
    }

    LaunchedEffect(currentPlaybackSpeed) {
        try {
            exoPlayer.setPlaybackSpeed(currentPlaybackSpeed)
        } catch (_: Exception) {}
    }

    LaunchedEffect(preferenceManager.myPrefs.omniQualityMaxHeight) {
        val maxHeight = preferenceManager.myPrefs.omniQualityMaxHeight
        val resolvedHeight = if (maxHeight <= 0) Int.MAX_VALUE else maxHeight
        trackSelector.setParameters(
            trackSelector.buildUponParameters().setMaxVideoSize(Int.MAX_VALUE, resolvedHeight)
        )
    }

    // Audio & Subtitles selector logic
    fun getAudioTrackOptions(player: ExoPlayer): List<Pair<String, String>> {
        val options = mutableListOf<Pair<String, String>>()
        try {
            val groups = player.currentTracks.groups
            for (i in 0 until groups.size) {
                val group = groups[i]
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (j in 0 until group.length) {
                        val format = group.getTrackFormat(j)
                        val label = format.label ?: format.language ?: "Audio ${options.size + 1}"
                        options.add(label to (format.language ?: ""))
                    }
                }
            }
        } catch (_: Exception) {}
        return options.distinctBy { it.first }
    }

    // Subtitle tracks options
    fun getSubtitleTrackOptions(player: ExoPlayer): List<Pair<String, String>> {
        val options = mutableListOf<Pair<String, String>>()
        try {
            val groups = player.currentTracks.groups
            for (i in 0 until groups.size) {
                val group = groups[i]
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (j in 0 until group.length) {
                        val format = group.getTrackFormat(j)
                        val label = format.label ?: format.language ?: "Subtitle ${options.size + 1}"
                        options.add(label to (format.language ?: ""))
                    }
                }
            }
        } catch (_: Exception) {}
        return options.distinctBy { it.first }
    }

    val favoriteStore = remember { OmniFavoritesStore(preferenceManager) }
    val isTv = remember { com.skylake.skytv.jgorunner.utils.DeviceUtils.isTvDevice(context) }

    // Mobile Swipe Gestures
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }
    var swipeVolumeValue by remember { mutableFloatStateOf(0f) }
    var swipeBrightnessValue by remember { mutableFloatStateOf(0.5f) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var dragSideIsLeft by remember { mutableStateOf(false) }
    var gestureIndicatorJob by remember { mutableStateOf<Job?>(null) }
    val enableSwipeGestures = true

    val swipeModifier = if (isTv || !enableSwipeGestures) Modifier else Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { offset ->
                dragSideIsLeft = offset.x < (size.width / 2)
                if (dragSideIsLeft) {
                    val act = context as? Activity
                    val lp = act?.window?.attributes
                    val currentBrightness = if (lp != null && lp.screenBrightness >= 0f) lp.screenBrightness else 0.5f
                    swipeBrightnessValue = currentBrightness
                    showBrightnessIndicator = true
                    showVolumeIndicator = false
                } else {
                    val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                    swipeVolumeValue = currentVol / maxVolume
                    showVolumeIndicator = true
                    showBrightnessIndicator = false
                }
                gestureIndicatorJob?.cancel()
            },
            onDragEnd = {
                gestureIndicatorJob = scope.launch {
                    delay(1500)
                    showVolumeIndicator = false
                    showBrightnessIndicator = false
                }
            },
            onVerticalDrag = { change, dragAmount ->
                gestureIndicatorJob?.cancel()
                val screenHeightPx = size.height.toFloat()
                val delta = -dragAmount / screenHeightPx
                if (dragSideIsLeft) {
                    swipeBrightnessValue = (swipeBrightnessValue + delta).coerceIn(0f, 1f)
                    val act = context as? Activity
                    val lp = act?.window?.attributes
                    if (lp != null) {
                        lp.screenBrightness = swipeBrightnessValue
                        act.runOnUiThread {
                            act.window.attributes = lp
                        }
                    }
                    showBrightnessIndicator = true
                } else {
                    swipeVolumeValue = (swipeVolumeValue + delta).coerceIn(0f, 1f)
                    val targetVol = (swipeVolumeValue * maxVolume).toInt().coerceIn(0, maxVolume.toInt())
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVol, 0)
                    showVolumeIndicator = true
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Picture in Picture controls command mapping
    DisposableEffect(activeList, currentIndex, exoPlayer) {
        com.skylake.skytv.jgorunner.services.player.PlayerCommandBus.setHandlers(
            playPause = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            next = {
                if (activeList.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % activeList.size
                }
            },
            prev = {
                if (activeList.isNotEmpty()) {
                    currentIndex = (currentIndex - 1 + activeList.size) % activeList.size
                }
            },
            isPlaying = { exoPlayer.isPlaying }
        )
        onDispose {
            com.skylake.skytv.jgorunner.services.player.PlayerCommandBus.clearHandlers()
        }
    }

    BackHandler {
        when {
            showChannelPanel -> showChannelPanel = false
            showSettingsPanel -> showSettingsPanel = false
            showChannelOverlay -> showChannelOverlay = false
            else -> (context as? Activity)?.finish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(swipeModifier)
            .pointerInput(isMovieOrVod) {
                detectTapGestures(
                    onTap = {
                        showChannelOverlay = !showChannelOverlay
                        if (showChannelOverlay) {
                            overlayVisibilityTick = System.currentTimeMillis()
                            triggerControllerTimeout()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isMovieOrVod) return@detectTapGestures
                        val screenWidth = size.width
                        val doubleTapLeft = offset.x < (screenWidth / 2)
                        val seekDelta = if (doubleTapLeft) -10000L else 10000L
                        
                        seekIndicatorForward = !doubleTapLeft
                        seekIndicatorSeconds = 10
                        showSeekIndicator = true
                        
                        seekIndicatorJob?.cancel()
                        seekIndicatorJob = scope.launch {
                            delay(1200)
                            showSeekIndicator = false
                        }

                        val targetPos = (exoPlayer.currentPosition + seekDelta).coerceIn(0L, exoPlayer.duration)
                        exoPlayer.seekTo(targetPos)
                    }
                )
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    triggerControllerTimeout()
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (!showChannelPanel && !showSettingsPanel && !showChannelOverlay) {
                                showChannelPanel = true
                                try { sidePanelFocusRequester.requestFocus() } catch (_: Exception) {}
                                return@onPreviewKeyEvent true
                            }
                        }
                        Key.DirectionRight -> {
                            if (!showChannelPanel && !showSettingsPanel && !showChannelOverlay) {
                                showSettingsPanel = true
                                try { settingsPanelFocusRequester.requestFocus() } catch (_: Exception) {}
                                return@onPreviewKeyEvent true
                            }
                        }
                        Key.DirectionUp -> {
                            if (!showChannelPanel && !showSettingsPanel && !showChannelOverlay && activeList.isNotEmpty()) {
                                currentIndex = (currentIndex + 1) % activeList.size
                                return@onPreviewKeyEvent true
                            }
                        }
                        Key.DirectionDown -> {
                            if (!showChannelPanel && !showSettingsPanel && !showChannelOverlay && activeList.isNotEmpty()) {
                                currentIndex = (currentIndex - 1 + activeList.size) % activeList.size
                                return@onPreviewKeyEvent true
                            }
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!showChannelPanel && !showSettingsPanel && !showChannelOverlay) {
                                showChannelOverlay = true
                                overlayVisibilityTick = System.currentTimeMillis()
                                try { overlayFocusRequester.requestFocus() } catch (_: Exception) {}
                                return@onPreviewKeyEvent true
                            }
                        }
                    }

                    val digit = when (event.key) {
                        Key.Zero -> 0; Key.One -> 1; Key.Two -> 2; Key.Three -> 3; Key.Four -> 4
                        Key.Five -> 5; Key.Six -> 6; Key.Seven -> 7; Key.Eight -> 8; Key.Nine -> 9
                        else -> null
                    }
                    if (digit != null) {
                        numericBuffer += digit.toString()
                        showNumericOverlay = true
                        numericJob?.cancel()
                        numericJob = scope.launch {
                            delay(1500)
                            val num = numericBuffer.toIntOrNull()
                            if (num != null && num in 1..activeList.size) {
                                currentIndex = num - 1
                            }
                            numericBuffer = ""
                            showNumericOverlay = false
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .focusable()
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = currentResizeMode
                }
            },
            update = { view ->
                view.resizeMode = currentResizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Volume / Brightness overlay indicators
        if (showVolumeIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (swipeVolumeValue == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.Cyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Volume: ${(swipeVolumeValue * 100).toInt()}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showBrightnessIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Brightness5,
                        contentDescription = null,
                        tint = Color.Cyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Brightness: ${(swipeBrightnessValue * 100).toInt()}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Fast Forward / Rewind seek indicators
        if (showSeekIndicator) {
            Box(
                modifier = Modifier
                    .align(if (seekIndicatorForward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (seekIndicatorForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (seekIndicatorForward) "+${seekIndicatorSeconds}s" else "-${seekIndicatorSeconds}s",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Channel numeric overlay
        if (showNumericOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Text(
                    text = numericBuffer,
                    color = Color.Cyan,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Playback error dialog overlay
        playerError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Playback Failed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(err, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        playerError = null
                        playbackTrigger++
                    }) {
                        Text("Retry")
                    }
                }
            }
        }

        // Main controls overlay
        AnimatedVisibility(
            visible = showChannelOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OmniPlayerOverlay(
                channel = activeChannel,
                currentIndex = currentIndex,
                focusRequester = overlayFocusRequester,
                seekBarFocusRequester = seekBarFocusRequester,
                autoFocusSeekBar = isTv && try { exoPlayer.isCurrentMediaItemSeekable } catch (_: Exception) { false },
                exoPlayer = exoPlayer,
                onUserInteraction = { overlayVisibilityTick = System.currentTimeMillis() },
                onMenuClick = { showSettingsPanel = true },
                onChannelsClick = { showChannelPanel = true },
                onRefreshClick = {
                    playbackTrigger++
                },
                onPrevClick = {
                    if (activeList.isNotEmpty()) currentIndex = (currentIndex - 1 + activeList.size) % activeList.size
                },
                onNextClick = {
                    if (activeList.isNotEmpty()) currentIndex = (currentIndex + 1) % activeList.size
                }
            )
        }

        // Sliding Side Channel Drawer
        AnimatedVisibility(
            visible = showChannelPanel,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            OmniSidePanel(
                channels = activeList,
                selectedIndex = currentIndex,
                focusRequester = sidePanelFocusRequester,
                onChannelSelected = { index ->
                    currentIndex = index
                    showChannelPanel = false
                },
                onClose = { showChannelPanel = false }
            )
        }

        // Sliding Settings Drawer
        AnimatedVisibility(
            visible = showSettingsPanel,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val audioTrackPairs = getAudioTrackOptions(exoPlayer)
            val audioLabels = audioTrackPairs.map { it.first }
            val resolvedAudioLabel = selectedAudioLabel ?: audioLabels.firstOrNull() ?: "Default"

            val subtitleTrackPairs = getSubtitleTrackOptions(exoPlayer)
            val subtitleLabels = subtitleTrackPairs.map { it.first }

            OmniSettingsPanel(
                preferenceManager = preferenceManager,
                focusRequester = settingsPanelFocusRequester,
                currentResizeMode = currentResizeMode,
                onResizeModeChange = { currentResizeMode = it },
                currentChannel = activeChannel,
                serverUrl = serverUrl,
                onClose = { showSettingsPanel = false },
                currentSpeed = currentPlaybackSpeed,
                onSpeedSelected = { currentPlaybackSpeed = it },
                audioLabels = audioLabels,
                currentAudio = resolvedAudioLabel,
                onAudioSelected = { label ->
                    try {
                        val lang = audioTrackPairs.firstOrNull { it.first == label }?.second
                        val params = exoPlayer.trackSelectionParameters.buildUpon()
                        if (!lang.isNullOrBlank()) {
                            params.setPreferredAudioLanguage(lang)
                        }
                        exoPlayer.trackSelectionParameters = params.build()
                        selectedAudioLabel = label
                    } catch (e: Exception) {
                        LogCollector.log("Audio select failed: ${e.message}")
                    }
                },
                subtitleLabels = subtitleLabels,
                currentSubtitle = selectedSubtitleLabel,
                onSubtitleSelected = { label ->
                    try {
                        val params = exoPlayer.trackSelectionParameters.buildUpon()
                        if (label == "Off") {
                            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        } else {
                            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            val lang = subtitleTrackPairs.firstOrNull { it.first == label }?.second
                            if (!lang.isNullOrBlank()) params.setPreferredTextLanguage(lang)
                        }
                        exoPlayer.trackSelectionParameters = params.build()
                        selectedSubtitleLabel = label
                    } catch (e: Exception) {
                        LogCollector.log("Subtitle select failed: ${e.message}")
                    }
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun OmniPlayerOverlay(
    channel: OmniChannel?,
    currentIndex: Int,
    focusRequester: FocusRequester,
    seekBarFocusRequester: FocusRequester,
    autoFocusSeekBar: Boolean,
    exoPlayer: ExoPlayer,
    onUserInteraction: () -> Unit,
    onMenuClick: () -> Unit,
    onChannelsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration
            isPlaying = exoPlayer.isPlaying
            delay(500)
        }
    }

    LaunchedEffect(autoFocusSeekBar) {
        if (autoFocusSeekBar) {
            delay(80)
            try { seekBarFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onUserInteraction() }
            .padding(24.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = channel?.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = channel?.name ?: "Unknown",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                if (!channel?.group.isNullOrBlank()) {
                    Text(
                        text = channel?.group ?: "",
                        color = Color.Cyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Center row
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayButton(onClick = onPrevClick, icon = Icons.Default.SkipPrevious)
            Spacer(modifier = Modifier.width(24.dp))
            OverlayButton(
                onClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                    isPlaying = !isPlaying
                },
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
            )
            Spacer(modifier = Modifier.width(24.dp))
            OverlayButton(onClick = onNextClick, icon = Icons.Default.SkipNext)
            Spacer(modifier = Modifier.width(24.dp))
            OverlayButton(onClick = onRefreshClick, icon = Icons.Default.Refresh)
        }

        // Bottom row
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Seek bar if seekable (catchup show)
            val seekable = try { exoPlayer.isCurrentMediaItemSeekable } catch (_: Exception) { false }
            if (seekable && duration > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = {
                            exoPlayer.seekTo(it.toLong())
                        },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .focusRequester(seekBarFocusRequester)
                    )
                    Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    OverlayButton(
                        onClick = onChannelsClick,
                        icon = Icons.Default.Menu,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OverlayButton(onClick = onMenuClick, icon = Icons.Default.Settings)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    var time by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        while (true) {
                            val cal = Calendar.getInstance()
                            time = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                            delay(30000)
                        }
                    }
                    Text(
                        text = time,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OverlayButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val rotationAngle = remember { mutableFloatStateOf(0f) }
    val animatedRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rotationAngle.value,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.LinearEasing),
        label = "refreshRotation"
    )

    IconButton(
        onClick = {
            if (icon == Icons.Default.Refresh) {
                rotationAngle.value += 360f
            }
            onClick()
        },
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) Color.Cyan.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(50)
            )
            .border(if (isFocused) 2.dp else 0.dp, Color.Cyan, RoundedCornerShape(50))
    ) {
        Icon(
            icon,
            null,
            tint = if (isFocused) Color.Cyan else Color.White,
            modifier = if (icon == Icons.Default.Refresh) {
                Modifier.graphicsLayer { rotationZ = animatedRotation }
            } else Modifier
        )
    }
}

@Composable
fun OmniSidePanel(
    channels: List<OmniChannel>,
    selectedIndex: Int,
    focusRequester: FocusRequester,
    onChannelSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.scrollToItem(selectedIndex)
    }

    Box(modifier = Modifier.fillMaxHeight().width(280.dp).background(Color.Black.copy(alpha = 0.85f)).padding(10.dp)) {
        Column {
            Text("Channels", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                itemsIndexed(channels) { index, channel ->
                    val isSelected = index == selectedIndex
                    var isFocused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == selectedIndex) Modifier.focusRequester(focusRequester) else Modifier)
                            .clip(RoundedCornerShape(8.dp))
                            .onFocusChanged { isFocused = it.isFocused }
                            .background(
                                if (isFocused) Color.Cyan.copy(alpha = 0.3f)
                                else if (isSelected) Color.Cyan.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .border(if (isFocused) 2.dp else 0.dp, Color.Cyan, RoundedCornerShape(8.dp))
                            .clickable { onChannelSelected(index) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = channel.logo, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = channel.name ?: "",
                            color = if (isFocused || isSelected) Color.Cyan else Color.White,
                            maxLines = 1,
                            fontSize = 12.sp,
                            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun OmniSettingsPanel(
    preferenceManager: SkySharedPref,
    focusRequester: FocusRequester,
    currentResizeMode: Int,
    onResizeModeChange: (Int) -> Unit,
    currentChannel: OmniChannel?,
    serverUrl: String?,
    onClose: () -> Unit,
    currentSpeed: Float = 1.0f,
    onSpeedSelected: (Float) -> Unit = {},
    audioLabels: List<String> = emptyList(),
    currentAudio: String = "Default",
    onAudioSelected: (String) -> Unit = {},
    subtitleLabels: List<String> = emptyList(),
    currentSubtitle: String = "Off",
    onSubtitleSelected: (String) -> Unit = {}
) {
    val modes = listOf(
        "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
        "Fill" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
        "Zoom" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        "Fixed Width" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        "Fixed Height" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    )

    val qualityOptions = listOf(
        "Auto" to 0,
        "144p" to 144,
        "240p" to 240,
        "360p" to 360,
        "480p" to 480,
        "720p" to 720,
        "1080p" to 1080,
        "1440p" to 1440,
        "2160p (4K)" to 2160
    )
    val qualityLabels = qualityOptions.map { it.first }
    val currentMaxHeight = preferenceManager.myPrefs.omniQualityMaxHeight
    val initialQualityLabel =
        qualityOptions.firstOrNull { it.second == currentMaxHeight }?.first ?: "Auto"

    // Playback speed options
    val speedOptions = listOf(
        0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f
    )
    fun speedLabel(s: Float): String = if (s == 1.0f) "Normal (1x)" else "${s}x"
    val speedLabels = speedOptions.map { speedLabel(it) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var currentQ by remember { mutableStateOf(initialQualityLabel) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    val subtitleOptions = remember(subtitleLabels) { listOf("Off") + subtitleLabels }

    Box(modifier = Modifier.fillMaxHeight().width(250.dp).background(Color.Black.copy(alpha = 0.85f)).padding(10.dp)) {
        Column {
            Text("Player Settings", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    val currentLabel = modes.find { it.second == currentResizeMode }?.first ?: "Fit"
                    SettingsActionItemCompact("Aspect Ratio: $currentLabel", Icons.Default.AspectRatio, modifier = Modifier.focusRequester(focusRequester)) {
                        val currentIndex = modes.indexOfFirst { it.second == currentResizeMode }
                        val nextIndex = (currentIndex + 1) % modes.size
                        onResizeModeChange(modes[nextIndex].second)
                    }
                }
                item {
                    SettingsActionItemCompact("Quality: $currentQ", Icons.Default.HighQuality) {
                        showQualityDialog = true
                    }
                }
                item {
                    SettingsActionItemCompact("Speed: ${speedLabel(currentSpeed)}", Icons.Default.Speed) {
                        showSpeedDialog = true
                    }
                }
                // Audio Language picker
                if (audioLabels.size > 1) {
                    item {
                        SettingsActionItemCompact("Audio: $currentAudio", Icons.Default.Language) {
                            showAudioDialog = true
                        }
                    }
                }
                // Subtitle picker
                if (subtitleLabels.isNotEmpty()) {
                    item {
                        SettingsActionItemCompact("Subtitles: $currentSubtitle", Icons.Default.ClosedCaption) {
                            showSubtitleDialog = true
                        }
                    }
                }
                item {
                    val store = remember(preferenceManager) { OmniFavoritesStore(preferenceManager) }
                    var isFavorite by remember(currentChannel) {
                        mutableStateOf(currentChannel != null && store.isFavorite(currentChannel))
                    }

                    if (currentChannel != null) {
                        val label = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
                        val icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                        val iconColor = if (isFavorite) Color.Red else Color.Gray
                        var isFocused by remember { mutableStateOf(false) }
                        val context = LocalContext.current

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .onFocusChanged { isFocused = it.isFocused }
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isFocused) Color.Cyan.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (isFocused) Color.Cyan else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable {
                                    val added = if (isFavorite) {
                                        store.remove(currentChannel.name ?: "")
                                        false
                                    } else {
                                        store.add(currentChannel)
                                    }
                                    isFavorite = added
                                    val msg = if (added) "Added to Favorites" else "Removed from Favorites"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = if (isFocused) Color.Cyan else iconColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, color = if (isFocused) Color.White else Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
                item {
                    SettingsActionItemCompact("Close Menu", Icons.Default.Close) { onClose() }
                }
            }
        }
    }

    if (showQualityDialog) {
        val selected = setOf(currentQ)
        OmniFilterDialog(
            title = "Quality",
            options = qualityLabels,
            selectedOptions = selected,
            singleSelect = true,
            onDismiss = { showQualityDialog = false },
            onConfirm = { selectedLabels ->
                val label = selectedLabels.firstOrNull() ?: "Auto"
                val index = qualityLabels.indexOf(label).let { if (it < 0) 0 else it }
                currentQ = label
                preferenceManager.myPrefs.omniQualityMaxHeight = qualityOptions[index].second
                preferenceManager.savePreferences()
                showQualityDialog = false
                onClose()
            }
        )
    }

    if (showSubtitleDialog) {
        OmniFilterDialog(
            title = "Subtitles",
            options = subtitleOptions,
            selectedOptions = setOf(currentSubtitle),
            singleSelect = true,
            onDismiss = { showSubtitleDialog = false },
            onConfirm = { selectedLabels ->
                onSubtitleSelected(selectedLabels.firstOrNull() ?: "Off")
                showSubtitleDialog = false
                onClose()
            }
        )
    }

    if (showSpeedDialog) {
        OmniFilterDialog(
            title = "Playback Speed",
            options = speedLabels,
            selectedOptions = setOf(speedLabel(currentSpeed)),
            singleSelect = true,
            onDismiss = { showSpeedDialog = false },
            onConfirm = { selectedLabels ->
                val label = selectedLabels.firstOrNull() ?: speedLabel(1.0f)
                val index = speedLabels.indexOf(label).let { if (it < 0) speedLabels.indexOf(speedLabel(1.0f)) else it }
                onSpeedSelected(speedOptions[index.coerceIn(0, speedOptions.lastIndex)])
                showSpeedDialog = false
                onClose()
            }
        )
    }

    if (showAudioDialog) {
        OmniFilterDialog(
            title = "Audio Language",
            options = audioLabels,
            selectedOptions = setOf(currentAudio),
            singleSelect = true,
            onDismiss = { showAudioDialog = false },
            onConfirm = { selectedLabels ->
                selectedLabels.firstOrNull()?.let { onAudioSelected(it) }
                showAudioDialog = false
                onClose()
            }
        )
    }
}

@Composable
fun SettingsActionItemCompact(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) Color.Cyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (isFocused) Color.Cyan else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isFocused) Color.Cyan else Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = if (isFocused) Color.White else Color.Gray, fontSize = 11.sp)
    }
}
