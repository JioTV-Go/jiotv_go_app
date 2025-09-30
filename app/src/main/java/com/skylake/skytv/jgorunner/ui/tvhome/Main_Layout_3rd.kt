package com.skylake.skytv.jgorunner.ui.tvhome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.tv.material3.CardDefaults as CardDefaultsTV
import androidx.tv.material3.ClassicCard
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat.startActivity
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Main_Layout_3rd(context: Context, reloadTrigger: Int ) {
    val preferenceManager = remember { SkySharedPref.getInstance(context) }
    var allChannels by remember { mutableStateOf<List<M3UChannelExp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val localPORT by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort ?: 8080)
    }
    val basefinURL = "http://localhost:$localPORT"
    var selectedChannel by remember { mutableStateOf<M3UChannelExp?>(null) }
    var epgData by remember { mutableStateOf<EpgProgram?>(null) }
    val epgDebugVar by remember { mutableStateOf(preferenceManager.myPrefs.epgDebug) }

    val categories = remember(allChannels) {
        listOf("All") + allChannels.mapNotNull { it.category }.distinct().sorted()
    }
    // Reorder for display: keep "All" first, then currently selected categories, then the rest
    val categoriesDisplay = remember(categories, selectedCategory) {
        val selectedSet = selectedCategory
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it != "All" }
            ?.toSet()
            ?: emptySet()
        val withoutAll = categories.filter { it != "All" }
        val selectedFirst = withoutAll.filter { it in selectedSet }
        val unselectedLater = withoutAll.filter { it !in selectedSet }
        listOf("All") + selectedFirst + unselectedLater
    }

    val filteredChannels = remember(selectedCategory, allChannels) {
        val sels = selectedCategory
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it != "All" }
            ?.toSet()
            ?: emptySet()
        if (selectedCategory == null || selectedCategory == "All" || sels.isEmpty()) {
            allChannels
        } else {
            allChannels.filter { ch -> ch.category?.let { it in sels } == true }
        }
    }


    LaunchedEffect(reloadTrigger) {
        isLoading = true
        try {
            val json = preferenceManager.myPrefs.channelListJson
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<List<M3UChannelExp>>() {}.type
                val channels: List<M3UChannelExp> = Gson().fromJson(json, type)
                allChannels = channels.distinctBy { it.url }

                val availableCategories = listOf("All") + allChannels.mapNotNull { it.category }.distinct()
                // Read saved value from existing key
                val savedValue = preferenceManager.myPrefs.lastSelectedCategoryExp ?: "All"
                val savedSet = savedValue.split(',').map { it.trim() }.filter { it.isNotEmpty() && it != "All" }.toSet()
                val intersect = savedSet.intersect(availableCategories.toSet())
                selectedCategory = when {
                    intersect.isNotEmpty() -> intersect.joinToString(",")
                    availableCategories.contains("  Marathi") -> "  Marathi"
                    else -> "All"
                }
            } else {
                Log.d("TVChannelsScreen", "Channel list JSON is empty.")
                selectedCategory = "All"
            }
        } catch (e: Exception) {
            Log.e("TVChannelsScreen", "Failed to load or parse channels.", e)
            selectedCategory = "All"
        }
        isLoading = false
    }


    LaunchedEffect(filteredChannels) {
        if (preferenceManager.myPrefs.startTvAutomatically &&
            !AppStartTracker.shouldPlayChannel &&
            filteredChannels.isNotEmpty()
        ) {
            val firstChannel = filteredChannels.first()

            val intent = Intent(context, ExoPlayJet::class.java).apply {
                putExtra("zone", "TV")
                putParcelableArrayListExtra("channel_list_data", ArrayList(
                    filteredChannels.map { ch ->
                        ChannelInfo(ch.url, ch.logo ?: "", ch.name)
                    }
                ))
                putExtra("current_channel_index", 0)
                putExtra("video_url", firstChannel.url)
                putExtra("logo_url", firstChannel.logo ?: "")
                putExtra("ch_name", firstChannel.name)
            }

            kotlinx.coroutines.delay(1000)
            startActivity(context, intent, null)

            AppStartTracker.shouldPlayChannel = true
        }
    }

    // Fetch EPG data for selected channel
    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            val chID = extractChannelIdFromPlayUrl(channel.url)
            val epgURLc = "$basefinURL/epg/${chID ?: ""}/0"
            Log.d("NANOdix3rd",epgURLc)
            epgData = ChannelUtils.fetchEpg(epgURLc)
        } ?: run {
            epgData = null
        }
    }



    if (isLoading) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
