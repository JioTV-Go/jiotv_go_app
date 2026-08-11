package com.skylake.skytv.jgorunner.ui.tvhome


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import androidx.compose.material3.Text as CText

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun OmniTvRecent_Layout(context: Context) {
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
//        CText(
//            text = "Recent Channels",
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Bold
//        )

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

                    var isFocused by remember { mutableStateOf(false) }

                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .height(120.dp)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            }
                            .border(
                                width = 4.dp,
                                color = if (isFocused) Color(0xFFFFD700) else Color.Transparent,
                                shape = CardDefaults.shape
                            )
                            .clickable {
                                Log.d("HT", channel.channel_name)
                                val serverPort = SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort

                                val intent = Intent(context, ExoPlayJet::class.java).apply {
                                    putExtra("zone", "TV")
                                    if (channel.channel_id.all { it.isDigit() }) putExtra("channel_list_kind", "jio")
                                    putExtra("current_channel_index", -1)
                                    putExtra("video_url", channel.channel_url)
                                    putExtra("logo_url", if (channel.logoUrl.startsWith("http")) channel.logoUrl else "http://localhost:$serverPort/jtvimage/${channel.logoUrl}")
                                    putExtra("ch_name", channel.channel_name)
                                }

                                if (context !is Activity) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)


                                val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                                val type = object : TypeToken<List<Channel>>() {}.type
                                val recentChannels: MutableList<Channel> =
                                    Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()

                                val existingIndex =
                                    recentChannels.indexOfFirst { it.channel_id == channel.channel_id }

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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                    ) {
                        Column {

                            val imageUrl = if (channel.logoUrl.contains("http")) {
                                channel.logoUrl
                            } else {
                                "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}"
                            }

                            GlideImage(
                                model = imageUrl,
                                contentDescription = channel.channel_name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentScale = ContentScale.Fit
                            )

                            Text(
                                text = channel.channel_name,
                                color = MaterialTheme.colorScheme.onSurface,
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
