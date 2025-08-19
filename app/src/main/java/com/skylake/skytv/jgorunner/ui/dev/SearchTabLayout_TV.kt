package com.skylake.skytv.jgorunner.ui.dev

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClassicCard
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.activities.ExoplayerActivity
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import kotlinx.coroutines.delay

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchTabLayoutTV(context: Context, focusRequester: FocusRequester) {
    val scope = rememberCoroutineScope()
    val scope2 = rememberCoroutineScope()
    val channelsResponse = remember { mutableStateOf<ChannelResponse?>(null) }
    val allChannels = remember { mutableStateOf<List<Channel>>(emptyList()) }
    val filteredChannels = remember { mutableStateOf<List<Channel>>(emptyList()) }
    val preferenceManager = SkySharedPref.getInstance(context)
    val localPORT by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort)
    }
    val basefinURL = "http://localhost:$localPORT"
    var fetched by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val searchBarFocusRequester = remember { FocusRequester() }
    val tabFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }


    LaunchedEffect(Unit) {
        if (!fetched) {
            scope.launch {
                val response = ChannelUtils.fetchChannels("$basefinURL/channels")
                channelsResponse.value = response

                if (response != null) {

                    val initialFiltered = ChannelUtils.filterChannels(response)

                    allChannels.value = initialFiltered
                    filteredChannels.value = initialFiltered
                }

                fetched = true

                scope2.launch {
                    delay(10)
                    searchBarFocusRequester.requestFocus()
                }

            }
        }
    }

    fun updateFilteredChannels(text: String) {
        searchText = text
        filteredChannels.value = allChannels.value.filter { channel ->
            channel.channel_name.contains(text, ignoreCase = true)
        }
    }

    if (!fetched) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp)
            )
        }
    } else {

    Column {
        SearchBar(
            searchText = searchText,
            onSearchTextChanged = { text ->
                updateFilteredChannels(text)
            },
            onClearClick = {
                updateFilteredChannels("")
            },
            focusRequester = searchBarFocusRequester,
            onDownKey = {
                listFocusRequester.requestFocus()
            },
            onUPKey = {
                focusRequester.requestFocus()
            }
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.focusRequester(listFocusRequester)
        ) {
            items(filteredChannels.value) { channel ->
                ClassicCard(
                    modifier = Modifier.height(120.dp),
                    image = {
                        GlideImage(
                            model = "http://localhost:${localPORT}/jtvimage/${channel.logoUrl}",
                            contentDescription = channel.channel_name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    },
                    title = {
                        Text(
                            text = channel.channel_name,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    },
                    onClick = {
                        Log.d("HT", channel.channel_name)
                        // Mark player active immediately so global loop doesn't override user choice
                        val pref = SkySharedPref.getInstance(context)
                        pref.myPrefs.startTvAutomatically = true
                        pref.myPrefs.tvPlayerActive = true
                        // persist choice for next loop
                        pref.myPrefs.currChannelUrl = channel.channel_url
                        pref.myPrefs.currChannelLogo = "http://localhost:${localPORT}/jtvimage/${channel.logoUrl}"
                        pref.myPrefs.currChannelName = channel.channel_name
                        pref.myPrefs.autoPlaySuppressUntil = System.currentTimeMillis() + 15000
                        pref.savePreferences()
                        val intent = Intent(context, ExoPlayJet::class.java).apply {
                            putExtra("video_url", channel.channel_url)
                            putExtra("zone", "TV")
                        }
                        startActivity(context, intent, null)

                        val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                        val type = object : TypeToken<List<Channel>>() {}.type
                        val recentChannels: MutableList<Channel> = Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()

                        val existingIndex = recentChannels.indexOfFirst { it.channel_id == channel.channel_id }

                        if (existingIndex != -1) {
                            // Channel exists, move it to the top
                            val existingChannel = recentChannels[existingIndex]
                            recentChannels.removeAt(existingIndex)
                            recentChannels.add(0, existingChannel)
                        } else {
                            // Channel doesn't exist, add it to the top
                            recentChannels.add(0, channel)
                            if (recentChannels.size > 25) {
                                recentChannels.removeAt(recentChannels.size - 1)
                            }
                        }

                        val gson = Gson()
                        val recentChannelsJsonx = gson.toJson(recentChannels)
                        preferenceManager.myPrefs.recentChannels = recentChannelsJsonx
                        preferenceManager.savePreferences()
                    },
                    colors = CardDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            }
        }
    }
    }
}