//            CircularWavyProgressIndicator()
            ContainedLoadingIndicator(modifier = Modifier.size(100.dp))
        }
    } else if (allChannels.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "No channels found",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please add an M3U playlist to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
//                Spacer(modifier = Modifier.height(24.dp))
//                Button(
//                    onClick = {
//                        (context as? Activity)?.let { activity ->
//                            val intent = activity.intent
//                            activity.finish()
//                            activity.startActivity(intent)
//                        }
//                    },
//                    modifier = Modifier.padding(horizontal = 24.dp)
//                ) {
//                    Icon(
//                        imageVector = Icons.Filled.AddLink,
//                        contentDescription = "Add Playlist",
//                        modifier = Modifier.size(18.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("Add M3U Playlist")
//                }
            }
        }

    } else {
        run {
            Column(modifier = Modifier.fillMaxSize()) {
                CategoryFilterRow(
                    categories = categoriesDisplay,
                    selectedCategory = selectedCategory ?: "All",
                    onCategorySelected = { category ->
                        selectedCategory = if (category == "All") {
                            "All"
                        } else {
                            val current = selectedCategory
                                ?.split(',')
                                ?.map { it.trim() }
                                ?.filter { it.isNotEmpty() && it != "All" }
                                ?.toMutableSet() ?: mutableSetOf()
                            if (current.contains(category)) current.remove(category) else current.add(category)
                            if (current.isEmpty()) "All" else current.joinToString(",")
                        }
                        preferenceManager.myPrefs.lastSelectedCategoryExp = selectedCategory ?: "All"
                        preferenceManager.savePreferences()
                    }
                )
                //////////

                if (epgDebugVar) {
                    if (epgData == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select channel to show EPG",
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                                color = Color.Gray
                            )
                        }
                    } else {
                        val epg = epgData
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp, end = 12.dp)
                                    ) {
                                        Text(
                                            text = epg?.channel_name ?: "Unknown Channel",
                                            style = TextStyle(fontSize = 14.sp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = epg?.showname ?: "No Program Info",
                                            maxLines = 1,
                                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = epg?.description ?: "No description available",
                                            style = TextStyle(fontSize = 13.sp),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    epg?.episodePoster?.let { poster ->
                                        GlideImage(
                                            model = "$basefinURL/jtvposter/$poster",
                                            contentDescription = null,
                                            modifier = Modifier
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty
                }


                ChannelGridTV(
                    context = context,
                    channels = filteredChannels,
                    selectedChannel = selectedChannel,
                    onSelectedChannelChanged = { channel -> selectedChannel = channel }
                )

                ///////////
//                ChannelGrid(
//                    context = context,
//                    channels = filteredChannels,
//                    selectedChannel = selectedChannel,
//                    onSelectedChannelChanged = { channel -> selectedChannel = channel }
//                )
            }
        }
    }
}


@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    // For UI highlighting: derive set from selectedCategory string
    val selectedSet = selectedCategory
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "All" }
        .toSet()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(categories, key = { it }) { category ->
            val isSelected = if (category == "All") selectedSet.isEmpty() else selectedSet.contains(category)
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Done, contentDescription = "Selected") }
                } else {
                    null
                }
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ChannelGrid(
    context: Context,
    channels: List<M3UChannelExp>,
    selectedChannel: M3UChannelExp?,
    onSelectedChannelChanged: (M3UChannelExp) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(channels, key = { it.url }) { channel ->
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.1f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "scaleAnimation"
            )

            ClassicCard(
                modifier = Modifier
                    .height(120.dp)
                    .scale(scale)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onSelectedChannelChanged(channel)
                        }
                    },
                image = {
                    GlideImage(
                        model = channel.logo,
                        contentDescription = "${channel.name} logo",
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                title = {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                },
                onClick = {
                    Log.d("ChannelGrid", "Clicked on: ${channel.name} - ${channel.url}")
                    val channelInfoList = ArrayList(channels.map {
                        ChannelInfo(it.url, it.logo ?: "", it.name)
                    })
                    val currentIndex = channels.indexOf(channel)

                    val intent = Intent(context, ExoPlayJet::class.java).apply {
                        putParcelableArrayListExtra("channel_list_data", channelInfoList)
                        putExtra("current_channel_index", currentIndex)
                    }
                    startActivity(context, intent, null)
                },
                colors = CardDefaultsTV.colors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            )
        }
    }
}
