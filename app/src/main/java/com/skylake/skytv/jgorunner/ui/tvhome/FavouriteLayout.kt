package com.skylake.skytv.jgorunner.ui.tvhome

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
// AppStartTracker and autoplay removed for FavouriteLayout; retaining background fetch only

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FavouriteLayout(context: Context) {
    val preferenceManager = SkySharedPref.getInstance(context)
    val gson = remember { Gson() }
    // Only JioTV favourites retained
    val jiotvFavKey = "favouriteChannelsJio"
    val shared = context.getSharedPreferences("favourites_store", Context.MODE_PRIVATE)
    if (!shared.contains(jiotvFavKey)) {
        // migrate legacy one-time
        preferenceManager.myPrefs.favouriteChannels?.takeIf { !it.isNullOrBlank() }?.let { legacy ->
            shared.edit().putString(jiotvFavKey, legacy).apply()
        }
    }
    var favouriteChannels by remember(shared.getString(jiotvFavKey, "")) {
        val json = shared.getString(jiotvFavKey, "")
        mutableStateOf(safeChannelParse(json))
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showModifyDialog by remember { mutableStateOf(false) }

    // Canonical full channel list (cache -> network)
    var allChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loadingAllChannels by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Load canonical channels once (or when port changes)
    LaunchedEffect(preferenceManager.myPrefs.jtvGoServerPort) {
        loadingAllChannels = true
        loadError = null
        val port = preferenceManager.myPrefs.jtvGoServerPort ?: 8080
        try {
            allChannels = ChannelUtils.loadCanonicalChannels(context, port)
            if (allChannels.isEmpty()) loadError = "No channels available"
        } catch (e: Exception) {
            loadError = e.message
            Log.e("FavouriteLayout", "Channel load failed: ${e.message}")
        } finally {
            loadingAllChannels = false
        }
    }

    // No filtering: show favourites exactly as stored
    val visibleFavourites = favouriteChannels

    // Background refresh only
    LaunchedEffect(visibleFavourites) {
        if (visibleFavourites.isEmpty()) {
            try {
                val port = preferenceManager.myPrefs.jtvGoServerPort ?: 8080
                ChannelUtils.fetchChannels("http://localhost:$port/channels")?.let { response ->
                    context.getSharedPreferences("channel_cache", Context.MODE_PRIVATE).edit().apply {
                        putString("channels_json", Gson().toJson(response))
                        apply()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title line
        Text(
            text = "JioTV Favourite Channels",
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Buttons centered below title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                if (loadingAllChannels) {
                    Toast.makeText(context, "Loading channels...", Toast.LENGTH_SHORT).show()
                } else if (allChannels.isEmpty()) {
                    Toast.makeText(context, loadError ?: "No channels available", Toast.LENGTH_SHORT).show()
                } else {
                    showAddDialog = true
                }
            }) { Text(if (loadingAllChannels) "Loading" else "Add") }
            OutlinedButton(onClick = {
                if (favouriteChannels.isEmpty()) {
                    Toast.makeText(context, "No favourites", Toast.LENGTH_SHORT).show()
                } else {
                    showModifyDialog = true
                }
            }) { Text("Modify") }
            OutlinedButton(onClick = {
                favouriteChannels = emptyList()
                shared.edit().putString(jiotvFavKey, "").apply()
            }) { Text("Remove All") }
        }

        // Filters removed per user request

        if (visibleFavourites.isEmpty()) {
            Text("No favourite channels", fontSize = 14.sp)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(visibleFavourites, key = { it.channel_id }) { channel ->
                    var isFocused by remember { mutableStateOf(false) }
                    Card(
                        border = if (isFocused) BorderStroke(4.dp, Color(0xFFFFA500)) else null,
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .height(120.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .clickable {
                                playChannel(context, channel, visibleFavourites)
                            }
                    ) {
                        Column {
                            val logo = "http://localhost:${preferenceManager.myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}"
                            GlideImage(
                                model = logo,
                                contentDescription = channel.channel_name,
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentScale = ContentScale.Fit
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
                                Text(channel.channel_name, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFavouriteChannelsDialog(
            context = context,
            allChannels = allChannels,
            alreadySelected = favouriteChannels.map { it.channel_id }.toSet(),
            onDismiss = { showAddDialog = false },
            onSelectionChanged = { updated ->
                favouriteChannels = updated
                shared.edit().putString(jiotvFavKey, try { gson.toJson(updated) } catch (e:Exception){""}).apply()
            }
        )
    }
    if (showModifyDialog) {
        ModifyFavouriteChannelsDialog(
            context = context,
            favourites = favouriteChannels,
            onDismiss = { showModifyDialog = false },
            onSelectionChanged = { updated ->
                favouriteChannels = updated
                shared.edit().putString(jiotvFavKey, try { gson.toJson(updated) } catch (e:Exception){""}).apply()
            }
        )
    }
}

private fun safeChannelParse(json: String?): List<Channel> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val gson = Gson()
        val type = object : TypeToken<List<Channel>>() {}.type
        gson.fromJson<List<Channel>>(json, type) ?: emptyList()
    } catch (e1: JsonSyntaxException) {
        // Try wrapped key formats
        try {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root: Map<String, Any> = gson.fromJson(json, mapType) ?: return emptyList()
            val keys = listOf("data", "result", "channels")
            val rawList = keys.firstNotNullOfOrNull { k -> root[k] as? List<*> } ?: return emptyList()
            rawList.mapNotNull { elem ->
                try {
                    val objJson = gson.toJson(elem)
                    gson.fromJson(objJson, Channel::class.java)
                } catch (e: Exception) { null }
            }.filterNotNull()
        } catch (e2: Exception) {
            Log.e("FavouriteLayout", "Parse fallback failed: ${e2.message}")
            emptyList()
        }
    } catch (e: Exception) {
        Log.e("FavouriteLayout", "Parse error: ${e.message}")
        emptyList()
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ModifyFavouriteChannelsDialog(
    context: Context,
    favourites: List<Channel>,
    onDismiss: () -> Unit,
    onSelectionChanged: (List<Channel>) -> Unit
) {
    val preferenceManager = SkySharedPref.getInstance(context)
    val selected = remember(favourites) { mutableStateOf(favourites.map { it.channel_id }.toMutableSet()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Modify Favourite Channels", fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                if (favourites.isEmpty()) {
                    Text("No favourite", fontSize = 12.sp)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 420.dp)
                    ) {
                        items(favourites, key = { it.channel_id }) { channel ->
                            var isChecked by remember { mutableStateOf(selected.value.contains(channel.channel_id)) }
                            Card(
                                modifier = Modifier.width(140.dp).height(140.dp).clickable {
                                    isChecked = !isChecked
                                    if (isChecked) selected.value.add(channel.channel_id) else selected.value.remove(channel.channel_id)
                                    val finalList = favourites.filter { selected.value.contains(it.channel_id) }
                                    onSelectionChanged(finalList)
                                },
                                border = if (isChecked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val logo = "http://localhost:${preferenceManager.myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}"
                                    GlideImage(
                                        model = logo,
                                        contentDescription = channel.channel_name,
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isChecked, onCheckedChange = { checked ->
                                            isChecked = checked
                                            if (checked) selected.value.add(channel.channel_id) else selected.value.remove(channel.channel_id)
                                            val finalList = favourites.filter { selected.value.contains(it.channel_id) }
                                            onSelectionChanged(finalList)
                                        })
                                        Text(channel.channel_name, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AddFavouriteChannelsDialog(
    context: Context,
    allChannels: List<Channel>,
    alreadySelected: Set<String>,
    onDismiss: () -> Unit,
    onSelectionChanged: (List<Channel>) -> Unit
) {
    val preferenceManager = SkySharedPref.getInstance(context)
    // Provided JioTV category mapping (Name -> ID)
    val jioCategoryNameToId: Map<String, Int?> = mapOf(
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
    // Build reverse map for id -> name (excluding null / Reset)
    val categoryIdToName: Map<Int, String> = jioCategoryNameToId.filterValues { it != null }.map { it.value!! to it.key }.toMap()
    // Build list of categories present in current channel list, with readable names.
    data class Cat(val id: Int, val label: String)
    val categories = remember(allChannels) {
        allChannels.map { ch ->
            val label = categoryIdToName[ch.channelCategoryId] ?: "Cat ${ch.channelCategoryId}"
            Cat(ch.channelCategoryId, label)
        }.distinctBy { it.id }
            .sortedBy { it.label }
    }
    var activeCategory by remember { mutableStateOf<Int?>(null) }
    val filteredChannels = allChannels.filter { activeCategory == null || it.channelCategoryId == activeCategory }
    val selectedIds = remember { mutableStateOf(alreadySelected.toMutableSet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Favourite Channels", fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                // Category buttons row (includes 'All') with highlight
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        val selected = activeCategory == null
                        OutlinedButton(
                            onClick = { activeCategory = null },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text("All") }
                    }
                    items(categories, key = { it.id }) { cat ->
                        val selected = activeCategory == cat.id
                        OutlinedButton(
                            onClick = { activeCategory = if (selected) null else cat.id },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text(cat.label) }
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (filteredChannels.isEmpty()) {
                    Text(
                        text = if (allChannels.isEmpty()) "No channels loaded" else "No channels in this category",
                        fontSize = 12.sp
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 420.dp)
                    ) {
                        items(filteredChannels, key = { it.channel_id }) { channel ->
                            var isChecked by remember { mutableStateOf(selectedIds.value.contains(channel.channel_id)) }
                            Card(
                                modifier = Modifier.width(140.dp).height(140.dp).clickable {
                                    isChecked = !isChecked
                                    if (isChecked) selectedIds.value.add(channel.channel_id) else selectedIds.value.remove(channel.channel_id)
                                    val finalList = allChannels.filter { selectedIds.value.contains(it.channel_id) }
                                    onSelectionChanged(finalList)
                                },
                                border = if (isChecked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val logo = "http://localhost:${preferenceManager.myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}"
                                    GlideImage(
                                        model = logo,
                                        contentDescription = channel.channel_name,
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isChecked, onCheckedChange = { checked ->
                                            isChecked = checked
                                            if (checked) selectedIds.value.add(channel.channel_id) else selectedIds.value.remove(channel.channel_id)
                                            val finalList = allChannels.filter { selectedIds.value.contains(it.channel_id) }
                                            onSelectionChanged(finalList)
                                        })
                                        Text(channel.channel_name, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

private fun playChannel(context: Context, channel: Channel, channels: List<Channel>) {
    try {
        val intent = Intent(context, ExoPlayJet::class.java).apply {
            putExtra("video_url", channel.channel_url)
            putExtra("zone", "TV")
            val allChannelsData = ArrayList(channels.map { ch ->
                val fullLogoUrl = "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/jtvimage/${ch.logoUrl}"
                ChannelInfo(ch.channel_url, fullLogoUrl, ch.channel_name)
            })
            putParcelableArrayListExtra("channel_list_data", allChannelsData)
            putExtra("current_channel_index", channels.indexOf(channel))
            putExtra("video_url", channel.channel_url)
            putExtra("logo_url", "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}/jtvimage/${channel.logoUrl}")
            putExtra("ch_name", channel.channel_name)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to play channel", Toast.LENGTH_SHORT).show()
    }
}
