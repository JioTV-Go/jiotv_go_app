package com.skylake.skytv.jgorunner.ui.tvhome

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Recent_Layout(
    context: Context,
    viewModel: TvViewModel,
    basefinURL: String
) {
    val recentChannels by viewModel.recentChannels.collectAsState()
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (recentChannels.isEmpty()) {
            Text(
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
                items(recentChannels, key = { it.channel_id }) { channel ->
                    var isFocused by remember { mutableStateOf(false) }

                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .height(120.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                            .clickable {
                                
                                val intent = Intent(context, ExoPlayJet::class.java).apply {
                                    putExtra("video_url", "$basefinURL/live/${channel.channel_id}")
                                    putExtra("zone", "TV")

                                    val allChannelsData = ArrayList(
                                        recentChannels.map { ch ->
                                            val fullLogoUrl = if (ch.logoUrl.contains("http")) ch.logoUrl else "$basefinURL/jtvimage/${ch.logoUrl}"
                                            ChannelInfo(ch.channel_url, fullLogoUrl, ch.channel_name)
                                        }
                                    )
                                    putParcelableArrayListExtra("channel_list_data", allChannelsData)
                                    putExtra("current_channel_index", recentChannels.indexOf(channel))
                                    putExtra("logo_url", if (channel.logoUrl.contains("http")) channel.logoUrl else "$basefinURL/jtvimage/${channel.logoUrl}")
                                    putExtra("ch_name", channel.channel_name)
                                }
                                if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)

                                
                                viewModel.saveToRecents(channel)
                            },
                        border = if (isFocused) BorderStroke(4.dp, Color(0xFFFFD700)) else null,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            val imageUrl = if (channel.logoUrl.contains("http")) channel.logoUrl else "$basefinURL/jtvimage/${channel.logoUrl}"
                            GlideImage(
                                model = imageUrl,
                                contentDescription = channel.channel_name,
                                modifier = Modifier.fillMaxWidth().height(80.dp),
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