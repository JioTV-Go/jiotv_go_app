package com.skylake.skytv.jgorunner.ui.tvhome.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.skylake.skytv.jgorunner.ui.tvhome.Channel
import com.skylake.skytv.jgorunner.ui.tvhome.EpgProgram
import kotlinx.coroutines.delay


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChannelGridScreen(
    context: Context,
    filteredChannels: List<Channel>,
    categoryMap: Map<String, Int?>,
    selectedChannelSetter: (Channel) -> Unit,
    localPORT: Int,
    preferenceManager: SkySharedPref,
    epgData: EpgProgram?
) {
    val basefinURL = remember(localPORT) { "http://localhost:$localPORT" }
    val categories = remember(categoryMap) {
        categoryMap.filter { (name, id) -> name != "Reset" && id != null }.toList()
    }
    val groupedChannels = remember(filteredChannels) {
        filteredChannels.groupBy { it.channelCategoryId }
    }
    val firstCategoryToFocus = remember(categories, groupedChannels) {
        categories.firstOrNull { groupedChannels[it.second]?.isNotEmpty() == true }
            ?: categories.firstOrNull()
    }
    var lastFocusedCategoryId by rememberSaveable { mutableStateOf(firstCategoryToFocus?.second) }
    var selectedChannelId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(categories, firstCategoryToFocus) {
        val validCategoryIds = categories.mapNotNull { it.second }.toSet()
        if (lastFocusedCategoryId != null && lastFocusedCategoryId !in validCategoryIds) {
            lastFocusedCategoryId = null
        }
        if (lastFocusedCategoryId == null) {
            lastFocusedCategoryId = firstCategoryToFocus?.second
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
            .focusProperties { canFocus = false },
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        itemsIndexed(
            categories,
            key = { _, entry -> entry.second ?: -1 }) { index, (categoryName, categoryId) ->
            val channelsInCategory = groupedChannels[categoryId].orEmpty()
            val requestInitialFocus = when {
                lastFocusedCategoryId != null -> lastFocusedCategoryId == categoryId
                firstCategoryToFocus == null -> index == 0
                else -> firstCategoryToFocus.second == categoryId
            }

            CategoryRow(
                context = context,
                categoryName = categoryName,
                categoryId = categoryId,
                channels = channelsInCategory,
                allChannels = filteredChannels,
                basefinURL = basefinURL,
                preferenceManager = preferenceManager,
                selectedChannelSetter = selectedChannelSetter,
                requestInitialFocus = requestInitialFocus,
                onCategoryFocused = { focusedId -> lastFocusedCategoryId = focusedId },
                selectedChannelId = selectedChannelId,
                onChannelSelected = { channelId -> selectedChannelId = channelId },
                isFirstCategory = index == 0,
                epgData = epgData
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CategoryRow(
    context: Context,
    categoryName: String,
    categoryId: Int?,
    channels: List<Channel>,
    allChannels: List<Channel>,
    basefinURL: String,
    preferenceManager: SkySharedPref,
    selectedChannelSetter: (Channel) -> Unit,
    requestInitialFocus: Boolean,
    onCategoryFocused: (Int?) -> Unit,
    selectedChannelId: String?,
    onChannelSelected: (String) -> Unit,
    isFirstCategory: Boolean,
    epgData: EpgProgram?
) {
    if (channels.isEmpty()) return
    val baseCardWidth = 120.dp
    val focusedCardWidth = 220.dp
    val cardHeight = 90.dp
    val cardSpacing = 8.dp
    val rowPadding = 20.dp
    val isTvDevice = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    rememberCoroutineScope()
    val listState = rememberLazyListState()
    var lastFocusedIndex by rememberSaveable(categoryId) { mutableIntStateOf(0) }
    val focusRequesters = remember(channels.size) { List(channels.size) { FocusRequester() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
    ) {
        Text(
            text = categoryName,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            contentPadding = PaddingValues(horizontal = rowPadding)
        ) {
            itemsIndexed(channels, key = { _, ch -> ch.channel_id }) { index, channel ->
                var isFocused by remember { mutableStateOf(false) }

                LaunchedEffect(isFocused) {
                    if (isFocused) {
                        lastFocusedIndex = index
                        onCategoryFocused(categoryId)
                    }
                }

                LaunchedEffect(requestInitialFocus) {
                    if (requestInitialFocus && index == lastFocusedIndex) {
                        delay(150)
                        focusRequesters[lastFocusedIndex].requestFocus()
                    }
                }


                val epgForChannel =
                    if (epgData?.channel_name == channel.channel_name) epgData else null
                val programName = epgForChannel?.showname
                val programInfo = epgForChannel?.description

                ChannelCard(
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .width(if (isFocused) focusedCardWidth else baseCardWidth)
                        .height(cardHeight)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (isFocused) {
                                selectedChannelSetter(channel)
                            }
                        }
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        if (index == 0 && !isFirstCategory) {
                                            true
                                        } else if (index > 0) {
                                            focusRequesters[index - 1].requestFocus()
                                            true
                                        } else false
                                    }

                                    Key.DirectionRight -> {
                                        if (index == channels.lastIndex && !isFirstCategory) {
                                            true
                                        } else if (index < channels.lastIndex) {
                                            focusRequesters[index + 1].requestFocus()
                                            true
                                        } else false
                                    }

                                    Key.DirectionCenter, Key.Enter -> {
                                        selectedChannelSetter(channel)
                                        handleChannelPlay(
                                            context = context,
                                            channel = channel,
                                            basefinURL = basefinURL,
                                            allChannels = allChannels,
                                            preferenceManager = preferenceManager
                                        )
                                        true
                                    }

                                    else -> false
                                }
                            } else false
                        },
                    channel = channel,
                    basefinURL = basefinURL,
                    isHighlighted = isTvDevice && isFocused,
                    programName = programName,
                    programInfo = programInfo,
                    onClick = {
                        selectedChannelSetter(channel)
                        handleChannelPlay(
                            context = context,
                            channel = channel,
                            basefinURL = basefinURL,
                            allChannels = allChannels,
                            preferenceManager = preferenceManager
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ChannelCard(
    modifier: Modifier,
    channel: Channel,
    basefinURL: String,
    isHighlighted: Boolean,
    programName: String? = null,
    programInfo: String? = null,
    onClick: () -> Unit
) {
    val goldenColor = Color(0xFFFFD700)

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = if (isHighlighted) BorderStroke(3.dp, goldenColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 8.dp else 4.dp)
    ) {
        if (programName == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlideImage(
                    model = "$basefinURL/jtvimage/${channel.logoUrl}",
                    contentDescription = channel.channel_name,
                    modifier = Modifier
                        .width(60.dp)
                        .height(50.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = channel.channel_name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Column(
                    modifier = Modifier.width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlideImage(
                        model = "$basefinURL/jtvimage/${channel.logoUrl}",
                        contentDescription = channel.channel_name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.channel_name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.Top) {
                    Text(
                        text = programName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = programInfo ?: "",
                        fontSize = 10.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun handleChannelPlay(
    context: Context,
    channel: Channel,
    basefinURL: String,
    allChannels: List<Channel>,
    preferenceManager: SkySharedPref
) {
    val intent = Intent(context, ExoPlayJet::class.java).apply {
        putExtra("video_url", channel.channel_url)
        putExtra("zone", "TV")

        val allChannelsData = ArrayList(allChannels.map { ch ->
            ChannelInfo(
                ch.channel_url,
                "$basefinURL/jtvimage/${ch.logoUrl}",
                ch.channel_name
            )
        })
        putParcelableArrayListExtra("channel_list_data", allChannelsData)
        val currentChannelIndex = allChannels.indexOfFirst { it.channel_id == channel.channel_id }
        putExtra("current_channel_index", currentChannelIndex.coerceAtLeast(0))
        putExtra("logo_url", "$basefinURL/jtvimage/${channel.logoUrl}")
        putExtra("ch_name", channel.channel_name)

        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    context.startActivity(intent)

    // --- Save Recent Channels ---
    val recentChannelsJson = preferenceManager.myPrefs.recentChannels
    val type = object : TypeToken<MutableList<Channel>>() {}.type
    val recentChannels: MutableList<Channel> = if (!recentChannelsJson.isNullOrEmpty()) {
        Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()
    } else {
        mutableListOf()
    }
    val existingIndex = recentChannels.indexOfFirst { it.channel_id == channel.channel_id }
    if (existingIndex >= 0) {
        val existingChannel = recentChannels.removeAt(existingIndex)
        recentChannels.add(0, existingChannel)
    } else {
        recentChannels.add(0, channel)
        if (recentChannels.size > 25) {
            recentChannels.removeAt(recentChannels.lastIndex)
        }
    }
    preferenceManager.myPrefs.recentChannels = Gson().toJson(recentChannels)
    preferenceManager.savePreferences()
}
