package com.skylake.skytv.jgorunner.ui.tvhome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Main_Layout(context: Context, reloadTrigger: Int ) {
    val scope = rememberCoroutineScope()
    val channelsResponse = remember { mutableStateOf<ChannelResponse?>(null) }
    val filteredChannels = remember { mutableStateOf<List<Channel>>(emptyList()) }
    val preferenceManager = SkySharedPref.getInstance(context)
    val localPORT by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort ?: 8080)
    }
    val basefinURL = "http://localhost:$localPORT"
    var fetched by remember { mutableStateOf(false) }

    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var epgData by remember { mutableStateOf<EpgProgram?>(null) }

    val focusRequester = remember { FocusRequester() }
    val categoryMap = mapOf(
        "Reset" to null,
        "Entertainment" to 5,
        "Movies" to 6,
        "Kids" to 7,
        "Sports" to 8,
        "Lifestyle" to 9,
        "Infotainment" to 10,
        "News" to 12,
        "Music" to 13,
        "Devotional" to 15,
        "Business" to 16,
        "Educational" to 17,
        "Shopping" to 18,
        "JioDarshan" to 19
    )

    val savedCategoryIds = preferenceManager.myPrefs.filterCI
        ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
    var selectedCategoryIds by remember { mutableStateOf(savedCategoryIds) }

    val sortedCategories = remember(selectedCategoryIds) {
        val resetCategoryName = "Reset"
        val allCategoryNames = categoryMap.keys.toList()
        val otherCategoryNames = allCategoryNames.filter { it != resetCategoryName }
        val (selectedOtherCategories, unselectedOtherCategories) = otherCategoryNames.partition { categoryName ->
            val categoryId = categoryMap[categoryName]
            categoryId != null && selectedCategoryIds.contains(categoryId)
        }
        listOf(resetCategoryName) + selectedOtherCategories + unselectedOtherCategories
    }

    // Fetch and filter channels (cache/network)
    LaunchedEffect(reloadTrigger) {
        val sharedPref = context.getSharedPreferences("channel_cache", Context.MODE_PRIVATE)
        var useCache = true
        var cachedChannels: ChannelResponse? = null

        val cachedJson = sharedPref.getString("channels_json", null)
        if (!cachedJson.isNullOrEmpty()) {
            try {
                cachedChannels = Gson().fromJson(cachedJson, ChannelResponse::class.java)
                channelsResponse.value = cachedChannels
            } catch (e: Exception) {
                with(sharedPref.edit()) {
                    remove("channels_json")
                    apply()
                }
                useCache = false
            }
        } else {
            useCache = false
        }

        if (useCache && cachedChannels != null) {
            val categories = preferenceManager.myPrefs.filterCI
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            val languages = preferenceManager.myPrefs.filterLI
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

            val filtered = when {
                categories.isNullOrEmpty() && languages.isNullOrEmpty() -> ChannelUtils.filterChannels(cachedChannels)
                categories.isNullOrEmpty() -> ChannelUtils.filterChannels(
                    cachedChannels,
                    languageIds = languages?.mapNotNull { it.toIntOrNull() }?.takeIf { it.isNotEmpty() }
                )
                languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                    cachedChannels,
                    categoryIds = categories.mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() }
                )
                else -> ChannelUtils.filterChannels(
                    cachedChannels,
                    categoryIds = categories.mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() },
                    languageIds = languages.mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() }
                )
            }
            filteredChannels.value = filtered ?: emptyList()
            fetched = true
        } else {
            var attempts = 0
            var success = false
            while (attempts < 2 && !success) {
                attempts++
                try {
                    val response = ChannelUtils.fetchChannels("$basefinURL/channels")
                    channelsResponse.value = response
                    if (response != null) {
                        val responseJsonString = Gson().toJson(response)
                        with(sharedPref.edit()) {
                            putString("channels_json", responseJsonString)
                            apply()
                        }
                        val categories = preferenceManager.myPrefs.filterCI
                            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                        val languages = preferenceManager.myPrefs.filterLI
                            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

                        val filtered = when {
                            categories.isNullOrEmpty() && languages.isNullOrEmpty() -> ChannelUtils.filterChannels(response)
                            categories.isNullOrEmpty() -> ChannelUtils.filterChannels(
                                response,
                                languageIds = languages?.mapNotNull { it.toIntOrNull() }?.takeIf { it.isNotEmpty() }
                            )
                            languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                                response,
                                categoryIds = categories.mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() }
                            )
                            else -> ChannelUtils.filterChannels(
                                response,
                                categoryIds = categories.mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() },
                                languageIds = languages.mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() }
                            )
                        }
                        filteredChannels.value = filtered ?: emptyList()
                        success = true
                    }
                } catch (e: Exception) {
                    // ignore, retry
                }
                if (!success) {
                    kotlinx.coroutines.delay(300)
                }
            }
            fetched = true
        }

        if (preferenceManager.myPrefs.startTvAutomatically) {
            if (!AppStartTracker.shouldPlayChannel &&
                filteredChannels.value.isNotEmpty()) {

                val firstChannel = filteredChannels.value.first()
                val intent = Intent(context, ExoPlayJet::class.java).apply {
                    putExtra("zone", "TV")
                    putParcelableArrayListExtra("channel_list_data", ArrayList(
                        filteredChannels.value.map { ch ->
                            ChannelInfo(
                                ch.channel_url ?: "",
                                "http://localhost:$localPORT/jtvimage/${ch.logoUrl ?: ""}",
                                ch.channel_name ?: ""
                            )
                        }
                    ))
                    putExtra("current_channel_index", 0) // Index 0 for the first channel
                    putExtra("video_url", firstChannel.channel_url ?: "")
                    putExtra(
                        "logo_url",
                        "http://localhost:$localPORT/jtvimage/${firstChannel.logoUrl ?: ""}"
                    )
                    putExtra("ch_name", firstChannel.channel_name ?: "")
                }
                kotlinx.coroutines.delay(1000)
                startActivity(context, intent, null)

                //Recent Channels
                val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                val type = object : TypeToken<List<Channel>>() {}.type
                val recentChannels: MutableList<Channel> =
                    Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()

                val existingIndex =
                    recentChannels.indexOfFirst { it.channel_id == firstChannel.channel_id }

                if (existingIndex != -1) {
                    val existingChannel = recentChannels[existingIndex]
                    recentChannels.removeAt(existingIndex)
                    recentChannels.add(0, existingChannel)
                } else {
                    recentChannels.add(0, firstChannel)
                    if (recentChannels.size > 25) {
                        recentChannels.removeAt(recentChannels.size - 1)
                    }
                }
                preferenceManager.myPrefs.recentChannels = Gson().toJson(recentChannels)
                preferenceManager.savePreferences()
            }

            AppStartTracker.shouldPlayChannel = true
        }

    }

    // Re-filter channels when category changes
    LaunchedEffect(selectedCategoryIds) {
        channelsResponse.value?.let { response ->
            val languages = preferenceManager.myPrefs.filterLI
                ?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.isNotEmpty() }

            val filtered = ChannelUtils.filterChannels(
                response,
                categoryIds = selectedCategoryIds.takeIf { it.isNotEmpty() }?.toList(),
                languageIds = languages
            )
            filteredChannels.value = filtered ?: emptyList()
        }
    }

    // Fetch EPG data for selected channel
    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            val epgURLc = "$basefinURL/epg/${channel.channel_id ?: ""}/0"
            Log.d("NANOdix",epgURLc)
            epgData = ChannelUtils.fetchEpg(epgURLc)
        } ?: run {
            epgData = null
        }
    }

    // UI: Loading state
    if (!fetched) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
