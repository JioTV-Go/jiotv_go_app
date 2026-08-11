package com.skylake.skytv.jgorunner.ui.tvhome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.core.LocalServerProbeStatus
import com.skylake.skytv.jgorunner.core.execution.runBinary
import com.skylake.skytv.jgorunner.core.probeLocalServer
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val OMNI_TV_TIMEOUT_MS = 5_000L
private const val OMNI_TV_POLL_DELAY_MS = 250L
private const val OMNI_TV_SETTLE_DELAY_MS = 900L
private const val OMNI_TV_MAX_FALLBACKS = 4

private data class OmniStartupResult(val ready: Boolean, val reason: String? = null)

private sealed class OmniStartupState {
    data object Idle : OmniStartupState()
    data object Checking : OmniStartupState()
    data class Timeout(val reason: String) : OmniStartupState()
    data class Failure(val reason: String) : OmniStartupState()
}

private suspend fun omniProbeStream(url: String, timeoutMs: Int = 1750): Boolean {
    return withContext(Dispatchers.IO) {
        fun exec(method: String): Int? = try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = method; c.connectTimeout = timeoutMs; c.readTimeout = timeoutMs
            c.instanceFollowRedirects = true
            val code = c.responseCode; c.disconnect(); code
        } catch (_: Exception) { null }
        try {
            val h = exec("HEAD")
            if (h != null && h in 200..299) return@withContext true
            if (h == 405 || h == 501) { val g = exec("GET"); return@withContext g != null && g in 200..299 }
            false
        } catch (_: Exception) { false }
    }
}

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OmniTvLayout(context: Context, reloadTrigger: Int) {
    val preferenceManager = SkySharedPref.getInstance(context)
    val localPORT by remember { mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort) }
    val baseURL = "http://localhost:$localPORT"
    val cachePrefs = "omni_tv_channel_cache"
    val cacheKey = "channels_json"
    val cacheTimeKey = "channels_cache_updated_at_ms"
    val cacheTtl = 12L * 3600L * 1000L

    val channelsResponse = remember { mutableStateOf<ChannelResponse?>(null) }
    val filteredChannels = remember { mutableStateOf<List<Channel>>(emptyList()) }
    var fetched by remember { mutableStateOf(false) }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var epgData by remember { mutableStateOf<EpgProgram?>(null) }
    var isEpgLoading by remember { mutableStateOf(false) }
    var epgError by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var reloadCount by rememberSaveable { mutableIntStateOf(0) }
    var waitDots by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(5) }
    var retryLoopActive by remember { mutableStateOf(false) }
    var startupState by remember { mutableStateOf<OmniStartupState>(OmniStartupState.Idle) }
    var retryToken by rememberSaveable { mutableIntStateOf(0) }
    var launchSessionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var launchInProgress by remember { mutableStateOf(false) }
    var autoplayDone by remember { mutableStateOf(false) }
    var resumeToken by remember { mutableIntStateOf(0) }
    var freeOnly by rememberSaveable { mutableStateOf(preferenceManager.myPrefs.freeOnly) }
    var freeJioCatchup by rememberSaveable { mutableStateOf(preferenceManager.myPrefs.freeJioCatchup) }
    var catchupTarget by remember { mutableStateOf<Channel?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val catMap = mapOf(
        "All" to null, "Entertainment" to 5, "Movies" to 6, "Kids" to 7,
        "Sports" to 8, "Lifestyle" to 9, "Infotainment" to 10, "News" to 12,
        "Music" to 13, "Devotional" to 15, "Business" to 16,
        "Educational" to 17, "Shopping" to 18, "JioDarshan" to 19
    )
    val savedCatIds = preferenceManager.myPrefs.filterCI
        ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    var selectedCatIds by rememberSaveable { mutableStateOf(savedCatIds) }

    val langById = mapOf(1 to "Hindi", 2 to "Marathi", 3 to "Punjabi", 4 to "Urdu",
        5 to "Bengali", 6 to "English", 7 to "Malayalam", 8 to "Tamil",
        9 to "Gujarati", 10 to "Odia", 11 to "Telugu", 12 to "Bhojpuri",
        13 to "Kannada", 14 to "Assamese", 15 to "Nepali", 16 to "French", 18 to "Other")
    val lang2Id = preferenceManager.myPrefs.filterLI2?.trim()?.toIntOrNull()
    val lang2Name = lang2Id?.let { langById[it] } ?: "Language"
    val lang2CatIds = catMap.values.filterNotNull()
    val savedLang2CatIds = preferenceManager.myPrefs.filterCI2
        ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
    var selectedLang2CatIds by rememberSaveable { mutableStateOf(savedLang2CatIds) }
    val showLang2Selector = lang2Id != null

    fun lang2Label(catId: Int): String {
        val n = catMap.entries.firstOrNull { it.value == catId }?.key?.trim().orEmpty()
        return if (n.isBlank()) "$lang2Name-Cat-$catId"
        else if (n.startsWith(lang2Name, ignoreCase = true)) n.replace(" ", "-")
        else "$lang2Name-${n.replace(" ", "-")}"
    }

    val sortedCats = remember(selectedCatIds) {
        val others = catMap.keys.filter { it != "All" }
        val (sel, unsel) = others.partition { catMap[it] != null && selectedCatIds.contains(catMap[it]) }
        listOf("All") + sel + unsel
    }

    fun langIdsFromPrefs(): List<Int>? =
        preferenceManager.myPrefs.filterLI?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.isNotEmpty() }

    fun applyFilters(resp: ChannelResponse): List<Channel> {
        var base = ChannelUtils.filterChannels(resp, categoryIds = selectedCatIds.takeIf { it.isNotEmpty() }?.toList(), languageIds = langIdsFromPrefs())
        if (freeOnly) base = base.filter { !it.requiresSubscription }
        if (lang2Id == null || selectedLang2CatIds.isEmpty()) return base
        var sec = ChannelUtils.filterChannels(resp, categoryIds = selectedLang2CatIds.toList(), languageIds = listOf(lang2Id))
        if (freeOnly) sec = sec.filter { !it.requiresSubscription }
        return (base + sec).distinctBy { "${it.channel_id}|${it.channel_url}" }
    }

    suspend fun fetchBackend(): List<Channel> {
        return try {
            ChannelUtils.fetchChannels("$baseURL/channels")?.let { r ->
                channelsResponse.value = r
                context.getSharedPreferences(cachePrefs, Context.MODE_PRIVATE).edit {
                    putString(cacheKey, Gson().toJson(r)); putLong(cacheTimeKey, System.currentTimeMillis())
                }
                applyFilters(r).also { filteredChannels.value = it }
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun ensureReady(): OmniStartupResult {
        val port = preferenceManager.myPrefs.jtvGoServerPort
        val act = context as? ComponentActivity ?: return OmniStartupResult(false, "Activity context required")
        var lastReason: String? = null; var started = false
        val result = withTimeoutOrNull<OmniStartupResult>(OMNI_TV_TIMEOUT_MS) {
            var attempt = 0
            while (true) {
                if (!started) { withContext(Dispatchers.Main) { runBinary(act, emptyArray(), {}, {}, forceStart = false) }; started = true }
                attempt++
                when (probeLocalServer(port)) {
                    LocalServerProbeStatus.READY -> return@withTimeoutOrNull OmniStartupResult(ready = true)
                    LocalServerProbeStatus.RUNNING_BUT_LOGIN_REQUIRED -> lastReason = "Server reachable but login required"
                    LocalServerProbeStatus.UNREACHABLE -> lastReason = "Attempt $attempt: no HTTP response"
                }
                delay(OMNI_TV_POLL_DELAY_MS)
            }
            error("unreachable")
        }
        return result ?: OmniStartupResult(false, lastReason ?: "Timed out after ${OMNI_TV_TIMEOUT_MS / 1000}s")
    }

    suspend fun launchChannel(channels: List<Channel>, sessionKey: String): Boolean {
        if (channels.isEmpty() || launchInProgress || launchSessionKey == sessionKey) return false
        launchInProgress = true; launchSessionKey = sessionKey
        return try {
            startupState = OmniStartupState.Checking; showLoading = true
            val r = ensureReady()
            if (!r.ready) { startupState = OmniStartupState.Timeout(r.reason ?: "Timed out"); showLoading = false; return false }
            delay(OMNI_TV_SETTLE_DELAY_MS)
            for ((i, ch) in channels.take(OMNI_TV_MAX_FALLBACKS).withIndex()) {
                if (!omniProbeStream(ch.channel_url, 900)) continue
                val (win, rel) = buildChannelInfoWindow(context, channels, baseURL, i)
                val intent = Intent(context, ExoPlayJet::class.java).apply {
                    putExtra("zone", "TV")
                    if (ch.channel_id.all { it.isDigit() }) putExtra("channel_list_kind", "jio")
                    putExtra("current_channel_index", rel); putParcelableArrayListExtra("channel_list_data", win)
                    putExtra("video_url", ch.channel_url)
                    putExtra("logo_url", if (ch.logoUrl.startsWith("http")) ch.logoUrl else "$baseURL/jtvimage/${ch.logoUrl}")
                    putExtra("ch_name", ch.channel_name)
                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) { context.startActivity(intent) }
                val type = object : TypeToken<List<Channel>>() {}.type
                val recents: MutableList<Channel> = Gson().fromJson(preferenceManager.myPrefs.recentChannels, type) ?: mutableListOf()
                val ei = recents.indexOfFirst { it.channel_id == ch.channel_id }
                if (ei != -1) { val e = recents.removeAt(ei); recents.add(0, e) }
                else { recents.add(0, ch); if (recents.size > 25) recents.removeAt(recents.size - 1) }
                preferenceManager.myPrefs.currChannelUrl = ch.channel_url
                preferenceManager.myPrefs.recentChannels = Gson().toJson(recents)
                preferenceManager.savePreferences()
                startupState = OmniStartupState.Idle; showLoading = false; return true
            }
            startupState = OmniStartupState.Failure("All autoplay candidates failed preflight"); showLoading = false; false
        } catch (_: Exception) { startupState = OmniStartupState.Failure("Launch failed"); showLoading = false; false }
        finally { launchInProgress = false }
    }

    suspend fun reload(): List<Channel> { reloadCount++; showLoading = true; return fetchBackend().also { filteredChannels.value = it; showLoading = false } }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev -> if (ev == Lifecycle.Event.ON_RESUME) { autoplayDone = false; resumeToken++ } }
        lifecycleOwner.lifecycle.addObserver(obs); onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(reloadTrigger, retryToken, resumeToken) {
        val sk = "$reloadTrigger:$retryToken:$resumeToken"
        showLoading = true
        val sp = context.getSharedPreferences(cachePrefs, Context.MODE_PRIVATE)
        val cachedJson = sp.getString(cacheKey, null)
        val cacheTime = sp.getLong(cacheTimeKey, 0L)
        val isFresh = cacheTime > 0L && (System.currentTimeMillis() - cacheTime) <= cacheTtl
        var cached: ChannelResponse? = null; var hasCached = false
        if (!cachedJson.isNullOrEmpty()) {
            try { cached = Gson().fromJson(cachedJson, ChannelResponse::class.java); hasCached = cached != null; if (hasCached) channelsResponse.value = cached }
            catch (_: Exception) { sp.edit { remove(cacheKey); remove(cacheTimeKey) } }
        }
        if (hasCached && isFresh && cached != null) {
            filteredChannels.value = applyFilters(cached); fetched = true
        } else {
            var attempts = 0; var ok = false
            while (attempts < 2 && !ok) {
                attempts++
                try {
                    val r = ChannelUtils.fetchChannels("$baseURL/channels")
                    if (r != null) { channelsResponse.value = r; sp.edit { putString(cacheKey, Gson().toJson(r)); putLong(cacheTimeKey, System.currentTimeMillis()) }; filteredChannels.value = applyFilters(r); ok = true }
                } catch (_: Exception) {}
                if (!ok) delay(300)
            }
            if (!ok && hasCached && cached != null) filteredChannels.value = applyFilters(cached)
            fetched = true
        }
        if (preferenceManager.myPrefs.startTvAutomatically && !autoplayDone) {
            var ch = filteredChannels.value; if (ch.isEmpty()) ch = fetchBackend()
            if (ch.isNotEmpty()) { if (launchChannel(ch, sk)) autoplayDone = true }
            else { showLoading = false; startupState = OmniStartupState.Idle }
        } else { showLoading = false; startupState = OmniStartupState.Idle }
    }

    LaunchedEffect(showLang2Selector, lang2Id) {
        if (!showLang2Selector && selectedLang2CatIds.isNotEmpty()) { selectedLang2CatIds = emptySet(); preferenceManager.myPrefs.filterCI2 = ""; preferenceManager.savePreferences() }
    }
    LaunchedEffect(selectedCatIds, selectedLang2CatIds, lang2Id, freeOnly) {
        channelsResponse.value?.let { filteredChannels.value = applyFilters(it) }
    }
    LaunchedEffect(fetched, filteredChannels.value) {
        if (fetched && filteredChannels.value.isEmpty() && !retryLoopActive) {
            retryLoopActive = true
            try {
                var wait = 2
                while (filteredChannels.value.isEmpty()) {
                    for (i in wait downTo 1) { if (filteredChannels.value.isNotEmpty()) break; countdown = i; waitDots = ".".repeat(((wait - i) % 3) + 1); delay(1000) }
                    if (filteredChannels.value.isNotEmpty()) break
                    reload(); wait = 5
                }
            } finally { retryLoopActive = false }
        }
    }
    LaunchedEffect(focusedChannel) {
        delay(250L)
        if (focusedChannel != null) {
            isEpgLoading = true; epgError = false
            try {
                val e = ChannelUtils.fetchEpg("$baseURL/epg/${focusedChannel!!.channel_id}/0")
                if (e != null) epgData = e else { epgData = null; epgError = true }
            } catch (_: Exception) { epgData = null; epgError = true }
            finally { isEpgLoading = false }
        } else { epgData = null; epgError = false; isEpgLoading = false }
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val focusColor = if (isDark) Color(0xFF00E5FF) else Color(0xFFFFD700)
    val state = startupState

    when {
        state is OmniStartupState.Timeout || state is OmniStartupState.Failure -> {
            val reason = when (state) { is OmniStartupState.Timeout -> state.reason; is OmniStartupState.Failure -> state.reason; else -> "" }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state is OmniStartupState.Timeout) "Startup timed out" else "Startup failed", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = Color.Red)
                    Spacer(Modifier.height(12.dp)); Text(reason, style = TextStyle(fontSize = 15.sp), color = Color.White); Spacer(Modifier.height(20.dp))
                    ElevatedCard(onClick = { startupState = OmniStartupState.Checking; showLoading = true; retryToken++ }) {
                        Row(Modifier.padding(22.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Refresh, "Retry"); Spacer(Modifier.width(8.dp)); Text("Retry startup") }
                    }
                    Spacer(Modifier.height(12.dp))
                    ElevatedCard(onClick = { startupState = OmniStartupState.Idle; showLoading = !fetched }) {
                        Row(Modifier.padding(22.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Done, "Open"); Spacer(Modifier.width(8.dp)); Text("Open channel list") }
                    }
                }
            }
        }
        showLoading || state is OmniStartupState.Checking -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ContainedLoadingIndicator(Modifier.size(100.dp)); Spacer(Modifier.height(16.dp))
                    Text(if (state is OmniStartupState.Checking) "Waiting for server..." else "Loading channels...", style = TextStyle(fontSize = 16.sp), color = Color.White)
                }
            }
        }
        fetched && filteredChannels.value.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No channels found", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Color.Red)
                    Spacer(Modifier.height(16.dp)); Text(waitDots, style = TextStyle(fontSize = 20.sp)); Spacer(Modifier.height(10.dp))
                    Text("Auto-retrying in ${countdown}s...", style = TextStyle(fontSize = 16.sp), color = Color.Blue); Spacer(Modifier.height(24.dp))
                    ElevatedCard(onClick = { scope.launch { reload() } }) {
                        Row(Modifier.padding(24.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Refresh, "Reload"); Spacer(Modifier.width(8.dp)); Text("Reload") }
                    }
                }
            }
        }
        else -> {
            // Category chips row + checkboxes
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sortedCats) { catName ->
                        val catId = catMap[catName]; val isSel = catId != null && selectedCatIds.contains(catId)
                        var isFocused by remember { mutableStateOf(false) }
                        FilterChip(
                            modifier = Modifier.onFocusChanged { isFocused = it.isFocused }.border(2.dp, if (isFocused) focusColor else Color.Transparent, RoundedCornerShape(8.dp)),
                            onClick = {
                                selectedCatIds = if (catName == "All") emptySet()
                                else if (catId != null) (if (isSel) selectedCatIds - catId else selectedCatIds + catId) else selectedCatIds
                                preferenceManager.myPrefs.filterCI = selectedCatIds.joinToString(","); preferenceManager.savePreferences()
                            },
                            label = { Text(catName) },
                            selected = if (catName == "All") selectedCatIds.isEmpty() else isSel,
                            leadingIcon = when {
                                catName == "All" && selectedCatIds.isEmpty() -> { { Icon(Icons.Filled.Done, null, Modifier.size(FilterChipDefaults.IconSize)) } }
                                isSel -> { { Icon(Icons.Filled.Done, null, Modifier.size(FilterChipDefaults.IconSize)) } }
                                else -> null
                            }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                var foFocused by remember { mutableStateOf(false) }
                Row(Modifier.onFocusChanged { foFocused = it.isFocused }.border(2.dp, if (foFocused) focusColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { freeOnly = !freeOnly; preferenceManager.myPrefs.freeOnly = freeOnly; preferenceManager.savePreferences() }.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = freeOnly, onCheckedChange = { freeOnly = it; preferenceManager.myPrefs.freeOnly = it; preferenceManager.savePreferences() }, Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp)); Text("Free only", style = TextStyle(fontSize = 14.sp, color = Color.White))
                }
                Spacer(Modifier.width(8.dp))
                var cuFocused by remember { mutableStateOf(false) }
                Row(Modifier.onFocusChanged { cuFocused = it.isFocused }.border(2.dp, if (cuFocused) focusColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { freeJioCatchup = !freeJioCatchup; preferenceManager.myPrefs.freeJioCatchup = freeJioCatchup; preferenceManager.savePreferences() }.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = freeJioCatchup, onCheckedChange = { freeJioCatchup = it; preferenceManager.myPrefs.freeJioCatchup = it; preferenceManager.savePreferences() }, Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp)); Text("Catchup", style = TextStyle(fontSize = 14.sp, color = Color.White))
                }
            }
            // Second language addon row
            if (showLang2Selector) {
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(lang2CatIds) { catId ->
                        val lbl = lang2Label(catId); val isSel = selectedLang2CatIds.contains(catId)
                        var isFocused by remember { mutableStateOf(false) }
                        FilterChip(modifier = Modifier.onFocusChanged { isFocused = it.isFocused }.border(2.dp, if (isFocused) focusColor else Color.Transparent, RoundedCornerShape(8.dp)),
                            onClick = {
                                selectedLang2CatIds = if (isSel) selectedLang2CatIds - catId else selectedLang2CatIds + catId
                                preferenceManager.myPrefs.filterCI2 = selectedLang2CatIds.joinToString(","); preferenceManager.savePreferences()
                            },
                            label = { Text(lbl) }, selected = isSel,
                            leadingIcon = if (isSel) { { Icon(Icons.Filled.Done, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null)
                    }
                }
            }
            // EPG card
            if (isEpgLoading || epgData != null || epgError) {
                Card(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 12.dp, vertical = 4.dp), elevation = CardDefaults.cardElevation(6.dp), shape = RoundedCornerShape(16.dp)) {
                    when {
                        isEpgLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Loading EPG...", color = Color.Gray) }
                        epgError -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No EPG available", color = Color.Gray) }
                        epgData != null -> Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(start = 8.dp, end = 12.dp).heightIn(max = 110.dp)) {
                                Text(epgData!!.channel_name, style = TextStyle(fontSize = 14.sp)); Spacer(Modifier.height(4.dp))
                                Text(epgData!!.showname, maxLines = 1, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)); Spacer(Modifier.height(6.dp))
                                Text(epgData!!.description, style = TextStyle(fontSize = 12.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            GlideImage(model = "$baseURL/jtvposter/${epgData!!.episodePoster}", contentDescription = null,
                                modifier = Modifier.height(90.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Fit)
                        }
                    }
                }
            }
            // Channel grid
            OmniTvGrid(context, filteredChannels.value, { focusedChannel = it }, localPORT, preferenceManager, freeJioCatchup) { catchupTarget = it }
            catchupTarget?.let { OmniCatchupOverlay(it, localPORT, { catchupTarget = null }, context) }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun OmniTvGrid(
    context: Context, channels: List<Channel>,
    onFocused: (Channel) -> Unit, localPORT: Int,
    prefs: SkySharedPref, catchupMode: Boolean,
    onCatchupClick: (Channel) -> Unit
) {
    val base = "http://localhost:$localPORT"
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val borderColor = if (isDark) Color(0xFF00BCD4) else Color(0xFFFFD700)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(channels) { ch ->
            var focused by remember { mutableStateOf(false) }
            Card(Modifier.height(160.dp)
                .onFocusChanged { s -> focused = s.isFocused; if (s.isFocused) onFocused(ch) }
                .border(4.dp, if (focused) borderColor else Color.Transparent, CardDefaults.shape)
                .combinedClickable(
                    onClick = {
                        if (catchupMode) { onCatchupClick(ch); return@combinedClickable }
                        val idx = channels.indexOf(ch).coerceAtLeast(0)
                        val (win, rel) = buildChannelInfoWindow(context, channels, base, idx)
                        context.startActivity(Intent(context, ExoPlayJet::class.java).apply {
                            putExtra("video_url", ch.channel_url); putExtra("zone", "TV")
                            if (ch.channel_id.all { it.isDigit() }) putExtra("channel_list_kind", "jio")
                            putExtra("current_channel_index", rel); putParcelableArrayListExtra("channel_list_data", win)
                            putExtra("logo_url", if (ch.logoUrl.startsWith("http")) ch.logoUrl else "$base/jtvimage/${ch.logoUrl}")
                            putExtra("ch_name", ch.channel_name)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                        val type = object : TypeToken<List<Channel>>() {}.type
                        val r: MutableList<Channel> = Gson().fromJson(prefs.myPrefs.recentChannels, type) ?: mutableListOf()
                        val ei = r.indexOfFirst { it.channel_id == ch.channel_id }
                        if (ei != -1) { val e = r.removeAt(ei); r.add(0, e) } else { r.add(0, ch); if (r.size > 25) r.removeAt(r.size - 1) }
                        prefs.myPrefs.recentChannels = Gson().toJson(r); prefs.savePreferences()
                    },
                    onLongClick = {
                        if (PlayerCommandBus.isInPipMode) { PlayerCommandBus.requestSwitch(url = ch.channel_url) }
                        else {
                            val idx = channels.indexOf(ch).coerceAtLeast(0)
                            val (win, rel) = buildChannelInfoWindow(context, channels, base, idx)
                            context.startActivity(Intent(context, ExoPlayJet::class.java).apply {
                                putExtra("video_url", ch.channel_url); putExtra("zone", "TV")
                                if (ch.channel_id.all { it.isDigit() }) putExtra("channel_list_kind", "jio")
                                putExtra("current_channel_index", rel); putParcelableArrayListExtra("channel_list_data", win)
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    }
                ),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                val img = if (ch.logoUrl.startsWith("http")) ch.logoUrl else "$base/jtvimage/${ch.logoUrl}"
                GlideImage(img, ch.channel_name, Modifier.fillMaxWidth().height(90.dp), contentScale = ContentScale.Fit)
                Text(text = ch.channel_name, fontSize = 10.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun OmniCatchupOverlay(ch: Channel, localPORT: Int, onClose: () -> Unit, context: Context) {
    var dayOffset by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var epgList by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    val base = "http://localhost:$localPORT"
    BackHandler { onClose() }
    LaunchedEffect(dayOffset, ch.channel_id) {
        loading = true; errMsg = null
        try {
            withContext(Dispatchers.IO) {
                val conn = URL("$base/epg/${ch.channel_id}/$dayOffset").openConnection() as HttpURLConnection
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val resp = Gson().fromJson(json, EpgResponse::class.java)
                val now = System.currentTimeMillis()
                val parsed = resp.epg.map { p ->
                    val s = if (p.startEpoch < 100000000000L) p.startEpoch * 1000 else p.startEpoch
                    val e = if (p.endEpoch < 100000000000L) p.endEpoch * 1000 else p.endEpoch
                    p.copy(startEpoch = s, endEpoch = e)
                }
                val past = parsed.filter { it.startEpoch <= now }
                val final = if (dayOffset == 0) {
                    val live = past.find { now >= it.startEpoch && now <= it.endEpoch }
                    if (live != null) listOf(live) + past.filter { it.srno != live.srno }.reversed() else past.reversed()
                } else past.reversed()
                withContext(Dispatchers.Main) { epgList = final; loading = false }
            }
        } catch (e: Exception) { Log.e("OmniCatchup", "Error", e); withContext(Dispatchers.Main) { errMsg = "Failed to load catchup guide"; loading = false } }
    }
    val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val todayCal = Calendar.getInstance()
    val isTv = LocalConfiguration.current.screenWidthDp >= 600
    Box(Modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.width(8.dp))
                GlideImage(model = if (ch.logoUrl.startsWith("http")) ch.logoUrl else "$base/jtvimage/${ch.logoUrl}",
                    contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(12.dp))
                Text("${ch.channel_name} - Catchup Guide", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
            LazyRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0 downTo -7).toList()) { offset ->
                    val cal = todayCal.clone() as Calendar; cal.add(Calendar.DAY_OF_YEAR, offset)
                    val lbl = if (offset == 0) "Today" else if (offset == -1) "Yesterday" else fmt.format(cal.time)
                    var f by remember { mutableStateOf(false) }
                    FilterChip(
                        selected = offset == dayOffset,
                        onClick = { dayOffset = offset },
                        label = { Text(text = lbl, color = if (offset == dayOffset) Color.Black else Color.White) },
                        modifier = Modifier.onFocusChanged { f = it.isFocused }.border(2.dp, if (f) Color(0xFF00E5FF) else Color.Transparent, RoundedCornerShape(8.dp)),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF), containerColor = Color(0xFF262626))
                    )
                }
            }
            when {
                loading -> Box(Modifier.fillMaxSize().weight(1f), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00E5FF)) }
                errMsg != null -> Box(Modifier.fillMaxSize().weight(1f), Alignment.Center) { Text(text = errMsg!!, color = Color.Red) }
                epgList.isEmpty() -> Box(Modifier.fillMaxSize().weight(1f), Alignment.Center) { Text(text = "No shows available", color = Color.Gray) }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (isTv) 320.dp else 260.dp),
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(epgList) { _, prog ->
                        val now2 = System.currentTimeMillis()
                        val isLive = now2 >= prog.startEpoch && now2 <= prog.endEpoch
                        OmniCatchupTile(prog, isLive, localPORT) {
                            val url = if (isLive) ch.channel_url else "$base/catchup/render/${ch.channel_id}?start=${prog.startEpoch}&end=${prog.endEpoch}&srno=${prog.srno}"
                            context.startActivity(Intent(context, ExoPlayJet::class.java).apply {
                                putExtra("video_url", url); putExtra("zone", "TV")
                                if (ch.channel_id.all { it.isDigit() }) putExtra("channel_list_kind", "jio")
                                putExtra("current_channel_index", -1)
                                putExtra("logo_url", if (ch.logoUrl.startsWith("http")) ch.logoUrl else "$base/jtvimage/${ch.logoUrl}")
                                putExtra("ch_name", if (isLive) "[LIVE] ${prog.showname}" else prog.showname)
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun OmniCatchupTile(prog: EpgProgram, isLive: Boolean, localPORT: Int, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp); val cyan = Color(0xFF00E5FF)
    Card(Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.border(2.dp, if (focused) cyan else Color.Transparent, shape).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(if (focused) Color(0xFF1F353D) else Color(0xFF1E1E1E)), shape = shape) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(110.dp).aspectRatio(1.5f).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                GlideImage("http://localhost:$localPORT/jtvposter/${prog.episodePoster}", null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (isLive) Box(Modifier.align(Alignment.TopStart).background(Color.Red, RoundedCornerShape(bottomEnd = 4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = "LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(prog.showname, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${prog.showtime} - ${prog.endtime}", color = if (isLive) cyan else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(prog.description, color = Color.LightGray, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
