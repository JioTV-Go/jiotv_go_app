package com.skylake.skytv.jgorunner.ui.tvhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.skylake.skytv.jgorunner.services.CastManager

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChannelGridTV(
    channels: List<M3UChannelExp>,
    favoriteUrls: Set<String>,
    onSelectedChannelChanged: (M3UChannelExp) -> Unit,
    onChannelClick: (M3UChannelExp, Int) -> Unit,
    onChannelLongClick: (M3UChannelExp) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val isProcessing by CastManager.isProcessing

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(channels.size, key = { index -> channels[index].url }) { index ->
                val channel = channels[index]
                var isFocused by remember { mutableStateOf(false) }
                val isFavorite = favoriteUrls.contains(channel.url)

                Card(
                    modifier = Modifier
                        .height(120.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                onSelectedChannelChanged(channel)
                            }
                        }
                        .combinedClickable(
                            onClick = { onChannelClick(channel, index) },
                            onLongClick = { onChannelLongClick(channel) }
                        ),
                    border = if (isFocused) BorderStroke(4.dp, Color(0xFFFFD700)) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column {
                            GlideImage(
                                model = channel.logo,
                                contentDescription = "${channel.name} logo",
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = channel.name,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        if (isProcessing) CastingOverlay()
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChannelGridMain(
    filteredChannels: List<Channel>,
    favoriteIds: Set<String>,
    basefinURL: String,
    onSelectedChannelChanged: (Channel) -> Unit,
    onChannelClick: (Channel, Int) -> Unit,
    onChannelLongClick: (Channel) -> Unit
) {
    val isProcessing by CastManager.isProcessing

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredChannels.size, key = { index -> filteredChannels[index].channel_id }) { index ->
                val channel = filteredChannels[index]
                var isFocused by remember { mutableStateOf(false) }
                val isFavorite = favoriteIds.contains(channel.channel_id)

                Card(
                    modifier = Modifier
                        .height(120.dp)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                onSelectedChannelChanged(channel)
                            }
                        }
                        .combinedClickable(
                            onClick = { onChannelClick(channel, index) },
                            onLongClick = { onChannelLongClick(channel) }
                        ),
                    border = if (isFocused) BorderStroke(4.dp, Color(0xFFFFD700)) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column {
                            GlideImage(
                                model = "$basefinURL/jtvimage/${channel.logoUrl}",
                                contentDescription = channel.channel_name,
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = channel.channel_name,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        if (isProcessing) CastingOverlay()
    }
}

@Composable
private fun CastingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Casting to device...", style = TextStyle(fontSize = 16.sp))
            }
        }
    }
}

fun buildChannelInfoWindow(
    context: android.content.Context,
    channels: List<Channel>,
    basefinURL: String,
    centerIndex: Int,
    maxItems: Int = 250
): Pair<ArrayList<com.skylake.skytv.jgorunner.activities.ChannelInfo>, Int> {
    if (channels.isEmpty()) return Pair(arrayListOf(), 0)
    val safeCenter = centerIndex.coerceIn(0, channels.lastIndex)
    val safeMax = maxItems.coerceAtLeast(1).coerceAtMost(channels.size)
    val half = safeMax / 2
    val start = (safeCenter - half).coerceIn(0, (channels.size - safeMax).coerceAtLeast(0))
    val endExclusive = (start + safeMax).coerceAtMost(channels.size)
    val slice = channels.subList(start, endExclusive)
    val list = ArrayList(slice.map { ch ->
        com.skylake.skytv.jgorunner.activities.ChannelInfo(
            com.skylake.skytv.jgorunner.utils.withQuality(context, ch.channel_url),
            if (ch.logoUrl.startsWith("http")) ch.logoUrl else "$basefinURL/jtvimage/${ch.logoUrl}",
            ch.channel_name
        )
    })
    val relativeIndex = safeCenter - start
    return Pair(list, relativeIndex)
}