package com.skylake.skytv.jgorunner.ui.tvhome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker
import com.skylake.skytv.jgorunner.ui.tvhome.components.ChannelGridScreen
import com.skylake.skytv.jgorunner.utils.withQuality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Main_Layout(context: Context, reloadTrigger: Int, layoutModeOverride: String? = null) {
    val scope = rememberCoroutineScope()
    val channelsResponse = remember { mutableStateOf<ChannelResponse?>(null) }
    val filteredChannels = remember { mutableStateOf<List<Channel>>(emptyList()) }
    val preferenceManager = SkySharedPref.getInstance(context)
    val localPORT by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort)
    }
    val basefinURL = "http://localhost:$localPORT"
    var fetched by remember { mutableStateOf(false) }

    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var epgData by remember { mutableStateOf<EpgProgram?>(null) }
    val layoutMode = layoutModeOverride ?: preferenceManager.myPrefs.tvLayoutMode ?: "Default"
    var isEpgLoading by remember { mutableStateOf(false) }
    var epgError by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var reloadAttemptCount by rememberSaveable { mutableIntStateOf(0) }
    var waitingDots by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    remember { FocusRequester() }
    val categoryMap = mapOf(
        "All" to null,
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
//    var selectedCategoryIds by remember { mutableStateOf(savedCategoryIds) }
    var selectedCategoryIds by rememberSaveable { mutableStateOf(savedCategoryIds.toSet()) }

    val sortedCategories = remember(selectedCategoryIds) {
        val allCategoryName = "All"
        val allCategoryNames = categoryMap.keys.toList()
        val otherCategoryNames = allCategoryNames.filter { it != allCategoryName }
        val (selectedOtherCategories, unselectedOtherCategories) = otherCategoryNames.partition { categoryName ->
            val categoryId = categoryMap[categoryName]
            categoryId != null && selectedCategoryIds.contains(categoryId)
        }
        listOf(allCategoryName) + selectedOtherCategories + unselectedOtherCategories
    }


    // Helper functions for enhanced autoplay
    suspend fun fetchFromBackend(): List<Channel> {
        return try {
            ChannelUtils.fetchChannels("$basefinURL/channels")?.let { response ->
                channelsResponse.value = response
                context.getSharedPreferences("channel_cache", Context.MODE_PRIVATE).edit().apply {
                    putString("channels_json", Gson().toJson(response))
                    apply()
                }
                val categories = preferenceManager.myPrefs.filterCI
                    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                val languages = preferenceManager.myPrefs.filterLI
                    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

                val filtered = when {
                    categories.isNullOrEmpty() && languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                        response
                    )

                    categories.isNullOrEmpty() -> ChannelUtils.filterChannels(
                        response,
                        languageIds = languages?.mapNotNull { it.toIntOrNull() }
                            ?.takeIf { it.isNotEmpty() }
                    )

                    languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                        response,
                        categoryIds = categories.mapNotNull { it.toIntOrNull() }
                            .takeIf { it.isNotEmpty() }
                    )

                    else -> ChannelUtils.filterChannels(
                        response,
                        categoryIds = categories.mapNotNull { it.toIntOrNull() }
                            .takeIf { it.isNotEmpty() },
                        languageIds = languages.mapNotNull { it.toIntOrNull() }
                            .takeIf { it.isNotEmpty() }
                    )
                }
                filteredChannels.value = filtered
                filtered
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun watchdogAutoplay(channels: List<Channel>) {
        repeat(5) {
            if (preferenceManager.myPrefs.currChannelUrl.isNullOrEmpty()) {
                val channelsToUse = channels.ifEmpty { fetchFromBackend() }
                if (channelsToUse.isNotEmpty()) {
                    try {
                        val firstChannel = channelsToUse.first()
                        val intent = Intent(context, ExoPlayJet::class.java).apply {
                            putExtra("zone", "TV")
                            putParcelableArrayListExtra(
                                "channel_list_data",
                                ArrayList(channelsToUse.map { ch ->
                                    ChannelInfo(
                                        withQuality(context, ch.channel_url),
                                        "http://localhost:$localPORT/jtvimage/${ch.logoUrl}",
                                        ch.channel_name
                                    )
                                })
                            )
                            putExtra("current_channel_index", 0)
                            putExtra("video_url", firstChannel.channel_url)
                            putExtra(
                                "logo_url",
                                "http://localhost:$localPORT/jtvimage/${firstChannel.logoUrl}"
                            )
                            putExtra("ch_name", firstChannel.channel_name)
                        }

                        if (context !is Activity) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)

                        AppStartTracker.shouldPlayChannel = true
                        return
                    } catch (_: Exception) {
                    }
                }
            }
            kotlinx.coroutines.delay(2000)
        }
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
            } catch (_: Exception) {
                sharedPref.edit {
                    remove("channels_json")
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
                categories.isNullOrEmpty() && languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                    cachedChannels
                )

                categories.isNullOrEmpty() -> ChannelUtils.filterChannels(
                    cachedChannels,
                    languageIds = languages?.mapNotNull { it.toIntOrNull() }
                        ?.takeIf { it.isNotEmpty() }
                )

                languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                    cachedChannels,
                    categoryIds = categories.mapNotNull { it.toIntOrNull() }
                        .takeIf { it.isNotEmpty() }
                )

                else -> ChannelUtils.filterChannels(
                    cachedChannels,
                    categoryIds = categories.mapNotNull { it.toIntOrNull() }
                        .takeIf { it.isNotEmpty() },
                    languageIds = languages.mapNotNull { it.toIntOrNull() }
                        .takeIf { it.isNotEmpty() }
                )
            }
            filteredChannels.value = filtered
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
                        sharedPref.edit {
                            putString("channels_json", responseJsonString)
                        }
                        val categories = preferenceManager.myPrefs.filterCI
                            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                        val languages = preferenceManager.myPrefs.filterLI
                            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

                        val filtered = when {
                            categories.isNullOrEmpty() && languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                                response
                            )

                            categories.isNullOrEmpty() -> ChannelUtils.filterChannels(
                                response,
                                languageIds = languages?.mapNotNull { it.toIntOrNull() }
                                    ?.takeIf { it.isNotEmpty() }
                            )

                            languages.isNullOrEmpty() -> ChannelUtils.filterChannels(
                                response,
                                categoryIds = categories.mapNotNull { it.toIntOrNull() }
                                    .takeIf { it.isNotEmpty() }
                            )

                            else -> ChannelUtils.filterChannels(
                                response,
                                categoryIds = categories.mapNotNull { it.toIntOrNull() }
                                    .takeIf { it.isNotEmpty() },
                                languageIds = languages.mapNotNull { it.toIntOrNull() }
                                    .takeIf { it.isNotEmpty() }
                            )
                        }
                        filteredChannels.value = filtered
                        success = true
                    }
                } catch (_: Exception) {
                    // ignore, retry
                }
                if (!success) {
                    kotlinx.coroutines.delay(300)
                }
            }
            fetched = true
        }

        if (preferenceManager.myPrefs.startTvAutomatically) {
            var channelsForAutoplay = filteredChannels.value
            if (channelsForAutoplay.isEmpty()) {
                channelsForAutoplay = fetchFromBackend()
            }

            if (!AppStartTracker.shouldPlayChannel && channelsForAutoplay.isNotEmpty()) {
                val firstChannel = channelsForAutoplay.first()
                val intent = Intent(context, ExoPlayJet::class.java).apply {
                    putExtra("zone", "TV")
                    putParcelableArrayListExtra(
                        "channel_list_data",
                        ArrayList(
                            channelsForAutoplay.map { ch ->
                                ChannelInfo(
                                    withQuality(context, ch.channel_url),
                                    "http://localhost:$localPORT/jtvimage/${ch.logoUrl}",
                                    ch.channel_name
                                )
                            }
                        )
                    )
                    putExtra("current_channel_index", 0)
                    putExtra("video_url", firstChannel.channel_url)
                    putExtra(
                        "logo_url",
                        "http://localhost:$localPORT/jtvimage/${firstChannel.logoUrl}"
                    )
                    putExtra("ch_name", firstChannel.channel_name)
                }

                kotlinx.coroutines.delay(1000)

                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)


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
            } else {
                // Watchdog for reliability
                watchdogAutoplay(channelsForAutoplay)
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
            filteredChannels.value = filtered
        }
    }

    LaunchedEffect(fetched, filteredChannels.value, reloadAttemptCount) {
        if (fetched && filteredChannels.value.isEmpty() && reloadAttemptCount < 2) {
            waitingDots = ""
            for (i in 1..6) {
                waitingDots += "."
                delay(300)
            }
            reloadAttemptCount++
            showLoading = true
            val fetchedChannels = fetchFromBackend()
            filteredChannels.value = fetchedChannels
            showLoading = false
        }
    }

    // Fetch EPG data for selected channel
    LaunchedEffect(selectedChannel) {
        if (selectedChannel != null) {
            isEpgLoading = true
            epgError = false
            val epgURL = "$basefinURL/epg/${selectedChannel!!.channel_id}/0"
            Log.d("EPG_FETCH", epgURL)

            try {
                val fetchedEpg = ChannelUtils.fetchEpg(epgURL)
                if (fetchedEpg != null) {
                    epgData = fetchedEpg
                } else {
                    epgData = null
                    epgError = true
                }
            } catch (_: Exception) {
                epgData = null
                epgError = true
            } finally {
                isEpgLoading = false
            }
        } else {
            epgData = null
            epgError = false
            isEpgLoading = false
        }
    }

    LaunchedEffect(fetched) {
        if (!fetched) {
            delay(100)
            if (!fetched) showLoading = true
        } else {
            showLoading = false
        }
    }

    // UI: Loading state
    if (!fetched && showLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            ContainedLoadingIndicator(modifier = Modifier.size(100.dp))
        }
    }
    // UI: Empty state
    else if (fetched && filteredChannels.value.isEmpty()) {
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
                Text(text = waitingDots, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))
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
                        reloadAttemptCount++
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
                        if (categoryName == "All") {
                            selectedCategoryIds = emptySet()
                        } else if (categoryId != null) {
                            selectedCategoryIds = if (isSelected) {
                                selectedCategoryIds - categoryId
                            } else {
                                selectedCategoryIds + categoryId
                            }
                        }
                        val updatedCI = selectedCategoryIds.joinToString(",")
                        preferenceManager.myPrefs.filterCI = updatedCI
                        preferenceManager.savePreferences()
                    },
                    label = { Text(categoryName) },
                    selected = if (categoryName == "All") {
                        selectedCategoryIds.isEmpty()
                    } else {
                        isSelected
                    },
                    leadingIcon = when {
                        categoryName == "All" && selectedCategoryIds.isEmpty() -> {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "All selected icon",
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
                    }
                )
            }
        }

        // EPG CARD (null-safe)
        if (!preferenceManager.myPrefs.cardUiExperiment) {
            if (isEpgLoading || epgData != null || epgError) {
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
                        when {
                            isEpgLoading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Loading EPG...",
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    )
                                }
                            }

                            epgError -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No EPG available",
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    )
                                }
                            }

                            epgData != null -> {
                                val epg = epgData!!
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
                                            .heightIn(max = 110.dp)
                                    ) {
                                        Text(
                                            text = epg.channel_name,
                                            style = TextStyle(fontSize = 14.sp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = epg.showname,
                                            maxLines = 1,
                                            style = TextStyle(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = epg.description,
                                            style = TextStyle(fontSize = 13.sp),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    GlideImage(
                                        model = "$basefinURL/jtvposter/${epg.episodePoster}",
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
            }
        }

        val nonNullChannels = filteredChannels.value ?: emptyList()
        if (layoutMode.equals("CardUI(TV)", ignoreCase = true)) {
            ChannelGridScreen(
                context = context,
                filteredChannels = nonNullChannels,
                categoryMap = categoryMap,
                selectedChannelSetter = { selectedChannel = it },
                localPORT = localPORT,
                preferenceManager = preferenceManager,
                epgData = epgData
            )
        } else {
        ChannelGridMain(
            context = context,
            filteredChannels = nonNullChannels,
            // filteredChannels = filteredChannels.value,
            selectedChannelSetter = { selectedChannel = it },
            localPORT = localPORT,
            preferenceManager = preferenceManager
        )
        }


    }
}

