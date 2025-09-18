package com.skylake.skytv.jgorunner.ui.tvhome.depreciated

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.skylake.skytv.jgorunner.data.SkySharedPref
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.tvhome.Channel
import com.skylake.skytv.jgorunner.ui.tvhome.ChannelResponse
import com.skylake.skytv.jgorunner.ui.tvhome.ChannelUtils
import com.skylake.skytv.jgorunner.ui.tvhome.EpgProgram
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Main_Layout_old(context: Context?) {
    if (context == null) return
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

    val categoryIdMap = categoryMap.filterValues { it != null }.map { it.value!! to it.key }.toMap()
    val savedCategoryIds = preferenceManager.myPrefs.filterCI
        ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
    var selectedCategoryIds by remember { mutableStateOf(savedCategoryIds) }
    val selectedCategories = categoryMap.filterValues { it in selectedCategoryIds }.keys

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

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("channel_cache", Context.MODE_PRIVATE)
        var useCache = true
        var cachedChannels: ChannelResponse? = null

        val cachedJson = sharedPref.getString("channels_json", null)
        if (cachedJson != null) {
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
                categories.isNullOrEmpty() && languages.isNullOrEmpty() -> {
                    ChannelUtils.filterChannels(cachedChannels)
                }

                categories.isNullOrEmpty() -> {
                    ChannelUtils.filterChannels(
                        cachedChannels,
                        languageIds = languages?.mapNotNull { it.toIntOrNull() }
                            ?.takeIf { it.isNotEmpty() }
                    )
                }

                languages.isNullOrEmpty() -> {
                    ChannelUtils.filterChannels(
                        cachedChannels,
                        categoryIds = categories.mapNotNull { it.toIntOrNull() }
                            .takeIf { it.isNotEmpty() }
                    )
                }

                else -> {
                    ChannelUtils.filterChannels(
                        cachedChannels,
                        categoryIds = categories.mapNotNull { it.toIntOrNull() }
                            .takeIf { it.isNotEmpty() },
                        languageIds = languages.mapNotNull { it.toIntOrNull() }
                            .takeIf { it.isNotEmpty() }
                    )
                }
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
                            categories.isNullOrEmpty() && languages.isNullOrEmpty() -> {
                                ChannelUtils.filterChannels(response)
                            }

                            categories.isNullOrEmpty() -> {
                                ChannelUtils.filterChannels(
                                    response,
                                    languageIds = languages?.mapNotNull { it.toIntOrNull() }
                                        ?.takeIf { it.isNotEmpty() }
                                )
                            }

                            languages.isNullOrEmpty() -> {
                                ChannelUtils.filterChannels(
                                    response,
                                    categoryIds = categories.mapNotNull { it.toIntOrNull() }
                                        ?.takeIf { it.isNotEmpty() }
                                )
                            }

                            else -> {
                                ChannelUtils.filterChannels(
                                    response,
                                    categoryIds = categories.mapNotNull { it.toIntOrNull() }
                                        ?.takeIf { it.isNotEmpty() },
                                    languageIds = languages.mapNotNull { it.toIntOrNull() }
                                        ?.takeIf { it.isNotEmpty() }
                                )
                            }
                        }
                        filteredChannels.value = filtered ?: emptyList()
                        success = true
                    }
                } catch (_: Exception) {
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

    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            val epgURLc = "$basefinURL/epg/${channel.channel_id}/0"
            Log.d("NANOdix",epgURLc)
            epgData = ChannelUtils.fetchEpg(epgURLc)
            Log.d("NANOdix", "Now playing: ${epgData?.showname}")
        } ?: run {
            epgData = null
        }
    }

    if (!fetched) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularWavyProgressIndicator(modifier = Modifier.size(60.dp))
        }
    } else if (filteredChannels.value.isNullOrEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No channels found",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Try the following steps:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\n• Check your internet connection\n• Reload the app\n• Go to Main Screen for detailed information",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
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
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reload")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reload App")
                    }
                }
            }
        }
    } else {
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
                    label = { Text(categoryName) },
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

        // EPG CARD
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = epg.showname ?: "No Program Info",
                                maxLines = 1,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = epg.description ?: "No description available",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
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

        // CHANNEL GRID
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredChannels.value ?: emptyList()) { channel ->
                var isFocused by remember { mutableStateOf(false) }
                ElevatedCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .height(120.dp)
                        .focusRequester(focusRequester)
                        .onFocusEvent { focusState ->
                            isFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                selectedChannel = channel
                            }
                        }
                        .clickable {
                            val intent = Intent(context, ExoPlayJet::class.java).apply {
                                putExtra("zone", "TV")
                                putParcelableArrayListExtra("channel_list_data", ArrayList(
                                    (filteredChannels.value ?: emptyList()).map { ch ->
                                        ChannelInfo(
                                            ch.channel_url ?: "",
                                            "http://localhost:$localPORT/jtvimage/${ch.logoUrl ?: ""}",
                                            ch.channel_name ?: ""
                                        )
                                    }
                                ))
                                putExtra("current_channel_index", (filteredChannels.value ?: emptyList()).indexOf(channel))
                                putExtra("video_url", channel.channel_url ?: "")
                                putExtra("logo_url", "http://localhost:$localPORT/jtvimage/${channel.logoUrl ?: ""}")
                                putExtra("ch_name", channel.channel_name ?: "")
                            }
                            startActivity(context, intent, null)

                            // Update recent channels
                            val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                            val type = object : TypeToken<List<Channel>>() {}.type
                            val recentChannels: MutableList<Channel> =
                                Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()

                            val existingIndex = recentChannels.indexOfFirst {
                                it.channel_id == channel.channel_id
                            }

                            if (existingIndex != -1) {
                                val existingChannel = recentChannels[existingIndex]
                                recentChannels.removeAt(existingIndex)
                                recentChannels.add(0, existingChannel)
                            } else {
                                recentChannels.add(0, channel)
                                if (recentChannels.size > 25) {
                                    recentChannels.removeAt(recentChannels.size - 1)
                                }
                            }

                            preferenceManager.myPrefs.recentChannels = Gson().toJson(recentChannels)
                            preferenceManager.savePreferences()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column {
                        // Channel logo
                        val imageUrl = "http://localhost:$localPORT/jtvimage/${channel.logoUrl ?: ""}"

                        GlideImage(
                            model = imageUrl,
                            contentDescription = channel.channel_name ?: "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentScale = ContentScale.Fit
                        )

                        // Channel name
                        Text(
                            text = channel.channel_name ?: "Unknown",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
