package com.skylake.skytv.jgorunner.ui.tvhome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus
import com.skylake.skytv.jgorunner.utils.withQuality

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChannelGridTV(
    context: Context,
    channels: List<M3UChannelExp>,
//    selectedChannel: M3UChannelExp?,
    onSelectedChannelChanged: (M3UChannelExp) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val preferenceManager = remember { SkySharedPref.getInstance(context) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(channels, key = { it.url }) { channel ->
            var isFocused by remember { mutableStateOf(false) }

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
                        onClick = {
                            Log.d("ChannelGridTV", "Open fullscreen: ${channel.name}")
                            val channelInfoList = ArrayList(channels.map {
                                ChannelInfo(it.url, it.logo ?: "", it.name)
                            })
                            val currentIndex = channels.indexOf(channel)
                            val intent = Intent(context, ExoPlayJet::class.java).apply {
                                putParcelableArrayListExtra("channel_list_data", channelInfoList)
                                putExtra("current_channel_index", currentIndex)
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)


                            // Update recent channels
                            val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                            val type = object : TypeToken<List<Channel>>() {}.type
                            val recentChannels: MutableList<Channel> =
                                if (!recentChannelsJson.isNullOrEmpty())
                                    Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()
                                else
                                    mutableListOf()

                            val clickedAsChannel = Channel(
                                channel_id = extractChannelIdFromPlayUrl(channel.url).toString(),
                                channel_url = channel.url,
                                logoUrl = channel.logo ?: "",
                                channel_name = channel.name,
                                channelCategoryId = 0,
                                channelLanguageId = 0,
                                isHD = true,
                            )

                            val existingIndex =
                                recentChannels.indexOfFirst { it.channel_id == clickedAsChannel.channel_id }

                            if (existingIndex != -1) {
                                val existingChannel = recentChannels[existingIndex]
                                recentChannels.removeAt(existingIndex)
                                recentChannels.add(0, existingChannel)
                            } else {
                                recentChannels.add(0, clickedAsChannel)
                                if (recentChannels.size > 25) {
                                    recentChannels.removeAt(recentChannels.size - 1)
                                }
                            }
                            preferenceManager.myPrefs.recentChannels = Gson().toJson(recentChannels)
                            preferenceManager.savePreferences()
                        },
                        onLongClick = {
                            if (PlayerCommandBus.isInPipMode) {
                                Log.d(
                                    "ChannelGridTV",
                                    "Switch in PiP (long-press): ${channel.name}"
                                )
                                PlayerCommandBus.requestSwitch(url = channel.url)
                            } else {
                                // Fallback to open fullscreen if not in PiP
                                val channelInfoList = ArrayList(channels.map {
                                    ChannelInfo(it.url, it.logo ?: "", it.name)
                                })
                                val currentIndex = channels.indexOf(channel)
                                val intent = Intent(context, ExoPlayJet::class.java).apply {
                                    putParcelableArrayListExtra(
                                        "channel_list_data",
                                        channelInfoList
                                    )
                                    putExtra("current_channel_index", currentIndex)
                                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)

                            }
                        }
                    ),
                border = if (isFocused) BorderStroke(4.dp, Color(0xFFFFD700)) else null,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                GlideImage(
                    model = channel.logo,
                    contentDescription = "${channel.name} logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
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
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChannelGridMain(
    context: Context,
    filteredChannels: List<Channel>,
    selectedChannelSetter: (Channel) -> Unit,
    localPORT: Int,
    preferenceManager: SkySharedPref
) {
    val basefinURL = "http://localhost:$localPORT"
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(filteredChannels) { channel ->
            var isFocused by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .height(120.dp)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            selectedChannelSetter(channel)
                        }
                    }
                    .combinedClickable(
                        onClick = {
                            val intent = Intent(context, ExoPlayJet::class.java).apply {
                                putExtra("video_url", channel.channel_url)
                                putExtra("zone", "TV")

                                val allChannelsData = ArrayList(filteredChannels.map { ch ->
                                    ChannelInfo(
                                        withQuality(context, ch.channel_url),
                                        "$basefinURL/jtvimage/${ch.logoUrl}",
                                        ch.channel_name
                                    )
                                })
                                putParcelableArrayListExtra("channel_list_data", allChannelsData)

                                val currentChannelIndex = filteredChannels.indexOf(channel)
                                putExtra("current_channel_index", currentChannelIndex)
                                putExtra("logo_url", "$basefinURL/jtvimage/${channel.logoUrl}")
                                putExtra("ch_name", channel.channel_name)

                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)

                            // Recent channels logic
                            val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                            val type = object : TypeToken<List<Channel>>() {}.type
                            val recentChannels: MutableList<Channel> =
                                if (!recentChannelsJson.isNullOrEmpty())
                                    Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()
                                else
                                    mutableListOf()
                            val existingIndex =
                                recentChannels.indexOfFirst { it.channel_id == channel.channel_id }
                            if (existingIndex != -1) {
                                val existingChannel = recentChannels[existingIndex]
                                recentChannels.removeAt(existingIndex)
                                recentChannels.add(0, existingChannel)
                            } else {
                                recentChannels.add(0, channel)
                                if (recentChannels.size > 25) recentChannels.removeAt(recentChannels.size - 1)
                            }
                            preferenceManager.myPrefs.recentChannels = Gson().toJson(recentChannels)
                            preferenceManager.savePreferences()
                        },
                        onLongClick = {
                            if (PlayerCommandBus.isInPipMode) {
                                PlayerCommandBus.requestSwitch(url = channel.channel_url)
                                // Also update recents on long-press
                                val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                                val type = object : TypeToken<List<Channel>>() {}.type
                                val recentChannels: MutableList<Channel> =
                                    if (!recentChannelsJson.isNullOrEmpty())
                                        Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()
                                    else
                                        mutableListOf()
                                val existingIndex =
                                    recentChannels.indexOfFirst { it.channel_id == channel.channel_id }
                                if (existingIndex != -1) {
                                    val existingChannel = recentChannels[existingIndex]
                                    recentChannels.removeAt(existingIndex)
                                    recentChannels.add(0, existingChannel)
                                } else {
                                    recentChannels.add(0, channel)
                                    if (recentChannels.size > 25) recentChannels.removeAt(
                                        recentChannels.size - 1
                                    )
                                }
                                preferenceManager.myPrefs.recentChannels =
                                    Gson().toJson(recentChannels)
                                preferenceManager.savePreferences()
                            } else {
                                // Fallback to normal open if not in PiP
                                val intent = Intent(context, ExoPlayJet::class.java).apply {
                                    putExtra("video_url", channel.channel_url)
                                    putExtra("zone", "TV")
                                    val allChannelsData = ArrayList(filteredChannels.map { ch ->
                                        ChannelInfo(
                                            withQuality(context, ch.channel_url),
                                            "$basefinURL/jtvimage/${ch.logoUrl}",
                                            ch.channel_name
                                        )
                                    })
                                    putParcelableArrayListExtra(
                                        "channel_list_data",
                                        allChannelsData
                                    )
                                    val currentChannelIndex = filteredChannels.indexOf(channel)
                                    putExtra("current_channel_index", currentChannelIndex)
                                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }

                                context.startActivity(intent)

                            }
                        }
                    ),
                border = if (isFocused) BorderStroke(4.dp, Color(0xFFFFD700)) else null,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                val logoUrl = channel.logoUrl
                val imageUrl = "$basefinURL/jtvimage/$logoUrl"
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
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
