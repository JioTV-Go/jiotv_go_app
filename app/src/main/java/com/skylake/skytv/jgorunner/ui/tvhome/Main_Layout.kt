package com.skylake.skytv.jgorunner.ui.tvhome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.screens.AppStartTracker
import com.skylake.skytv.jgorunner.ui.tvhome.components.ShimmerChannelGrid
import com.skylake.skytv.jgorunner.utils.withQuality
import kotlinx.coroutines.delay

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun Main_Layout(context: Context, reloadTrigger: Int) {
    val tvViewModel: TvViewModel = viewModel()

    val filteredChannels by tvViewModel.filteredChannels.collectAsState()
    val isLoading by tvViewModel.isLoading.collectAsState()
    val isError by tvViewModel.isError.collectAsState()
    val favoriteChannels by tvViewModel.favoriteChannels.collectAsState()

    val favoriteIds = remember(favoriteChannels) { favoriteChannels.map { it.channel_id }.toSet() }
    val basefinURL = tvViewModel.basefinURL

    LaunchedEffect(Unit) {
        if (filteredChannels.isEmpty() && !isLoading) {
            tvViewModel.loadChannels(forceRefresh = false)
        }
    }

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            tvViewModel.loadChannels(forceRefresh = true)
        }
    }

    LaunchedEffect(filteredChannels) {
        if (filteredChannels.isNotEmpty() && tvViewModel.preferenceManager.myPrefs.startTvAutomatically && !AppStartTracker.shouldPlayChannel) {
            val firstChannel = filteredChannels.first()
            val intent = Intent(context, ExoPlayJet::class.java).apply {
                putExtra("zone", "TV")
                putParcelableArrayListExtra(
                    "channel_list_data",
                    ArrayList(filteredChannels.map { ch ->
                        ChannelInfo(
                            withQuality(context, ch.channel_url),
                            ch.key_url,
                            "$basefinURL/jtvimage/${ch.logoUrl}",
                            ch.channel_name
                        )
                    })
                )
                putExtra("current_channel_index", 0)
                putExtra("video_url", firstChannel.channel_url)
                putExtra("key_url", firstChannel.key_url)
                putExtra("logo_url", "$basefinURL/jtvimage/${firstChannel.logoUrl}")
                putExtra("ch_name", firstChannel.channel_name)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            delay(500) 
            context.startActivity(intent)
            tvViewModel.saveToRecents(firstChannel)
            AppStartTracker.shouldPlayChannel = true
        }
    }

    
    val languageMap = remember {
        mapOf(
            "All Languages" to null, "Hindi" to 1, "Marathi" to 2, "Punjabi" to 3,
            "Urdu" to 4, "Bengali" to 5, "English" to 6, "Malayalam" to 7,
            "Tamil" to 8, "Gujarati" to 9, "Odia" to 10, "Telugu" to 11,
            "Bhojpuri" to 12, "Kannada" to 13, "Assamese" to 14, "Nepali" to 15,
            "French" to 16, "Other" to 18
        )
    }

    
    val categoryMap = remember {
        mapOf(
            "All" to null, "Entertainment" to 5, "Movies" to 6, "Kids" to 7,
            "Sports" to 8, "Lifestyle" to 9, "Infotainment" to 10, "News" to 12,
            "Music" to 13, "Devotional" to 15, "Business" to 16,
            "Educational" to 17, "Shopping" to 18, "JioDarshan" to 19
        )
    }

    val savedCategoryIds = remember {
        tvViewModel.preferenceManager.myPrefs.filterCI
            ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    var selectedCategoryIds by rememberSaveable { mutableStateOf(savedCategoryIds) }
    var selectedLanguageIds by rememberSaveable { mutableStateOf(emptySet<Int>()) }

    val sortedCategories = remember(selectedCategoryIds) {
        val allCategoryNames = categoryMap.keys.toList()
        val otherCategoryNames = allCategoryNames.filter { it != "All" }
        val (selected, unselected) = otherCategoryNames.partition { categoryName ->
            val id = categoryMap[categoryName]
            id != null && selectedCategoryIds.contains(id)
        }
        listOf("All") + selected + unselected
    }

    
    val epgData by tvViewModel.epgData.collectAsState()
    val isEpgLoading by tvViewModel.isEpgLoading.collectAsState()
    val epgError by tvViewModel.epgError.collectAsState()
    val GoldColor = Color(0xFFFFD700)

    
    when {
        isLoading && filteredChannels.isEmpty() -> {
            ShimmerChannelGrid()
        }
        isError && filteredChannels.isEmpty() -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Unable to Load Channels",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Server not responding or no data received.\n\nTroubleshooting steps:\n• Check server status\n• Verify your DNS and internet settings\n• Click retry below to attempt reconnection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.widthIn(max = 360.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ElevatedCard(
                        onClick = { tvViewModel.loadChannels(forceRefresh = true) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry connection"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Retry Connection",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    
                    item {
                        Box {
                            var isFocused by remember { mutableStateOf(false) }
                            var showLanguageDropdown by remember { mutableStateOf(false) }
                            val isChipSelected = selectedLanguageIds.isNotEmpty()

                            FilterChip(
                                modifier = Modifier.onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                },
                                onClick = { showLanguageDropdown = true },

                                label = {
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = "Languages",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                selected = isChipSelected,
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isChipSelected,
                                    borderColor = if (isFocused) GoldColor else MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = if (isFocused) GoldColor else MaterialTheme.colorScheme.primary,
                                    borderWidth = if (isFocused) 2.dp else 1.dp,
                                    selectedBorderWidth = if (isFocused) 2.dp else 1.dp
                                )
                            )

                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false }
                            ) {
                                languageMap.forEach { (langName, langId) ->
                                    val isSelected = if (langId == null) selectedLanguageIds.isEmpty() else selectedLanguageIds.contains(langId)
                                    DropdownMenuItem(
                                        text = { Text(langName) },
                                        onClick = {
                                            selectedLanguageIds = if (langId == null) {
                                                emptySet()
                                            } else {
                                                if (isSelected) selectedLanguageIds - langId else selectedLanguageIds + langId
                                            }
                                            
                                            tvViewModel.applyFilters(
                                                newCategoryIds = selectedCategoryIds,
                                                newLanguageIds = selectedLanguageIds
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Filled.Done, contentDescription = "Selected")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    
                    items(sortedCategories) { categoryName ->
                        val categoryId = categoryMap[categoryName]
                        val isSelected = categoryId != null && selectedCategoryIds.contains(categoryId)
                        val isChipSelected = if (categoryName == "All") selectedCategoryIds.isEmpty() else isSelected

                        var isFocused by remember { mutableStateOf(false) }

                        FilterChip(
                            modifier = Modifier.onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            },
                            onClick = {
                                selectedCategoryIds = if (categoryName == "All") {
                                    emptySet()
                                } else if (categoryId != null) {
                                    if (isSelected) selectedCategoryIds - categoryId else selectedCategoryIds + categoryId
                                } else selectedCategoryIds

                                tvViewModel.preferenceManager.myPrefs.filterCI = selectedCategoryIds.joinToString(",")
                                tvViewModel.preferenceManager.savePreferences()

                                
                                tvViewModel.applyFilters(
                                    newCategoryIds = selectedCategoryIds,
                                    newLanguageIds = selectedLanguageIds
                                )
                            },
                            label = { Text(categoryName) },
                            selected = isChipSelected,
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isChipSelected,
                                borderColor = if (isFocused) GoldColor else MaterialTheme.colorScheme.outline,
                                selectedBorderColor = if (isFocused) GoldColor else MaterialTheme.colorScheme.primary,
                                borderWidth = if (isFocused) 2.dp else 1.dp,
                                selectedBorderWidth = if (isFocused) 2.dp else 1.dp
                            ),
                            leadingIcon = if (isChipSelected) {
                                { Icon(Icons.Filled.Done, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }

                
                if (tvViewModel.preferenceManager.myPrefs.showEPG && (isEpgLoading || epgData != null || epgError)) {
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
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Loading EPG...", color = Color.Gray)
                                    }
                                }

                                epgError -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No EPG available", color = Color.Gray)
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

                
                ChannelGridMain(
                    filteredChannels = filteredChannels,
                    favoriteIds = favoriteIds,
                    basefinURL = basefinURL,
                    onSelectedChannelChanged = { channel ->
                        if (tvViewModel.preferenceManager.myPrefs.showEPG) {
                            tvViewModel.loadEpg(channel.channel_id)
                        }
                    },
                    onChannelClick = { channel, index ->
                        val intent = Intent(context, ExoPlayJet::class.java).apply {
                            putExtra("video_url", channel.channel_url)
                            putExtra("key_url", channel.key_url)
                            putExtra("zone", "TV")
                            putParcelableArrayListExtra(
                                "channel_list_data",
                                ArrayList(filteredChannels.map { ch ->
                                    ChannelInfo(
                                        withQuality(context, ch.channel_url),
                                        ch.key_url,
                                        "$basefinURL/jtvimage/${ch.logoUrl}",
                                        ch.channel_name
                                    )
                                })
                            )
                            putExtra("current_channel_index", index)
                            putExtra("logo_url", "$basefinURL/jtvimage/${channel.logoUrl}")
                            putExtra("ch_name", channel.channel_name)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        tvViewModel.saveToRecents(channel)
                    },
                    onChannelLongClick = { channel ->
                        tvViewModel.toggleFavorite(channel)
                        Toast.makeText(context, "${channel.channel_name} favorites updated", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}