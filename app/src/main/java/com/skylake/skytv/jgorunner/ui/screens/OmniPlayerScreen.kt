package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.skylake.skytv.jgorunner.data.OmniFavoritesStore
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val OMNI_TAG = "OmniPlayerScreen"

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@Composable
fun OmniPlayerScreen(
    preferenceManager: SkySharedPref,
    channelList: List<OmniChannel>,
    initialIndex: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentIndex by remember(initialIndex) { mutableIntStateOf(initialIndex) }
    var activeChannel by remember(currentIndex) { mutableStateOf(channelList.getOrNull(currentIndex)) }
    
    var showChannelPanel by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var currentResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var currentPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }
    
    var playerError by remember { mutableStateOf<String?>(null) }
    var showController by remember { mutableStateOf(true) }
    var controllerTimeoutJob by remember { mutableStateOf<Job?>(null) }
    
    var useDrm by remember(currentIndex) { mutableStateOf(true) }
    var drmRetryCount by remember(currentIndex) { mutableIntStateOf(0) }

    fun buildOmniMediaItem(videoUrl: String, forceDrm: Boolean): MediaItem {
        val targetUrl = if (forceDrm) {
            videoUrl.replace("/live/mpd/", "/live/mpd/").replace("/live/", "/live/mpd/").replace(".m3u8", ".mpd")
        } else {
            videoUrl.replace("/live/mpd/", "/live/").replace(".mpd", ".m3u8")
        }
        val builder = MediaItem.Builder().setUri(targetUrl)
        if (forceDrm) {
            val licenseUrl = targetUrl.replace("/live/mpd/", "/live/key/").replace(".mpd", "")
            builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(licenseUrl)
                    .build()
            )
            builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
        } else {
            builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        }
        return builder.build()
    }

    val trackSelector = remember { DefaultTrackSelector(context) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(OMNI_TAG, "ExoPlayer error: ${error.message}")
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
            exoPlayer.release()
        }
    }

    LaunchedEffect(currentIndex, useDrm) {
        val ch = channelList.getOrNull(currentIndex) ?: return@LaunchedEffect
        activeChannel = ch
        playerError = null
        try {
            val url = ch.m3u8Url ?: ch.url ?: return@LaunchedEffect
            val mediaItem = buildOmniMediaItem(url, forceDrm = useDrm)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
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

    val favoriteStore = remember { OmniFavoritesStore(preferenceManager) }
    val isTv = com.skylake.skytv.jgorunner.utils.DeviceUtils.isTvDevice(context)

    BackHandler {
        when {
            showChannelPanel -> showChannelPanel = false
            showSettingsPanel -> showSettingsPanel = false
            showController -> showController = false
            else -> (context as? Activity)?.finish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        triggerControllerTimeout()
                    }
                )
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    triggerControllerTimeout()
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            if (!showChannelPanel && !showSettingsPanel) {
                                showChannelPanel = true
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (!showChannelPanel && !showSettingsPanel) {
                                showSettingsPanel = true
                                true
                            } else false
                        }
                        Key.DirectionUp -> {
                            if (!showChannelPanel && !showSettingsPanel && currentIndex > 0) {
                                currentIndex--
                                true
                            } else false
                        }
                        Key.DirectionDown -> {
                            if (!showChannelPanel && !showSettingsPanel && currentIndex < channelList.size - 1) {
                                currentIndex++
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Video Player View
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

        // Controller Overlay
        AnimatedVisibility(
            visible = showController,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Header (Channel Info)
                activeChannel?.let { ch ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                )
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!ch.logo.isNullOrBlank()) {
                            AsyncImage(
                                model = ch.logo,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column {
                            Text(
                                text = ch.name ?: "Unknown Channel",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!ch.group.isNullOrBlank()) {
                                Text(
                                    text = ch.group,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            triggerControllerTimeout()
                        },
                        modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (currentIndex < channelList.size - 1) currentIndex++ },
                        modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                // Bottom Control Options
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { showChannelPanel = true }) {
                            Icon(Icons.Default.List, contentDescription = "Channels", tint = Color.White)
                        }
                        IconButton(onClick = { showSettingsPanel = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }

                    IconButton(
                        onClick = {
                            activeChannel?.let { ch ->
                                if (favoriteStore.isFavorite(ch)) {
                                    favoriteStore.remove(ch.name ?: "")
                                } else {
                                    favoriteStore.add(ch)
                                }
                                triggerControllerTimeout()
                            }
                        }
                    ) {
                        val isFav = activeChannel?.let { favoriteStore.isFavorite(it) } ?: false
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color.Red else Color.White
                        )
                    }
                }
            }
        }

        // Channels Side Panel
        AnimatedVisibility(
            visible = showChannelPanel,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Channels", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showChannelPanel = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
                    LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                        itemsIndexed(channelList) { index, ch ->
                            val isSelected = index == currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        currentIndex = index
                                        showChannelPanel = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!ch.logo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ch.logo,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(Color.White).padding(2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column {
                                    Text(
                                        text = ch.name ?: "Unknown",
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!ch.group.isNullOrBlank()) {
                                        Text(
                                            text = ch.group,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Side Panel
        AnimatedVisibility(
            visible = showSettingsPanel,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Playback Settings", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showSettingsPanel = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scale / Resize Mode Option
                    Text("Aspect Ratio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                            "Zoom" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                            "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL
                        )
                        modes.forEach { (label, mode) ->
                            val isSelected = currentResizeMode == mode
                            Button(
                                onClick = { currentResizeMode = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Speed Option
                    Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val speeds = listOf(1.0f, 1.25f, 1.5f)
                        speeds.forEach { speed ->
                            val isSelected = currentPlaybackSpeed == speed
                            Button(
                                onClick = { currentPlaybackSpeed = speed },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${speed}x", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Error Dialog / Display
        playerError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Playback Failed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(err, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            playerError = null
                            val url = activeChannel?.m3u8Url ?: activeChannel?.url ?: return@Button
                            exoPlayer.setMediaItem(MediaItem.fromUri(url))
                            exoPlayer.prepare()
                            exoPlayer.play()
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
