package com.skylake.skytv.jgorunner.ui.tvhome.depreciated

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.ContentScale

import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.data.SkySharedPref
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusEvent
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.ui.tvhome.Channel
import com.skylake.skytv.jgorunner.ui.tvhome.ChannelResponse
import com.skylake.skytv.jgorunner.ui.tvhome.ChannelUtils

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchTabLayout(context: Context, focusRequester: FocusRequester) {
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
            CircularWavyProgressIndicator(
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
                            .clickable {
                                Log.d("HT", channel.channel_name)
                                val intent = Intent(context, ExoPlayJet::class.java).apply {
                                    putExtra("video_url", channel.channel_url)
                                }
                                startActivity(context, intent, null)

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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    ) {
                        Column {
                            // Image
                            GlideImage(
                                model = "http://localhost:${localPORT}/jtvimage/${channel.logoUrl}",
                                contentDescription = channel.channel_name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentScale = ContentScale.Fit
                            )

                            // Title
                            Text(
                                text = channel.channel_name,
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

@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onClearClick: () -> Unit,
    focusRequester: FocusRequester,
    onDownKey: () -> Unit,
    onUPKey: () -> Unit
) {
    val focusManager = LocalFocusManager.current
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