//            CircularWavyProgressIndicator(modifier = Modifier.size(60.dp))
            ContainedLoadingIndicator(modifier = Modifier.size(100.dp))
        }
    }
    // UI: Empty state
    else if (filteredChannels.value.isNullOrEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No channels found",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Try the following steps:",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\n• Check your internet connection\n• Reload the app\n• Go to Main Screen for detailed information",
                    style = TextStyle(fontSize = 15.sp, color = Color.Gray)
                )
                Spacer(modifier = Modifier.height(24.dp))
                ElevatedCard(
                    onClick = {
                        (context as? Activity)?.let { activity ->
                            val intent = activity.intent
                            activity.finish()
                            activity.startActivity(intent)
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reload"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reload App")
                    }
                }
            }
        }
    }
    // UI: Main content
    else {
        // CATEGORY CHIPS
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sortedCategories) { categoryName ->
                val categoryId = categoryMap[categoryName]
                val isSelected = categoryId != null && selectedCategoryIds.contains(categoryId)

                FilterChip(
                    onClick = {
                        if (categoryName == "Reset") {
                            selectedCategoryIds = mutableSetOf()
                        } else if (categoryId != null) {
                            val newSelection = if (isSelected) {
                                (selectedCategoryIds - categoryId).toMutableSet()
                            } else {
                                (selectedCategoryIds + categoryId).toMutableSet()
                            }
                            selectedCategoryIds = newSelection
                        }
                        val updatedCI = selectedCategoryIds.joinToString(",")
                        preferenceManager.myPrefs.filterCI = updatedCI
                        preferenceManager.savePreferences()
                    },
                    label = { Text(categoryName ?: "Unknown") },
                    selected = isSelected,
                    leadingIcon = when {
                        categoryName == "Reset" -> {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reset icon",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        }
                        isSelected -> {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Done icon",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        }
                        else -> null
                    },
                )
            }
        }

        // EPG CARD (null-safe)
        epgData?.let { epg ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = epg.channel_name ?: "Unknown Channel",
                                style = TextStyle(fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = epg.showname ?: "No Program Info",
                                maxLines = 1,
                                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = epg.description ?: "No description available",
                                style = TextStyle(fontSize = 13.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        epg.episodePoster?.let { poster ->
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

        ChannelGridMain(
            context = context,
            filteredChannels = filteredChannels.value ?: emptyList(),
            selectedChannelSetter = { selectedChannel = it },
            localPORT = localPORT,
            preferenceManager = preferenceManager
        )


    }
}
