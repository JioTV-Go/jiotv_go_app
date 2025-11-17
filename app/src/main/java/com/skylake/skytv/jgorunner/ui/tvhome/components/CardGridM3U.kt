package com.skylake.skytv.jgorunner.ui.tvhome.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.skylake.skytv.jgorunner.ui.tvhome.M3UChannelExp
import com.skylake.skytv.jgorunner.ui.tvhome.extractChannelIdFromPlayUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun M3UChannelGridScreen(
    context: Context,
    channels: List<M3UChannelExp>,
    selectedChannelSetter: (M3UChannelExp) -> Unit
) {
    val groupedCategories = remember(channels) {
        channels.groupBy { channel ->
            channel.category?.takeIf { it.isNotBlank() } ?: "Other"
        }
    }
    val orderedCategories = remember(groupedCategories) {
        groupedCategories.keys.sorted()
    }
    val firstCategory = remember(orderedCategories, groupedCategories) {
        orderedCategories.firstOrNull { groupedCategories[it]?.isNotEmpty() == true }
            ?: orderedCategories.firstOrNull()
    }
    var lastFocusedCategoryName by rememberSaveable { mutableStateOf(firstCategory) }
    var selectedChannelUrl by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(orderedCategories, firstCategory) {
        if (lastFocusedCategoryName != null && lastFocusedCategoryName !in orderedCategories) {
            lastFocusedCategoryName = null
        }
        if (lastFocusedCategoryName == null) {
            lastFocusedCategoryName = firstCategory
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
        itemsIndexed(orderedCategories, key = { _, category -> category }) { index, categoryName ->
            val categoryChannels = groupedCategories[categoryName].orEmpty()
            val requestInitialFocus = when {
                lastFocusedCategoryName != null -> lastFocusedCategoryName == categoryName
                firstCategory == null -> index == 0
                else -> firstCategory == categoryName
            }

            M3UCategoryRow(
                context = context,
                categoryName = categoryName,
                channels = categoryChannels,
                allChannels = channels,
                selectedChannelSetter = selectedChannelSetter,
                requestInitialFocus = requestInitialFocus,
                onCategoryFocused = { focusedName -> lastFocusedCategoryName = focusedName },
                selectedChannelUrl = selectedChannelUrl,
                onChannelSelected = { url -> selectedChannelUrl = url },
                isFirstCategory = index == 0
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun M3UCategoryRow(
    context: Context,
    categoryName: String,
    channels: List<M3UChannelExp>,
    allChannels: List<M3UChannelExp>,
    selectedChannelSetter: (M3UChannelExp) -> Unit,
    requestInitialFocus: Boolean,
    onCategoryFocused: (String) -> Unit,
    selectedChannelUrl: String?,
    onChannelSelected: (String) -> Unit,
    isFirstCategory: Boolean
) {
    if (channels.isEmpty()) return
    val isTvDevice = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentFocusedIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(channels.size) { FocusRequester() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
    ) {
        Text(
            text = categoryName,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(channels, key = { _, ch -> ch.url }) { index, channel ->
                var isFocused by remember { mutableStateOf(false) }

                LaunchedEffect(isFocused) {
                    if (isFocused) {
                        currentFocusedIndex = index
                        onCategoryFocused(categoryName)
                    }
                }

                LaunchedEffect(requestInitialFocus) {
                    if (requestInitialFocus && index == currentFocusedIndex) {
                        delay(150)
                        focusRequesters[currentFocusedIndex].requestFocus()
                    }
                }

                M3UChannelCard(
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .width(120.dp)
                        .height(90.dp)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionCenter, Key.Enter -> {
                                        selectedChannelSetter(channel)
                                        handleM3UChannelPlay(context, channel, allChannels)
                                        true
                                    }

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


                                    else -> false
                                }
                            } else false
                        },
                    channel = channel,
                    isHighlighted = isTvDevice && isFocused,
                    onClick = {
                        selectedChannelSetter(channel)
                        handleM3UChannelPlay(context, channel, allChannels)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun M3UChannelCard(
    modifier: Modifier,
    channel: M3UChannelExp,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "focusScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        border = if (isHighlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlideImage(
                model = channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = channel.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun handleM3UChannelPlay(
    context: Context,
    channel: M3UChannelExp,
    allChannels: List<M3UChannelExp>
) {
    val channelInfoList = ArrayList(allChannels.map {
        ChannelInfo(it.url, it.logo ?: "", it.name)
    })
    val currentIndex = allChannels.indexOfFirst { it.url == channel.url }.takeIf { it >= 0 } ?: 0
    val intent = Intent(context, ExoPlayJet::class.java).apply {
        putParcelableArrayListExtra("channel_list_data", channelInfoList)
        putExtra("current_channel_index", currentIndex)
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(intent)

    // Recent Channels
    val preferenceManager = SkySharedPref.getInstance(context)
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
        channelCategoryId= 0,
        channelLanguageId= 0,
        isHD= true,
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

}