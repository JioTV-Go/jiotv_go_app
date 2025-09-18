package com.skylake.skytv.jgorunner.ui.tvhome.depreciated

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
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
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.tvhome.M3UChannelExp
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("NewApi")
@Composable
fun Main_Layout_3rd_old(context: Context) {
    val preferenceManager = remember { SkySharedPref.getInstance(context) }
    var allChannels by remember { mutableStateOf<List<M3UChannelExp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(allChannels) {
        listOf("All") + allChannels.mapNotNull { it.category }.distinct().sorted()
    }

    val filteredChannels = remember(selectedCategory, allChannels) {
        if (selectedCategory == null || selectedCategory == "All") {
            allChannels
        } else {
            allChannels.filter { it.category == selectedCategory }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val json = preferenceManager.myPrefs.channelListJson
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<List<M3UChannelExp>>() {}.type
                val channels: List<M3UChannelExp> = Gson().fromJson(json, type)
                allChannels = channels.distinctBy { it.url }

                val availableCategories = listOf("All") + allChannels.mapNotNull { it.category }.distinct()
                val savedCategory = preferenceManager.myPrefs.lastSelectedCategoryExp

                selectedCategory = when {
                    availableCategories.contains("  Marathi") -> "  Marathi"
                    savedCategory != null && availableCategories.contains(savedCategory) -> savedCategory
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


    if (isLoading) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularWavyProgressIndicator()
        }
    } else if (allChannels.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("No channels found. Please add an M3U list.")
        }
    } else {
        selectedCategory?.let { currentSelection ->
            Column(modifier = Modifier.fillMaxSize()) {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = currentSelection,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        preferenceManager.myPrefs.lastSelectedCategoryExp = category
                        preferenceManager.savePreferences()
                    }
                )
                ChannelGrid(
                    context = context,
                    channels = filteredChannels
                )
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
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(categories, key = { it }) { category ->
            val isSelected = (selectedCategory == category)
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
private fun ChannelGrid(context: Context, channels: List<M3UChannelExp>) {
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

            ElevatedCard(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .height(120.dp)
                    .scale(scale)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                    .clickable {
                        Log.d("ChannelGrid", "Clicked on: ${channel.name} - ${channel.url}")
                        val channelInfoList = ArrayList(channels.map {
                            ChannelInfo(it.url, it.logo ?: "", it.name)
                        })
                        val currentIndex = channels.indexOf(channel)

                        val intent = Intent(context, ExoPlayJet::class.java).apply {
                            putParcelableArrayListExtra("channel_list_data", channelInfoList)
                            putExtra("current_channel_index", currentIndex)
                        }
                        ContextCompat.startActivity(context, intent, null)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column {
                    GlideImage(
                        model = channel.logo,
                        contentDescription = "${channel.name} logo",
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
