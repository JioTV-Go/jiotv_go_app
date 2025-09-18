package com.skylake.skytv.jgorunner.ui.tvhome

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
//import androidx.tv.material3.Text
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text as CText
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Recent_Layout(context: Context) {
    val preferenceManager = SkySharedPref.getInstance(context)
    val recentChannelsJson = preferenceManager.myPrefs.recentChannels

    val type = object : TypeToken<List<Channel>>() {}.type

    val recentChannels = remember {
        mutableStateOf<List<Channel>>(Gson().fromJson(recentChannelsJson, type) ?: emptyList())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CText(
            text = "Recent Channels",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (recentChannels.value.isEmpty()) {
            CText(
                text = "No recent channels",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            val focusRequester = remember { FocusRequester() }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(recentChannels.value) { channel ->

                    var isFocused by remember { mutableStateOf(false) }

                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .height(120.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            }
                            .clickable {
                                Log.d("HT", channel.channel_name)
                                val intent = Intent(context, ExoPlayJet::class.java).apply {
                                    putExtra("video_url", "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/live/${channel.channel_id}" )
                                    putExtra("zone", "TV")
                                    // Prepare channel list for ExoplayerActivityPass
                                    val allChannelsData = ArrayList(recentChannels.value.map { ch ->
                                        val fullLogoUrl = "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/jtvimage/${ch.logoUrl}"
                                        ChannelInfo(ch.channel_url, fullLogoUrl, ch.channel_name)
                                    })
                                    putParcelableArrayListExtra("channel_list_data", allChannelsData)

                                    val currentChannelIndex = recentChannels.value.indexOf(channel)
                                    putExtra("current_channel_index", currentChannelIndex)

                                    putExtra("video_url", channel.channel_url)
                                    putExtra("logo_url", "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}")
                                    putExtra("ch_name", channel.channel_name)
                                }
                                startActivity(context, intent, null)

                                val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                                val type = object : TypeToken<List<Channel>>() {}.type
                                val recentChannels: MutableList<Channel> = Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()

                                val existingIndex = recentChannels.indexOfFirst { it.channel_id == channel.channel_id }

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

                                val gson = Gson()
                                val recentChannelsJsonx = gson.toJson(recentChannels)
                                preferenceManager.myPrefs.recentChannels = recentChannelsJsonx
                                preferenceManager.savePreferences()
                            },
                        border = if (isFocused) BorderStroke(4.dp, Color(0xFFFFD700)) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                    ) {
                        Column {
                            // Image
                            val imageUrl = "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}"

                            GlideImage(
                                model = imageUrl,
                                contentDescription = channel.channel_name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentScale = ContentScale.Fit
                            )

                            // Title
                            Text(
                                text = channel.channel_name,
                                color=MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
