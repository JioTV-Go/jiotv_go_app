package com.skylake.skytv.jgorunner.ui.tvhome.depreciated

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.tv.material3.Text
import androidx.compose.material3.Text as CText
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.data.SkySharedPref
import androidx.tv.material3.ClassicCard
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.tvhome.Channel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Recent_LayoutTV(context: Context) {
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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentChannels.value) { channel ->
                    ClassicCard(
                        modifier = Modifier.height(120.dp),
                        image = {
                            GlideImage(
                                model = "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}",
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
                        }
                    )
                }
            }
        }
    }
}
