package com.skylake.skytv.jgorunner.ui.tvhome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker
import com.skylake.skytv.jgorunner.ui.tvhome.components.M3UChannelGridScreen
import com.skylake.skytv.jgorunner.ui.screens.restartAppV1
import com.skylake.skytv.jgorunner.ui.tvhome.components.TvScreenMenu

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Main_Layout_3rd(context: Context,reloadTrigger: Int,layoutModeOverride: String? = null) {
    val preferenceManager = remember { SkySharedPref.getInstance(context) }
    var allChannels by remember { mutableStateOf<List<M3UChannelExp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val localPORT by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort)
    }
    val basefinURL = "http://localhost:$localPORT"
    var selectedChannel by remember { mutableStateOf<M3UChannelExp?>(null) }
    var epgData by remember { mutableStateOf<EpgProgram?>(null) }
    val layoutMode = layoutModeOverride ?: preferenceManager.myPrefs.tvLayoutMode ?: "Default"
    val epgDebugVar by remember { mutableStateOf(preferenceManager.myPrefs.epgDebug) }

    var showDialog by remember { mutableStateOf(false) }

    var selectedCategories2 by remember {
        mutableStateOf(preferenceManager.myPrefs.lastSelectedCategoriesExp?.let {
            Gson().fromJson(it, object : TypeToken<List<String>>() {}.type)
        } ?: listOf("All"))
    }

    val categoriesList = remember(allChannels) {
        listOf("All") + allChannels.mapNotNull { it.category }.distinct().sorted()
    }

    val filteredChannels = remember(selectedCategories2, allChannels) {
        if (selectedCategories2.contains("All") || selectedCategories2.isEmpty()) {
            allChannels
        } else {
            allChannels.filter { it.category in selectedCategories2 }
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

                val availableCategories =
                    listOf("All") + allChannels.mapNotNull { it.category }.distinct()
                val savedCategory = preferenceManager.myPrefs.lastSelectedCategoryExp

                selectedCategory = when {
                    availableCategories.contains("  Marathi") -> "  Marathi"
                    savedCategory != null && availableCategories.contains(savedCategory) -> savedCategory
                    else -> "All"
                }

                selectedCategories2 = preferenceManager.myPrefs.lastSelectedCategoriesExp?.let {
                    Gson().fromJson(it, object : TypeToken<List<String>>() {}.type)
                } ?: listOf("All")

            } else {
                Log.d("TVChannelsScreen", "Channel list JSON is empty.")
                selectedCategory = "All"
                selectedCategories2 = listOf("All")
            }
        } catch (e: Exception) {
            Log.e("TVChannelsScreen", "Failed to load or parse channels.", e)
            selectedCategory = "All"
            selectedCategories2 = listOf("All")
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
                putParcelableArrayListExtra(
                    "channel_list_data",
                    ArrayList(
                        filteredChannels.map { ch ->
                            ChannelInfo(ch.url, ch.logo ?: "", ch.name)
                        }
                    )
                )
                putExtra("current_channel_index", 0)
                putExtra("video_url", firstChannel.url)
                putExtra("logo_url", firstChannel.logo ?: "")
                putExtra("ch_name", firstChannel.name)
            }

            kotlinx.coroutines.delay(1000)

            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            AppStartTracker.shouldPlayChannel = true
        }
    }

    // Fetch EPG data for selected channel
    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            val chID = extractChannelIdFromPlayUrl(channel.url)
            val epgURLc = "$basefinURL/epg/${chID ?: ""}/0"
            Log.d("NANOdix3rd", epgURLc)
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
                ////---
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
//                        (context as? Activity)?.let { activity ->
//                            val intent = activity.intent
//                            activity.finish()
//                            activity.startActivity(intent)
//                        }

                        Log.d("ninit", "hello from m3u")


                        showDialog = true

                    },
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddLink,
                        contentDescription = "Add Playlist",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add M3U Playlist")
                }

                if (showDialog) {
                    TvScreenMenu(
                        showDialog = true,
                        context = context,
                        onDismiss = { showDialog = false },
                        onReset = {
                            showDialog = false
                        },
                        onSelectionsMade = { quality, layoutMode, categoryNames, categoryIds, languageNames, languageIds ->
                            Toast.makeText(context, "Restarting App!", Toast.LENGTH_LONG).show()
                            restartAppV1(context)
                        }
                    )
                }

                ////---
            }
        }

    } else {
        selectedCategory?.let { currentSelection ->
            Column(modifier = Modifier.fillMaxSize()) {
                CategoryFilterRow(
                    categories = categoriesList,
                    selectedCategories2 = selectedCategories2,
                    onSelectedCategoriesChange = { newSelection ->
                        selectedCategories2 = newSelection
                        preferenceManager.myPrefs.lastSelectedCategoriesExp =
                            Gson().toJson(newSelection)
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
                                            style = TextStyle(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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

                if (layoutMode.equals("CardUI(TV)", ignoreCase = true)) {
                    M3UChannelGridScreen(
                        context = context,
                        channels = filteredChannels,
                        selectedChannelSetter = { selectedChannel = it }
                    )
                } else {
                    ChannelGridTV(
                        context = context,
                        channels = filteredChannels,
                        onSelectedChannelChanged = { channel -> selectedChannel = channel }
                    )
                }

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
    selectedCategories2: List<String>,
    onSelectedCategoriesChange: (List<String>) -> Unit
) {
    val reorderedCategories = remember(categories, selectedCategories2) {
        val allCategory = categories.find { it == "All" }
        val selectedExceptAll = selectedCategories2.filter { it != "All" && it in categories }
        val others = categories.filter { it != "All" && it !in selectedCategories2 }
        listOfNotNull(allCategory) + selectedExceptAll + others
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(reorderedCategories, key = { it }) { category ->
            val isSelected = selectedCategories2.contains(category)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newSelection = selectedCategories2.toMutableList()
                    if (category == "All") {
                        newSelection.clear()
                        newSelection.add("All")
                    } else {
                        if (isSelected) {
                            newSelection.remove(category)
                            if (newSelection.isEmpty()) {
                                newSelection.add("All")
                            }
                        } else {
                            newSelection.add(category)
                            newSelection.remove("All")
                        }
                    }
                    onSelectedCategoriesChange(newSelection)
                },
                label = { Text(category) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Done, contentDescription = "Selected") }
                } else null
            )
        }
    }
}
