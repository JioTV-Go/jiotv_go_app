package com.skylake.skytv.jgorunner.ui.screens
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.ChannelUtils
import com.skylake.skytv.jgorunner.ui.tvhome.extractChannelIdFromPlayUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TAG = "ExoJetScreen"

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("AutoboxingStateCreation", "DefaultLocale")
@Composable
fun ExoPlayJetScreen(
    preferenceManager: SkySharedPref,
    videoUrl: String,
    channelList: ArrayList<ChannelInfo>?,
    currentChannelIndex: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayDisplayTimeMs = 3000
    val localPORT by remember { mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort) }
    val basefinURL = "http://localhost:$localPORT"
    val tvNAV = preferenceManager.myPrefs.selectedRemoteNavTV ?: "0"
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

    // --- Epg fetch ---
    val epgCache = remember { mutableStateMapOf<String, Pair<Long, String?>>() }
    LaunchedEffect(showChannelPanel, channelList) {
        if (showChannelPanel && channelList != null) {
            while (showChannelPanel) {
                val visibleChannels = channelList
                withContext(Dispatchers.IO) {
                    visibleChannels.mapNotNull { channel ->
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
                }
                delay(900_000)
            }
        }
    }

    // --- Resize Config : This ensures overlays and controls remain stable. ---
    val resizeModes = remember {
        listOf(
            Triple(RESIZE_MODE_FIT, "Default", "DEF"),
            Triple(RESIZE_MODE_FILL, "Stretch", "STRETCH"),
            Triple(RESIZE_MODE_ZOOM, "Zoom", "ZOOM")
        )
    }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var showResizeOverlay by remember { mutableStateOf(false) }
    var resizeOverlayLabel by remember { mutableStateOf(resizeModes.first().second) }
    var resizeOverlayJob by remember { mutableStateOf<Job?>(null) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }

    // --- Key Num Entry ---
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
            context = context,
            retryCountRef = retryCountRef
        )
    }

    // --- Resize Config ---
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
                Lifecycle.Event.ON_PAUSE -> exoPlayer.playWhenReady = false
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // --- Custom Buffering ---
    var isBuffering by remember { mutableStateOf(false) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    isBuffering = false
                }
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
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(channelList?.getOrNull(currentIndex)?.videoUrl ?: videoUrl)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        showChannelOverlay = true
        delay(overlayDisplayTimeMs.toLong())
        showChannelOverlay = false
    }

    LaunchedEffect(currentIndex) {
        val channelId = channelList?.getOrNull(currentIndex)?.videoUrl?.let { extractChannelIdFromPlayUrl(it) }
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
                (context as? Activity)?.finish()
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
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                    if (isControllerVisible) {
                        return@onPreviewKeyEvent false
                    } else {
                        panelSelectedIndex = currentIndex
                        showChannelPanel = channelList?.isNotEmpty() == true
                        return@onPreviewKeyEvent true
                    }
                }

                // --- Key Num Entry ---
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
                        if (!(numericBuffer.isEmpty() && digit == 0)) { // avoid leading zero
                            if (numericBuffer.length < 4) {
                                numericBuffer += digit.toString()
                                showNumericOverlay = true
                                numericJob?.cancel()
                                numericJob = scope.launch {
                                    delay(1200)
                                    commitNumericEntryLocal(channelList)
                                }
                            }
                        }
                        return@onPreviewKeyEvent true
                    }
                }

                if (event.type == KeyEventType.KeyUp) {
                    if (isControllerVisible) {
                        return@onPreviewKeyEvent false
                    } else {
                        if (numericBuffer.isNotEmpty() && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)) {
                            numericJob?.cancel()
                            commitNumericEntryLocal(channelList)
                            return@onPreviewKeyEvent true
                        }
                    }
                    if (event.key == Key.Menu || event.key == Key.DirectionLeft) {
                        panelSelectedIndex = currentIndex
                        showChannelPanel = channelList?.isNotEmpty() == true
                        return@onPreviewKeyEvent true
                    }
                    if (showChannelPanel && (event.key == Key.DirectionRight || event.key == Key.Back)) {
                        showChannelPanel = false
                        return@onPreviewKeyEvent true
                    }
                    if (showChannelPanel) {
                        when (event.key) {
                            Key.DirectionUp -> {
                                if (!channelList.isNullOrEmpty()) {
                                    panelSelectedIndex = (panelSelectedIndex - 1 + channelList.size) % channelList.size
                                }
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionDown -> {
                                if (!channelList.isNullOrEmpty()) {
                                    panelSelectedIndex = (panelSelectedIndex + 1) % channelList.size
                                }
                                return@onPreviewKeyEvent true
                            }
                            Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                                if (!channelList.isNullOrEmpty()) {
                                    currentIndex = panelSelectedIndex.coerceIn(0, channelList.size - 1)
                                    showChannelPanel = false
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
                false
            }

    ) {
        val currentResizeMode = resizeModes[resizeModeIndex].first

        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setResizeMode(resizeModes[resizeModeIndex].first)
                    player = exoPlayer
                    exoPlayerView = this

                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            isControllerVisible = visibility == View.VISIBLE
                        }
                    )

                    // Inject resize btn
                    post {
                        try {
                            val controller = findViewById<View>(androidx.media3.ui.R.id.exo_controller) as? ViewGroup ?: return@post
                            var targetBar: ViewGroup? = null
                            val candidateIds = listOf(
                                androidx.media3.ui.R.id.exo_basic_controls,
                                androidx.media3.ui.R.id.exo_bottom_bar,
                                androidx.media3.ui.R.id.exo_controls_background
                            )
                            for (cid in candidateIds) {
                                val v = controller.findViewById<View>(cid)
                                if (v is ViewGroup) { targetBar = v; break }
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

                            val resizeButton = ImageButton(context).apply {
                                setBackgroundResource(android.R.color.transparent)
                                setImageResource(R.drawable.baseline_aspect_ratio_24)
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
                                layoutParams = params
                            }

                            val count = targetBar.childCount
                            val insertIndex = if (count > 0) count - 1 else count
                            try {
                                targetBar.addView(resizeButton, insertIndex)
                            } catch (_: Exception) {
                                targetBar.addView(resizeButton)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to position resize button: ${e.message}")
                        }
                    }
                }
            },
            // Keep a stable layout area so only the video surface is zoomed by setResizeMode
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
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

        LaunchedEffect(showChannelPanel) {
            if (showChannelPanel) {
                focusRequester.requestFocus()
            }
        }

        AnimatedVisibility(
            visible = (showChannelOverlay && channelList != null) || isControllerVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ChannelInfoOverlay(
                channelList = channelList,
                currentIndex = currentIndex,
                currentProgramName = currentProgramName
            )
            CurrentTimeOverlay(
                visible = (showChannelOverlay && channelList != null) || isControllerVisible
            )
        }

        // --- Key Num Config ---
        if (showNumericOverlay && numericBuffer.isNotEmpty()) {
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

        if (isControllerVisible) {
            IconButton(
                onClick = {
                    panelSelectedIndex = currentIndex
                    showChannelPanel = channelList?.isNotEmpty() == true && !showChannelPanel
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(0.dp)
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

        // Left-side channel panel
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
            text = epg ?: "",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}





suspend fun fetchCurrentProgram(basefinURL: String, channelId: String): String? {
    val epgURLc = "$basefinURL/epg/${channelId}/0"
//    Log.d("NANOdix1", epgURLc)
    val epgData = ChannelUtils.fetchEpg(epgURLc)
//    Log.d(TAG, "Now playing: ${epgData?.showname}")
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
                        // Logo
                        Card(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f))
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
                            // Channel Number + Name
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
    val cal = java.util.Calendar.getInstance()
    val hour = cal.get(java.util.Calendar.HOUR)
    val minute = cal.get(java.util.Calendar.MINUTE)
    val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"
    return String.format("%02d:%02d %s", if (hour == 0) 12 else hour, minute, amPm)
}

@UnstableApi
fun initializePlayer(
    getCurrentVideoUrl: () -> String,
    context: Context,
    retryCountRef: MutableState<Int>
): ExoPlayer {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
    val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)

    val player = ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
    val maxRetries = 5
    retryCountRef.value = 0

    fun prepareAndPlay() {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(getCurrentVideoUrl()))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    prepareAndPlay()

//    player.addListener(object : Player.Listener {
//        override fun onPlayerError(error: PlaybackException) {
//            if (retryCountRef.value < maxRetries) {
//                retryCountRef.value++
//                Log.d(TAG, "Retrying playback: attempt ${retryCountRef.value}")
//                player.stop()
//                prepareAndPlay()
//            } else {
//                Toast.makeText(context, "Playback failed after $maxRetries attempts", Toast.LENGTH_SHORT).show()
//                Log.e(TAG, "Playback permanently failed.")
//            }
//        }
//    })

    // Always Retry
    player.addListener(object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.d(TAG, "Retrying playback infinitely")
            player.stop()
            prepareAndPlay()
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
