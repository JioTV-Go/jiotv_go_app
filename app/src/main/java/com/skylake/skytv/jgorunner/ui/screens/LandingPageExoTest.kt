package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

@SuppressLint("NewApi")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LandingPageExoTest() {
    val context = LocalContext.current

    val m3u8Url = "http://192.168.1.17:5349/zana/9.m3u"

    val userAgent = "Mozilla/5.0"

    val exoPlayer = remember {
        // Create track selector with video track priority and high quality for Sun TV channels
        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setForceHighestSupportedBitrate(true)
                .setPreferredVideoMimeTypes("video/avc", "video/hevc", "video/mp4")
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setMaxVideoBitrate(Integer.MAX_VALUE)
                .build()
        }

        // Create renderers factory with hardware acceleration and fallback support
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(renderersFactory)
            .build().apply {

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)

            val dataSourceFactory = DefaultDataSource.Factory(
                context,
                httpDataSourceFactory
            )

            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(m3u8Url))

            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Exo:Player Check",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .padding(16.dp)
        )
    }
}
