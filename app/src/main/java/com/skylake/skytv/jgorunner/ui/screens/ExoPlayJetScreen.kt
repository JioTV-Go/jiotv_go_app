package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.receivers.PipActionReceiver
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus
import com.skylake.skytv.jgorunner.ui.tvhome.ChannelUtils
import com.skylake.skytv.jgorunner.ui.tvhome.extractChannelIdFromPlayUrl
import com.skylake.skytv.jgorunner.utils.setupCustomPlaybackLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.pow

const val TAG = "ExoJetScreen"

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("AutoboxingStateCreation", "DefaultLocale")
@Composable
fun ExoPlayJetScreen(
    preferenceManager: SkySharedPref,
    videoUrl: String,
    keyUrl: String,
    channelList: ArrayList<ChannelInfo>?,
    currentChannelIndex: Int,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayDisplayTimeMs = 3000
    val localPORT by remember { mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort) }
    val basefinURL = "http://localhost:$localPORT"
    val tvNAV = preferenceManager.myPrefs.selectedRemoteNavTV ?: "0"
    val pipEnabled = preferenceManager.myPrefs.enablePip
    val isTv = remember {
        try {
            val pm = context.packageManager
            pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking TV feature", e)
            false
        }
    }
    val focusRequester = remember { FocusRequester() }
    var currentIndex by remember { mutableStateOf(currentChannelIndex) }
    var showChannelPanel by remember { mutableStateOf(false) }
    var panelSelectedIndex by remember { mutableStateOf(currentChannelIndex.coerceAtLeast(0)) }
    var currentProgramName by remember { mutableStateOf<String?>(null) }
    var showChannelOverlay by remember { mutableStateOf(false) }
    val retryCountRef = remember { mutableStateOf(0) }
    var exoPlayerView: PlayerView? by remember { mutableStateOf(null) }
    var numericBuffer by remember { mutableStateOf("") }
    var showNumericOverlay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var numericJob: Job? by remember { mutableStateOf(null) }
    var isControllerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentChannelIndex) {
        val incoming = currentChannelIndex.coerceAtLeast(0)
        if (incoming != currentIndex) {
            currentIndex = incoming
        }
    }

    val epgCache = remember { mutableStateMapOf<String, Pair<Long, String?>>() }

    LaunchedEffect(currentIndex, channelList) {
        val currentChannel = channelList?.getOrNull(currentIndex) ?: return@LaunchedEffect
        val channelId = extractChannelIdFromPlayUrl(currentChannel.videoUrl) ?: return@LaunchedEffect

        val now = System.currentTimeMillis()
        val cached = epgCache[channelId]

        if (cached == null || now - cached.first > 900_000) {
            val epgName = withContext(Dispatchers.IO) {
                fetchCurrentProgram(basefinURL, channelId)
            }
            epgCache[channelId] = now to epgName
        }
    }

    LaunchedEffect(showChannelPanel, channelList) {
        if (showChannelPanel && channelList != null) {
            while (showChannelPanel) {
                val visibleChannels = channelList
                withContext(Dispatchers.IO) {
                    
                    visibleChannels.chunked(10).forEach { chunk ->
                        chunk.mapNotNull { channel ->
                            val channelId = extractChannelIdFromPlayUrl(channel.videoUrl)
                            if (channelId != null) {
                                async {
                                    val now = System.currentTimeMillis()
                                    val cached = epgCache[channelId]
                                    if (cached == null || now - cached.first > 900_000) {
                                        val epgName = fetchCurrentProgram(basefinURL, channelId)
                                        epgCache[channelId] = now to epgName
                                    }
                                }
                            } else null
                        }.awaitAll()
                        delay(500) 
                    }
                }
                delay(900_000)
            }
        }
    }

    val resizeModes = remember {
        listOf(
            Triple(RESIZE_MODE_FIT, "Default", "DEF"),
            Triple(RESIZE_MODE_FILL, "Stretch", "STRETCH")
        )
    }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var showResizeOverlay by remember { mutableStateOf(false) }
    var resizeOverlayLabel by remember { mutableStateOf(resizeModes.first().second) }
    var resizeOverlayJob by remember { mutableStateOf<Job?>(null) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }

    fun commitNumericEntryLocal(list: ArrayList<ChannelInfo>?) {
        val num = numericBuffer.toIntOrNull()
        if (num != null && !list.isNullOrEmpty()) {
            val idx = (num - 1).coerceIn(0, list.size - 1)
            currentIndex = idx
        }
        numericBuffer = ""
        showNumericOverlay = false
    }

    val exoPlayer = remember {
        initializePlayer(
            getCurrentVideoUrl = { channelList?.getOrNull(currentIndex)?.videoUrl ?: videoUrl },
            getCurrentKeyUrl = { channelList?.getOrNull(currentIndex)?.keyUrl ?: keyUrl },
            context = context,
            retryCountRef = retryCountRef,
            localPort = localPORT,
            onError = onError
        )
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height != 0) {
                    val ar = videoSize.width.toFloat() / videoSize.height.toFloat()
                    if (ar.isFinite() && ar > 0.1f && ar < 10f) {
                        videoAspect = ar
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer.playWhenReady = true
                Lifecycle.Event.ON_PAUSE -> {
                    if (!PlayerCommandBus.isInPipMode && !PlayerCommandBus.isEnteringPip) {
                        exoPlayer.playWhenReady = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayerView?.player = null
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping player on dispose", e)
            }
            exoPlayer.release()
        }
    }

    var isBuffering by remember { mutableStateOf(false) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    isBuffering = false
                }
                PlayerCommandBus.notifyStateChanged()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PlayerCommandBus.notifyStateChanged()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                PlayerCommandBus.notifyStateChanged()
            }

            @Deprecated("Deprecated in Java")
            override fun onPositionDiscontinuity(reason: Int) {
                PlayerCommandBus.notifyStateChanged()
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(lifecycleOwner.lifecycle.currentStateAsState().value) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(currentIndex) {
        retryCountRef.value = 0
        isBuffering = true
        val currentUrl = channelList?.getOrNull(currentIndex)?.videoUrl ?: videoUrl
        val currrentKeyUrl = channelList?.getOrNull(currentIndex)?.keyUrl ?: keyUrl

        
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val mediaItem = buildMediaItem(currentUrl, currrentKeyUrl)
        val mediaSourceFactory = createL3MediaSourceFactory(context, currrentKeyUrl)

        exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        setupCustomPlaybackLogic(exoPlayer, currentUrl)

        if (!PlayerCommandBus.isInPipMode) {
            showChannelOverlay = true
            delay(overlayDisplayTimeMs.toLong())
            showChannelOverlay = false
        }
    }

    DisposableEffect(Unit) {
        PlayerCommandBus.setHandlers(
            playPause = {
                try {
                    exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                } finally {
                    PlayerCommandBus.notifyStateChanged()
                    
                }
            },
            next = {
                channelList?.let {
                    if (it.isNotEmpty()) {
                        val ni = (currentIndex + 1) % it.size
                        currentIndex = ni
                        PlayerCommandBus.requestSwitch(index = ni)
                    }
                }
            },
            prev = {
                channelList?.let {
                    if (it.isNotEmpty()) {
                        val pi = if (currentIndex - 1 < 0) it.size - 1 else currentIndex - 1
                        currentIndex = pi
                        PlayerCommandBus.requestSwitch(index = pi)
                    }
                }
            },
            isPlaying = { exoPlayer.isPlaying }
        )
        PlayerCommandBus.setOnStopPlayback {
            try {
                exoPlayerView?.player = null
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.playWhenReady = false
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping playback from command bus", e)
            }
        }
        onDispose {
            PlayerCommandBus.setOnStopPlayback(null)
            PlayerCommandBus.clearHandlers()
        }
    }

    DisposableEffect(Unit) {
        PlayerCommandBus.setOnSwitchRequest { url, index ->
            try {
                var found = false
                if (index != null && !channelList.isNullOrEmpty() && index in channelList.indices) {
                    currentIndex = index 
                    found = true
                } else if (!url.isNullOrEmpty()) {
                    val foundIdx = channelList?.indexOfFirst { it.videoUrl == url } ?: -1
                    if (foundIdx >= 0) {
                        currentIndex = foundIdx
                        found = true
                    }
                }

                
                if (!found && !url.isNullOrEmpty()) {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    val mediaItem = buildMediaItem(url, url)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error switching channel request", e)
            }
        }
        onDispose { PlayerCommandBus.setOnSwitchRequest(null) }
    }

    LaunchedEffect(currentIndex) {
        val channelId =
            channelList?.getOrNull(currentIndex)?.videoUrl?.let { extractChannelIdFromPlayUrl(it) }
        currentProgramName = channelId?.let { epgCache[it]?.second }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(panelSelectedIndex, showChannelPanel) {
        if (showChannelPanel) {
            val safeIndex = panelSelectedIndex.coerceIn(0, (channelList?.size ?: 1) - 1)
            listState.animateScrollToItem(safeIndex)
        }
    }

    BackHandler {
        when {
            showChannelPanel -> {
                showChannelPanel = false
            }
            isControllerVisible -> {
                exoPlayerView?.hideController()
            }
            else -> {
                val act = (context as? Activity)
                val pm = context.packageManager
                val supportsPip = try {
                    pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking PiP feature", e)
                    false
                }
                if (!isTv && supportsPip && pipEnabled) {
                    try {
                        PlayerCommandBus.isEnteringPip = true
                        val params = android.app.PictureInPictureParams.Builder()
                            .setAspectRatio(android.util.Rational(16, 9))
                            .build()
                        act?.enterPictureInPictureMode(params)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to enter Picture-in-Picture mode", e)
                        act?.finish()
                    }
                } else {
                    act?.finish()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                val isOkKey =
                    event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                    if (isControllerVisible) {
                        return@onPreviewKeyEvent false
                    } else {
                        panelSelectedIndex = currentIndex
                        showChannelPanel = channelList?.isNotEmpty() == true
                        return@onPreviewKeyEvent true
                    }
                }

                if (event.type == KeyEventType.KeyDown) {
                    val digit = when (event.key) {
                        Key.Zero -> 0
                        Key.One -> 1
                        Key.Two -> 2
                        Key.Three -> 3
                        Key.Four -> 4
                        Key.Five -> 5
                        Key.Six -> 6
                        Key.Seven -> 7
                        Key.Eight -> 8
                        Key.Nine -> 9
                        else -> null
                    }
                    if (digit != null) {
                        if (!(numericBuffer.isEmpty() && digit == 0) && numericBuffer.length < 4) {
                            numericBuffer += digit.toString()
                            showNumericOverlay = true
                            numericJob?.cancel()
                            numericJob = scope.launch {
                                delay(1200)
                                commitNumericEntryLocal(channelList)
                            }
                        }
                        return@onPreviewKeyEvent true
                    }
                }

                if (event.type == KeyEventType.KeyUp && isOkKey) {
                    if (showChannelPanel) {
                        if (!channelList.isNullOrEmpty()) {
                            currentIndex = panelSelectedIndex.coerceIn(0, channelList.size - 1)
                            showChannelPanel = false
                        }
                        return@onPreviewKeyEvent true
                    }

                    if (isControllerVisible) {
                        return@onPreviewKeyEvent false
                    } else {
                        val androidKeyEvent = android.view.KeyEvent(
                            android.view.KeyEvent.ACTION_UP,
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER
                        )
                        val handled = exoPlayerView?.dispatchKeyEvent(androidKeyEvent) == true
                        return@onPreviewKeyEvent handled
                    }
                }

                if (event.type == KeyEventType.KeyUp) {
                    if (showChannelPanel && (event.key == Key.DirectionRight || event.key == Key.Back)) {
                        showChannelPanel = false
                        return@onPreviewKeyEvent true
                    }
                }

                if (showChannelPanel && event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            if (!channelList.isNullOrEmpty()) {
                                panelSelectedIndex =
                                    (panelSelectedIndex - 1 + channelList.size) % channelList.size
                            }
                            return@onPreviewKeyEvent true
                        }

                        Key.DirectionDown -> {
                            if (!channelList.isNullOrEmpty()) {
                                panelSelectedIndex = (panelSelectedIndex + 1) % channelList.size
                            }
                            return@onPreviewKeyEvent true
                        }

                        else -> {}
                    }
                }

                return@onPreviewKeyEvent handleTVRemoteKey(
                    event = event,
                    tvNAV = tvNAV,
                    channelList = channelList,
                    currentIndexState = { currentIndex },
                    onChannelChange = { currentIndex = it }
                )
            }
    ) {
        val currentResizeMode = resizeModes[resizeModeIndex].first
        val isDefaultMode = resizeModeIndex == 0
        val aspectForModifier = when {
            isDefaultMode -> 16f / 9f
            currentResizeMode == RESIZE_MODE_FILL -> videoAspect
            else -> videoAspect
        }

        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    controllerShowTimeoutMs = 3000
                    setResizeMode(resizeModes[resizeModeIndex].first)
                    player = exoPlayer
                    exoPlayerView = this

                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            isControllerVisible = visibility == View.VISIBLE
                        }
                    )

                    post {
                        try {
                            val controller =
                                findViewById<View>(androidx.media3.ui.R.id.exo_controller) as? ViewGroup
                                    ?: return@post

                            val nextBtn = controller.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_next)
                            val prevBtn = controller.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_prev)

                            nextBtn?.setImageResource(R.drawable.ic_skip_next_24)
                            prevBtn?.setImageResource(R.drawable.ic_skip_previous_24)
                            nextBtn?.visibility = View.VISIBLE
                            prevBtn?.visibility = View.VISIBLE
                            nextBtn?.isEnabled = true
                            prevBtn?.isEnabled = true

                            nextBtn?.setOnClickListener {
                                try {
                                    val intent = Intent(context, PipActionReceiver::class.java).apply {
                                        action = PipActionReceiver.ACTION_NEXT
                                    }
                                    context.sendBroadcast(intent)
                                } catch (e: Exception) {
                                    Log.e("ExoCustom", "Failed to send NEXT", e)
                                }
                            }

                            prevBtn?.setOnClickListener {
                                try {
                                    val intent = Intent(context, PipActionReceiver::class.java).apply {
                                        action = PipActionReceiver.ACTION_PREV
                                    }
                                    context.sendBroadcast(intent)
                                } catch (e: Exception) {
                                    Log.e("ExoCustom", "Failed to send PREV", e)
                                }
                            }

                            Log.d("ExoCustom", "ExoPlayer next/prev hooked to PipActionReceiver")

                            var targetBar: ViewGroup? = null
                            val candidateIds = listOf(
                                androidx.media3.ui.R.id.exo_basic_controls,
                                androidx.media3.ui.R.id.exo_bottom_bar,
                                androidx.media3.ui.R.id.exo_controls_background
                            )
                            for (cid in candidateIds) {
                                val v = controller.findViewById<View>(cid)
                                if (v is ViewGroup) {
                                    targetBar = v; break
                                }
                            }
                            if (targetBar == null) {
                                fun deepest(group: ViewGroup): ViewGroup {
                                    var best = group
                                    (0 until group.childCount).forEach { i ->
                                        val c = group.getChildAt(i)
                                        if (c is ViewGroup) {
                                            val d = deepest(c)
                                            if (d.childCount > best.childCount) best = d
                                        }
                                    }
                                    return best
                                }
                                targetBar = deepest(controller)
                            }

                            val isTV = try {
                                val pm = context.packageManager
                                pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error checking leanback feature for UI", e)
                                false
                            }
                            val supportsPip = try {
                                (context as? Activity)?.packageManager?.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
                            } catch (e: Exception) {
                                Log.e(TAG, "Error checking PIP feature for UI", e)
                                false
                            }

                            val pipButton = ImageButton(context).apply {
                                setBackgroundResource(android.R.color.transparent)
                                setImageResource(R.drawable.ic_pip_24)
                                contentDescription = "Picture-in-Picture"
                                imageAlpha = 230
                                val pad = (4 * resources.displayMetrics.density).toInt()
                                setPadding(pad, pad, pad, pad)
                                setOnClickListener {
                                    try {
                                        (context as? Activity)?.let { act ->
                                            PlayerCommandBus.isEnteringPip = true
                                            val params =
                                                android.app.PictureInPictureParams.Builder()
                                                    .setAspectRatio(android.util.Rational(16, 9))
                                                    .build()
                                            act.enterPictureInPictureMode(params)
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed PiP enter: ${e.message}", e)
                                    }
                                }
                                val params = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                params.gravity = Gravity.CENTER_VERTICAL
                                layoutParams = params
                            }

                            val resizeButton = ImageButton(context).apply {
                                setBackgroundResource(android.R.color.transparent)
                                setImageResource(R.drawable.ic_aspect_ratio_24)
                                contentDescription = "Resize mode"
                                imageAlpha = 220
                                val pad = (4 * resources.displayMetrics.density).toInt()
                                setPadding(pad, pad, pad, pad)
                                setOnClickListener {
                                    resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                    val mode = resizeModes[resizeModeIndex]
                                    exoPlayerView?.setResizeMode(mode.first)
                                    exoPlayerView?.requestLayout()
                                    resizeOverlayLabel = mode.second
                                    showResizeOverlay = true
                                    resizeOverlayJob?.cancel()
                                    resizeOverlayJob = scope.launch {
                                        delay(1200)
                                        showResizeOverlay = false
                                    }
                                }
                                val params = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                params.gravity = Gravity.CENTER_VERTICAL
                                params.marginStart = (8 * resources.displayMetrics.density).toInt()
                                layoutParams = params
                            }

                            val count = targetBar.childCount
                            val insertIndex = if (count > 0) count - 1 else count
                            if (!isTv && !isTV && supportsPip && pipEnabled) {
                                try {
                                    targetBar.addView(pipButton, insertIndex)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to insert PIP button at index", e)
                                    targetBar.addView(pipButton)
                                }
                            }
                            try {
                                targetBar.addView(resizeButton, insertIndex + 1)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to insert Resize button at index", e)
                                targetBar.addView(resizeButton)
                            }

                            hookExoControllerButtons(findViewById(R.id.player_view),context)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to position custom player buttons", e)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    stroke = Stroke(width = 4.dp.toPx()),
                    gapSize = 8.dp,
                    amplitude = 0.75f,
                )
            }
        }

        DisposableEffect(Unit) {
            PlayerCommandBus.setOnPipModeChanged { isPip ->
                if (isPip) {
                    try {
                        showChannelPanel = false
                        isControllerVisible = false
                        exoPlayerView?.useController = false
                        exoPlayerView?.hideController()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating UI on enter PIP", e)
                    }
                } else {
                    try {
                        exoPlayerView?.useController = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating UI on exit PIP", e)
                    }
                }
            }
            onDispose { PlayerCommandBus.setOnPipModeChanged(null) }
        }

        LaunchedEffect(showChannelPanel) {
            if (showChannelPanel) {
                focusRequester.requestFocus()
            }
        }

        AnimatedVisibility(
            visible = !PlayerCommandBus.isInPipMode && ((showChannelOverlay && channelList != null) || isControllerVisible),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val currentChannel = channelList?.getOrNull(currentIndex)
            val currentChannelId = currentChannel?.let { extractChannelIdFromPlayUrl(it.videoUrl) }
            val currentProgramName = epgCache[currentChannelId]?.second

            ChannelInfoOverlay(
                channelList = channelList,
                currentIndex = currentIndex,
                currentProgramName = currentProgramName
            )
            CurrentTimeOverlay(
                visible = !PlayerCommandBus.isInPipMode && ((showChannelOverlay && channelList != null) || isControllerVisible)
            )
        }

        if (!PlayerCommandBus.isInPipMode && showNumericOverlay && numericBuffer.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = numericBuffer,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }

        if (!PlayerCommandBus.isInPipMode && isControllerVisible) {
            IconButton(
                onClick = {
                    panelSelectedIndex = currentIndex
                    showChannelPanel = channelList?.isNotEmpty() == true && !showChannelPanel
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesomeMotion,
                    contentDescription = "Toggle channel list",
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        if (showResizeOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = resizeOverlayLabel,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        if (showChannelPanel && channelList != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .width(280.dp)
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = channelList,
                        key = { idx, _ -> idx }
                    ) { idx, ch ->
                        val isSelected = idx == panelSelectedIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0x33FFFFFF) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Transparent)
                                .clickable {
                                    panelSelectedIndex = idx
                                    currentIndex = idx
                                    showChannelPanel = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(ch.logoUrl)
                                    .size(80)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = String.format("%02d", idx + 1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            val channelId = extractChannelIdFromPlayUrl(ch.videoUrl)
                            Column {
                                Text(
                                    text = ch.channelName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1
                                )
                                EpgText(channelId, epgCache)
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                preferenceManager.myPrefs.currChannelUrl = ""
                preferenceManager.savePreferences()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving shared preferences on dispose", e)
            }
        }
    }
}

@Composable
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

@Composable
fun EpgText(
    channelId: String?,
    epgCache: Map<String, Pair<Long, String?>>
) {
    val epg = channelId?.let { epgCache[it]?.second }
    if (!epg.isNullOrBlank()) {
        Text(
            text = epg,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

suspend fun fetchCurrentProgram(basefinURL: String, channelId: String): String? {
    val epgURLc = "$basefinURL/epg/${channelId}/0"
    val epgData = ChannelUtils.fetchEpg(epgURLc)
    return epgData?.showname
}

@SuppressLint("DefaultLocale")
@Composable
fun ChannelInfoOverlay(
    channelList: List<ChannelInfo>?,
    currentIndex: Int,
    currentProgramName: String?
) {
    channelList?.getOrNull(currentIndex)?.let { channel ->
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.DarkGray.copy(
                                    alpha = 0.5f
                                )
                            )
                        ) {
                            AsyncImage(
                                model = channel.logoUrl,
                                contentDescription = "Channel Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(60.dp)
                                    .shadow(8.dp, RoundedCornerShape(16.dp), clip = false)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.8f))
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format("%02d", currentIndex + 1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .widthIn(min = 36.dp)
                                )
                                Text(
                                    text = channel.channelName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    maxLines = 1
                                )
                            }

                            currentProgramName?.let { programName ->
                                Spacer(modifier = Modifier.height(0.dp))
                                Text(
                                    text = programName,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentTimeOverlay(visible: Boolean) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    AnimatedVisibility(
        visible = visible && isLandscape,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                var currentTime by remember { mutableStateOf(getCurrentFormattedTime()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        currentTime = getCurrentFormattedTime()
                        delay(60000L)
                    }
                }
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun getCurrentFormattedTime(): String {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    return String.format("%02d:%02d %s", if (hour == 0) 12 else hour, minute, amPm)
}

@UnstableApi
fun initializePlayer(
    getCurrentVideoUrl: () -> String,
    getCurrentKeyUrl: () -> String,
    context: Context,
    retryCountRef: MutableState<Int>,
    localPort: Int,
    onError: (String) -> Unit
): ExoPlayer {

    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(8000)

    val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)

    val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            25000,
            50000,
            1500,
            2500
        ).build()

    val player = ExoPlayer.Builder(context, renderersFactory)
        .setLoadControl(loadControl)
        .build()

    var useDrmFallback = false
    val maxRetries = 2
    retryCountRef.value = 0

    fun prepareAndPlay(seekToPosition: Long = 0L) {
        val originalUrl = getCurrentVideoUrl()
        val originalKeyUrl = getCurrentKeyUrl()

        val mediaItem = buildMediaItem(originalUrl, originalKeyUrl)
        val mediaSourceFactory = createL3MediaSourceFactory(context, originalKeyUrl)

        player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        player.prepare()

        if (seekToPosition > 0L) {
            player.seekTo(seekToPosition)
        }
        player.playWhenReady = true
    }

    player.addListener(object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val currentPosition = player.currentPosition
            val currentUrl = getCurrentVideoUrl()
            val portIdentifier = ":$localPort"

            if (!useDrmFallback) {
                Log.w(TAG, "Standard playback failed. Switching to DRM MPD playback. Error: ${error.message}")
                useDrmFallback = true
                retryCountRef.value = 0
                player.stop()
                prepareAndPlay(currentPosition)
                return
            }

            if (retryCountRef.value >= maxRetries) {
                if (currentUrl.contains(portIdentifier)) {
                    Log.e(TAG, "DRM playback failed $maxRetries times, delegating to WebView.")
                    val formattedUrl = currentUrl
                        .replace(".m3u8", "", ignoreCase = true)
                        .replace("/live/", "/mpd/", ignoreCase = true)
                    onError(formattedUrl)
                } else {
                    Log.e(TAG, "External playback failed completely after $maxRetries retries.")
                }
            } else {
                retryCountRef.value++
                val delayTimeMs = (1000L * 2.0.pow(retryCountRef.value.toDouble())).toLong()
                Log.w(TAG, "DRM Playback error. Retrying attempt ${retryCountRef.value}/$maxRetries in ${delayTimeMs}ms...")

                player.stop()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    prepareAndPlay(currentPosition)
                }, delayTimeMs)
            }
        }
    })

    return player
}

fun handleTVRemoteKey(
    event: KeyEvent,
    tvNAV: String?,
    channelList: ArrayList<ChannelInfo>?,
    currentIndexState: () -> Int,
    onChannelChange: (Int) -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyUp) return false

    val currentIndex = currentIndexState()
    val newIndex = when (tvNAV) {
        "0" -> when (event.key) {
            Key.ChannelUp -> (currentIndex + 1) % (channelList?.size ?: return false)
            Key.ChannelDown -> if (currentIndex - 1 < 0) (channelList?.size ?: 1) - 1 else currentIndex - 1
            else -> return false
        }
        "1" -> when (event.key) {
            Key.DirectionUp -> (currentIndex + 1) % (channelList?.size ?: return false)
            Key.DirectionDown -> if (currentIndex - 1 < 0) (channelList?.size ?: 1) - 1 else currentIndex - 1
            else -> return false
        }
        else -> return false
    }

    onChannelChange(newIndex)
    return true
}

