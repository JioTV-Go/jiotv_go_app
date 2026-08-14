package com.skylake.skytv.jgorunner.ui.tvhome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker
import com.skylake.skytv.jgorunner.utils.withQuality
import kotlinx.coroutines.delay

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun Favorites_Layout(
    context: Context,
    viewModel: TvViewModel,
    basefinURL: String
) {
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val gridState = rememberLazyGridState()

    var isSortModeActive by remember { mutableStateOf(false) }
    var movingChannelId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = isSortModeActive) {
        isSortModeActive = false
        movingChannelId = null
    }

    LaunchedEffect(favoriteChannels) {
        if (favoriteChannels.isNotEmpty() && viewModel.preferenceManager.myPrefs.startTvAutomatically && !AppStartTracker.shouldPlayChannel) {
            val firstChannel = favoriteChannels.first()
            val intent = Intent(context, ExoPlayJet::class.java).apply {
                putExtra("zone", "TV")
                putParcelableArrayListExtra(
                    "channel_list_data",
                    ArrayList(favoriteChannels.map { ch ->
                        val fullLogoUrl = if (ch.logoUrl.contains("http")) ch.logoUrl else "$basefinURL/jtvimage/${ch.logoUrl}"
                        ChannelInfo(
                            withQuality(context, ch.channel_url),
                            ch.key_url,
                            fullLogoUrl,
                            ch.channel_name
                        )
                    })
                )
                putExtra("current_channel_index", 0)
                putExtra("video_url", firstChannel.channel_url)
                putExtra("key_url", firstChannel.key_url)

                val firstLogoUrl = if (firstChannel.logoUrl.contains("http")) firstChannel.logoUrl else "$basefinURL/jtvimage/${firstChannel.logoUrl}"
                putExtra("logo_url", firstLogoUrl)
                putExtra("ch_name", firstChannel.channel_name)

                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            delay(500)
            context.startActivity(intent)
            viewModel.saveToRecents(firstChannel)
            AppStartTracker.shouldPlayChannel = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSortModeActive) "Sort Mode Active" else "Your Favorites",
                fontSize = 18.sp,
                color = if (isSortModeActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
            )

            if (favoriteChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No favorite channels yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Go to the channel list and long press a channel to add it!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 320.dp)
                        )
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columnCount = maxOf(1, (maxWidth.value / 116f).toInt())

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isSortModeActive) {
                                if (!isSortModeActive) return@pointerInput

                                var draggingIndex: Int? = null

                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                            offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                                                    offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                                        }?.let {
                                            draggingIndex = it.index
                                            movingChannelId = favoriteChannels[it.index].channel_id
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val currentDraggingIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress

                                        val currentOffset = change.position
                                        val targetItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                            currentOffset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                                                    currentOffset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                                        }

                                        if (targetItem != null && targetItem.index != currentDraggingIndex) {
                                            viewModel.swapFavoriteChannels(currentDraggingIndex, targetItem.index)
                                            draggingIndex = targetItem.index
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        movingChannelId = null
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        movingChannelId = null
                                    }
                                )
                            }
                    ) {
                        itemsIndexed(favoriteChannels, key = { _, it -> it.channel_id }) { index, channel ->
                            val focusRequester = remember(channel.channel_id) { FocusRequester() }
                            var isFocused by remember { mutableStateOf(false) }
                            val isBeingMoved = movingChannelId == channel.channel_id

                            Card(
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isBeingMoved) 12.dp else 6.dp
                                ),
                                modifier = Modifier
                                    .height(120.dp)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (isSortModeActive && isBeingMoved && keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.nativeKeyEvent.keyCode) {
                                                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                                    if (index > 0) viewModel.swapFavoriteChannels(index, index - 1)
                                                    true
                                                }
                                                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    if (index < favoriteChannels.size - 1) viewModel.swapFavoriteChannels(index, index + 1)
                                                    true
                                                }
                                                AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                                    if (index - columnCount >= 0) viewModel.swapFavoriteChannels(index, index - columnCount)
                                                    true
                                                }
                                                AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    if (index + columnCount < favoriteChannels.size) viewModel.swapFavoriteChannels(index, index + columnCount)
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = {
                                            if (isSortModeActive) {
                                                if (movingChannelId == null) {
                                                    movingChannelId = channel.channel_id
                                                } else if (movingChannelId == channel.channel_id) {
                                                    movingChannelId = null
                                                } else {
                                                    val fromIndex = favoriteChannels.indexOfFirst { it.channel_id == movingChannelId }
                                                    if (fromIndex != -1) {
                                                        viewModel.swapFavoriteChannels(fromIndex, index)
                                                        movingChannelId = null
                                                    }
                                                }
                                            } else {
                                                val intent = Intent(context, ExoPlayJet::class.java).apply {
                                                    putExtra("video_url", channel.channel_url)
                                                    putExtra("key_url", channel.key_url)
                                                    putExtra("zone", "TV")
                                                    val allChannelsData = ArrayList(
                                                        favoriteChannels.map { ch ->
                                                            val fullLogoUrl = if (ch.logoUrl.contains("http")) ch.logoUrl else "$basefinURL/jtvimage/${ch.logoUrl}"
                                                            ChannelInfo(ch.channel_url,ch.key_url, fullLogoUrl, ch.channel_name)
                                                        }
                                                    )
                                                    putParcelableArrayListExtra("channel_list_data", allChannelsData)
                                                    putExtra("current_channel_index", favoriteChannels.indexOf(channel))
                                                    putExtra("logo_url", if (channel.logoUrl.contains("http")) channel.logoUrl else "$basefinURL/jtvimage/${channel.logoUrl}")
                                                    putExtra("ch_name", channel.channel_name)
                                                }
                                                if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                                viewModel.saveToRecents(channel)
                                            }
                                        },
                                        onLongClick = if (!isSortModeActive) {
                                            {
                                                viewModel.toggleFavorite(channel)
                                                Toast.makeText(context, "${channel.channel_name} favorite toggled", Toast.LENGTH_SHORT).show() }
                                        } else null
                                    ),
                                border = if (isBeingMoved) BorderStroke(4.dp, Color.Red)
                                else if (isFocused) BorderStroke(4.dp, Color(0xFFFFD700))
                                else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBeingMoved) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column {
                                        val imageUrl = if (channel.logoUrl.contains("http")) channel.logoUrl else "$basefinURL/jtvimage/${channel.logoUrl}"
                                        GlideImage(
                                            model = imageUrl,
                                            contentDescription = channel.channel_name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(80.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = channel.channel_name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isSortModeActive) Icons.Filled.Edit else Icons.Filled.Star,
                                        contentDescription = "Status",
                                        tint = if (isSortModeActive) Color.Red else Color(0xFFFFD700),
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
            }
        }

        IconButton(
            onClick = {
                isSortModeActive = !isSortModeActive
                if (!isSortModeActive) movingChannelId = null
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (isSortModeActive) Icons.Filled.Check else Icons.Filled.Edit,
                contentDescription = "Toggle Sort Mode",
                tint = if (isSortModeActive) Color.Green else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}