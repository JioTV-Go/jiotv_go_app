package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.skylake.skytv.jgorunner.data.model.CloudChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(UnstableApi::class)
@Composable
fun CloudPlayerScreen(
    channel: CloudChannel,
    channelList: List<CloudChannel>,
    onChannelChange: (CloudChannel) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showOverlay by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentTime by remember { mutableStateOf("") }

    var numericBuffer by remember { mutableStateOf("") }
    var showNumericOverlay by remember { mutableStateOf(false) }
    var numericJob: Job? by remember { mutableStateOf(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(channel) {
        playbackError = null
        isBuffering = true
        showOverlay = true

        val dataSourceFactory: HttpDataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent(channel.user_agent ?: "CloudPlay")
            .setAllowCrossProtocolRedirects(true)

        channel.headers?.let { headers ->
            dataSourceFactory.setDefaultRequestProperties(headers)
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(channel.mpd_url ?: channel.m3u8_url)

        if (channel.license_url != null) {
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(channel.license_url)
                    .build()
            )
        }

        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(mediaItemBuilder.build())

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        delay(3000)
        showOverlay = false
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = error.message
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(10000)
        }
    }

    fun commitNumericEntry() {
        val num = numericBuffer.toIntOrNull()
        if (num != null && num > 0 && num <= channelList.size) {
            onChannelChange(channelList[num - 1])
        }
        numericBuffer = ""
        showNumericOverlay = false
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
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
                        numericBuffer += digit.toString()
                        showNumericOverlay = true
                        numericJob?.cancel()
                        numericJob = scope.launch {
                            delay(1500)
                            commitNumericEntry()
                        }
                        return@onPreviewKeyEvent true
                    }

                    when (event.key) {
                        Key.DirectionUp -> {
                            val idx = channelList.indexOf(channel)
                            if (idx > 0) onChannelChange(channelList[idx - 1])
                            true
                        }
                        Key.DirectionDown -> {
                            val idx = channelList.indexOf(channel)
                            if (idx < channelList.size - 1) onChannelChange(channelList[idx + 1])
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            showOverlay = !showOverlay
                            true
                        }
                        Key.DirectionLeft -> {
                            // Requirement: Left opens side menu during playback
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (playbackError != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Playback Error", color = Color.Red)
                Button(onClick = {
                    exoPlayer.prepare()
                    exoPlayer.play()
                }) {
                    Text("Retry")
                }
            }
        }

        // Clock Overlay
        Text(
            text = currentTime,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        // Numeric entry overlay
        if (showNumericOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(text = numericBuffer, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Channel Info Overlay
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${channel.group} | ${channel.language}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