private fun hookExoControllerButtons(playerView: PlayerView, context: Context) {
    playerView.post {
        try {
            val controller = playerView.findViewById<ViewGroup>(androidx.media3.ui.R.id.exo_controller) ?: return@post
        } catch (e: Exception) {
            Log.e("ExoCustom", "Failed to hook Exo buttons: ${e.message}")
        }
    }
}

@OptIn(UnstableApi::class)
fun createL3MediaSourceFactory(
    context: Context,
    licenseUrl: String,
    userAgent: String = "ExoPlayer",
    headers: Map<String, String> = emptyMap()
): DefaultMediaSourceFactory {

    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(8000)
        .setDefaultRequestProperties(headers)

    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    val drmCallback = HttpMediaDrmCallback(licenseUrl, httpDataSourceFactory)

    val drmSessionManager = DefaultDrmSessionManager.Builder()
        .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID) { uuid ->
            FrameworkMediaDrm.newInstance(uuid).apply {
                setPropertyString("securityLevel", "L3")
            }
        }
        .build(drmCallback)

    return DefaultMediaSourceFactory(context)
        .setDataSourceFactory(dataSourceFactory)
        .setDrmSessionManagerProvider { drmSessionManager }
}

@UnstableApi
fun buildMediaItem(videoUrl: String, keyUrl: String?): MediaItem {
    Log.d(TAG, "Building Stream URL: $videoUrl | DRM Key: $keyUrl")

    return MediaItem.Builder()
        .setUri(videoUrl.toUri())
        .setMimeType(MimeTypes.APPLICATION_MPD)
        .setDrmConfiguration(
            MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                .setLicenseUri(keyUrl ?: "")
                .build()
        )
        .build()
}
