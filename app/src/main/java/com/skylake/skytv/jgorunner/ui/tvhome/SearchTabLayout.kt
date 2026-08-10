
package com.skylake.skytv.jgorunner.ui.tvhome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalGlideComposeApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SearchTabLayout(
    context: Context,
    focusRequester: FocusRequester,
    tvViewModel: TvViewModel
) {
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
    val listFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!fetched) {
            scope.launch {
                val response = ChannelUtils.fetchChannels("$basefinURL/channels")
                channelsResponse.value = response

                if (response != null) {
                    allChannels.value = ChannelUtils.filterChannels(response)
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
        filteredChannels.value = if (text.isBlank()) {
            emptyList()
        } else {
            allChannels.value.filter { channel ->
                channel.channel_name.contains(text, ignoreCase = true)
            }
        }
    }

    if (!fetched) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularWavyProgressIndicator(modifier = Modifier.size(60.dp))
        }
    } else {
        Column {
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { text -> updateFilteredChannels(text) },
                onClearClick = { updateFilteredChannels("") },
                focusRequester = searchBarFocusRequester,
                onDownKey = { listFocusRequester.requestFocus() },
                onUPKey = { focusRequester.requestFocus() }
            )

            if (searchText.isNotBlank()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.focusRequester(listFocusRequester)
                ) {
                    items(filteredChannels.value) { channel ->
                        var isFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (isFocused) 1.1f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ), label = ""
                        )

                        ElevatedCard(
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .height(120.dp)
                                .scale(scale)
                                .onFocusEvent { focusState ->
                                    isFocused = focusState.isFocused
                                }

                                .combinedClickable(
                                    onClick = {
                                        Log.d("HT", channel.channel_name)
                                        val intent = Intent(context, ExoPlayJet::class.java).apply {
                                            putExtra("video_url", channel.channel_url)
                                        }
                                        if (context !is Activity) {
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)


                                        val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                                        val type = object : TypeToken<List<Channel>>() {}.type
                                        val recentChannels: MutableList<Channel> =
                                            Gson().fromJson(recentChannelsJson, type)
                                                ?: mutableListOf()

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
                                    onLongClick = {

                                        tvViewModel.toggleFavorite(channel)
                                        Toast.makeText(context, "${channel.channel_name} favorites updated", Toast.LENGTH_SHORT).show()
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        ) {
                            Column {
                                GlideImage(
                                    model = "http://localhost:${localPORT}/jtvimage/${channel.logoUrl}",
                                    contentDescription = channel.channel_name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    text = channel.channel_name,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text("Type to search channels", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onClearClick: () -> Unit,
    focusRequester: FocusRequester,
    onDownKey: () -> Unit,
    onUPKey: () -> Unit
) {
    LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    TextField(
        value = searchText,
        onValueChange = {
            onSearchTextChanged(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.Back -> {
                            if (isFocused) {
                                onUPKey()
                                true
                            } else {
                                false
                            }
                        }

                        Key.DirectionDown -> {
                            if (isFocused) {
                                onDownKey()
                                true
                            } else {
                                false
                            }
                        }

                        Key.DirectionUp -> {
                            if (isFocused) {
                                onUPKey()
                                true
                            } else {
                                false
                            }
                        }

                        else -> false
                    }
                } else {
                    false
                }
            },
        placeholder = { Text("Search Channels") },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onClearClick() }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
