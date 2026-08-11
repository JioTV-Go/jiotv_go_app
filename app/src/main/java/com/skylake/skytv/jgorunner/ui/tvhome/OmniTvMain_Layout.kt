package com.skylake.skytv.jgorunner.ui.tvhome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.core.LocalServerProbeStatus
import com.skylake.skytv.jgorunner.core.probeLocalServer
import com.skylake.skytv.jgorunner.core.execution.runBinary
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.BinaryService
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet
import com.skylake.skytv.jgorunner.utils.withQuality
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues

private const val TV_STARTUP_TIMEOUT_MS = 5_000L
private const val TV_STARTUP_POLL_DELAY_MS = 250L
private const val TV_AUTOPLAY_SETTLE_DELAY_MS = 3_000L
private const val TV_STARTUP_MAX_FALLBACK_CHANNELS = 4
private const val TV_STREAM_CONNECT_TIMEOUT_MS = 1_250L
private const val TV_STREAM_READ_TIMEOUT_MS = 1_250L
private const val TV_STREAM_CALL_TIMEOUT_MS = 1_750L
private const val TV_PREFLIGHT_CONNECT_TIMEOUT_MS = 650L
private const val TV_PREFLIGHT_READ_TIMEOUT_MS = 650L
private const val TV_PREFLIGHT_CALL_TIMEOUT_MS = 900L

private val startupProbeClient = OkHttpClient.Builder()
    .connectTimeout(TV_STREAM_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .readTimeout(TV_STREAM_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .callTimeout(TV_STREAM_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

private val preflightProbeClient = OkHttpClient.Builder()
    .connectTimeout(TV_PREFLIGHT_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .readTimeout(TV_PREFLIGHT_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .callTimeout(TV_PREFLIGHT_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

private data class TvStartupReadiness(
    val ready: Boolean,
    val reason: String? = null
)

private sealed class TvStartupOutcome {
    data object Idle : TvStartupOutcome()
    data object Checking : TvStartupOutcome()
    data class Timeout(val reason: String) : TvStartupOutcome()
    data class Failure(val reason: String) : TvStartupOutcome()
}

private suspend fun probeStreamEndpoint(
    url: String,
    isPreflightProbe: Boolean = false
): Boolean {
    return withContext(Dispatchers.IO) {
        val client = if (isPreflightProbe) preflightProbeClient else startupProbeClient

        fun execute(method: String): Int? {
            val request = Request.Builder()
                .url(url)
                .method(method, null)
                .build()

            return client.newCall(request).execute().use { response ->
                response.code
            }
        }

        try {
            val headCode = execute("HEAD")
            if (headCode != null && headCode in 200..299) {
                return@withContext true
            }

            if (headCode == 405 || headCode == 501) {
                val getCode = execute("GET")
                return@withContext getCode != null && getCode in 200..299
            }

            false
        } catch (_: Exception) {
            false
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OmniTvMain_Layout(context: Context, reloadTrigger: Int) {
    rememberCoroutineScope()
    val channelsResponse = remember { mutableStateOf<ChannelResponse?>(null) }
    val filteredChannels = remember { mutableStateOf<List<Channel>>(emptyList()) }
    val preferenceManager = SkySharedPref.getInstance(context)
    val localPORT by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort)
    }
    val basefinURL = "http://localhost:$localPORT"
    val channelCachePrefsName = "channel_cache"
    val channelCacheJsonKey = "channels_json"
    val channelCacheUpdatedAtKey = "channels_cache_updated_at_ms"
    val channelCacheTtlMs = 12L * 60L * 60L * 1000L
    var fetched by remember { mutableStateOf(false) }

    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var epgData by remember { mutableStateOf<EpgProgram?>(null) }
    var isEpgLoading by remember { mutableStateOf(false) }
    var epgError by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var reloadAttemptCount by rememberSaveable { mutableIntStateOf(0) }
    var waitingDots by remember { mutableStateOf("") }
    var autoLoadCountdown by remember { mutableIntStateOf(5) }
    var autoRetryLoopRunning by remember { mutableStateOf(false) }
    var startupOutcome by remember { mutableStateOf<TvStartupOutcome>(TvStartupOutcome.Idle) }
    var startupRetryToken by rememberSaveable { mutableIntStateOf(0) }
    var startupLaunchSessionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var startupLaunchInProgress by remember { mutableStateOf(false) }
    var autoplayLaunched by remember { mutableStateOf(false) }
    var autoplayResumeToken by remember { mutableIntStateOf(0) }
    var freeOnly by rememberSaveable { mutableStateOf(preferenceManager.myPrefs.freeOnly) }
    var freeJioCatchup by rememberSaveable { mutableStateOf(preferenceManager.myPrefs.freeJioCatchup) }
    var catchupChannelTarget by remember { mutableStateOf<Channel?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    remember { FocusRequester() }
    val categoryMap = mapOf(
        "All" to null,
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

    val savedCategoryIds = preferenceManager.myPrefs.filterCI
        ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                autoplayLaunched = false
                autoplayResumeToken++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
//    var selectedCategoryIds by remember { mutableStateOf(savedCategoryIds) }
    var selectedCategoryIds by rememberSaveable { mutableStateOf(savedCategoryIds.toSet()) }
    val languageNameById = mapOf(
        1 to "Hindi",
        2 to "Marathi",
        3 to "Punjabi",
        4 to "Urdu",
        5 to "Bengali",
        6 to "English",
        7 to "Malayalam",
        8 to "Tamil",
        9 to "Gujarati",
        10 to "Odia",
        11 to "Telugu",
        12 to "Bhojpuri",
        13 to "Kannada",
        14 to "Assamese",
        15 to "Nepali",
        16 to "French",
        18 to "Other"
    )
    val secondLanguageIdForUi = preferenceManager.myPrefs.filterLI2
        ?.trim()
        ?.toIntOrNull()
    val secondLanguageNameForUi = secondLanguageIdForUi?.let { languageNameById[it] } ?: "Language"
    val secondLanguageAddonCategoryIds = categoryMap.values.filterNotNull()
    val savedSecondLanguageAddonCategoryIds = preferenceManager.myPrefs.filterCI2
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.toSet()
        ?: emptySet()
    fun secondLanguageAddonLabel(categoryId: Int): String {
        val originalName = categoryMap.entries.firstOrNull { it.value == categoryId }?.key?.trim().orEmpty()
        if (originalName.isBlank()) {
            return "$secondLanguageNameForUi-Category-$categoryId"
        }
        return if (originalName.startsWith(secondLanguageNameForUi, ignoreCase = true)) {
            originalName.replace(" ", "-")
        } else {
            "$secondLanguageNameForUi-${originalName.replace(" ", "-")}"
        }
    }
    var selectedSecondLanguageAddonCategoryIds by rememberSaveable {
        mutableStateOf(savedSecondLanguageAddonCategoryIds)
    }
    val showSecondLanguageAddonSelector = secondLanguageIdForUi != null

    val sortedCategories = remember(selectedCategoryIds) {
        val allCategoryName = "All"
        val allCategoryNames = categoryMap.keys.toList()
        val otherCategoryNames = allCategoryNames.filter { it != allCategoryName }
        val (selectedOtherCategories, unselectedOtherCategories) = otherCategoryNames.partition { categoryName ->
            val categoryId = categoryMap[categoryName]
            categoryId != null && selectedCategoryIds.contains(categoryId)
        }
        listOf(allCategoryName) + selectedOtherCategories + unselectedOtherCategories
    }

    fun currentLanguageIdsFromPrefs(): List<Int>? {
        return preferenceManager.myPrefs.filterLI
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
    }

    fun applyMainFilters(response: ChannelResponse): List<Channel> {
        val languageIds = currentLanguageIdsFromPrefs()
        val isFreeOnly = freeOnly

        var baseFiltered = ChannelUtils.filterChannels(
            response,
            categoryIds = selectedCategoryIds.takeIf { it.isNotEmpty() }?.toList(),
            languageIds = languageIds
        )

        if (isFreeOnly) {
            baseFiltered = baseFiltered.filter { !it.requiresSubscription }
        }

        if (secondLanguageIdForUi == null || selectedSecondLanguageAddonCategoryIds.isEmpty()) {
            return baseFiltered
        }

        var secondLanguageAddonFiltered = ChannelUtils.filterChannels(
            response,
            categoryIds = selectedSecondLanguageAddonCategoryIds.toList(),
            languageIds = listOf(secondLanguageIdForUi)
        )

        if (isFreeOnly) {
            secondLanguageAddonFiltered = secondLanguageAddonFiltered.filter { !it.requiresSubscription }
        }

        return (baseFiltered + secondLanguageAddonFiltered)
            .distinctBy { "${it.channel_id}|${it.channel_url}" }
    }


    suspend fun fetchFromBackend(): List<Channel> {
        return try {
            ChannelUtils.fetchChannels("$basefinURL/channels")?.let { response ->
                channelsResponse.value = response
                context.getSharedPreferences(channelCachePrefsName, Context.MODE_PRIVATE).edit().apply {
                    putString(channelCacheJsonKey, Gson().toJson(response))
                    putLong(channelCacheUpdatedAtKey, System.currentTimeMillis())
                    apply()
                }
                val filtered = applyMainFilters(response)
                filteredChannels.value = filtered
                filtered
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun ensureServerReady(): TvStartupReadiness {
        val port = preferenceManager.myPrefs.jtvGoServerPort

        val activity = context as? ComponentActivity
            ?: return TvStartupReadiness(false, "TV autoplay requires an activity context")

        suspend fun requestBinaryStart(forceStart: Boolean, reason: String) {
            Log.d(
                "TVAutoplay",
                "Service start requested (forceStart=$forceStart). reason=$reason"
            )
            withContext(Dispatchers.Main) {
                runBinary(
                    activity = activity,
                    arguments = emptyArray(),
                    onRunSuccess = {
                        Log.d("TVAutoplay", "Binary service start acknowledged")
                    },
                    onOutput = { },
                    forceStart = forceStart
                )
            }
        }

        var lastProbeReason: String? = null
        var startRequested = false
        val readiness: TvStartupReadiness? = withTimeoutOrNull<TvStartupReadiness>(TV_STARTUP_TIMEOUT_MS) {
            var attempt = 0
            while (true) {
                if (!startRequested) {
                    requestBinaryStart(
                        forceStart = false,
                        reason = "initial readiness gate request"
                    )
                    startRequested = true
                }

                attempt++
                Log.d("TVAutoplay", "HTTP ready probe attempt=$attempt port=$port")
                when (probeLocalServer(port)) {
                    LocalServerProbeStatus.READY -> {
                        Log.d("TVAutoplay", "HTTP ready true after attempt=$attempt")
                        return@withTimeoutOrNull TvStartupReadiness(ready = true)
                    }
                    LocalServerProbeStatus.RUNNING_BUT_LOGIN_REQUIRED -> {
                        lastProbeReason = "Server reachable but login/setup is still required"
                    }
                    LocalServerProbeStatus.UNREACHABLE -> {
                        lastProbeReason = "Attempt $attempt did not receive any HTTP response from the local server"
                    }
                }

                delay(TV_STARTUP_POLL_DELAY_MS)
            }

            error("TV startup readiness timeout block completed unexpectedly")
        }

        return readiness ?: TvStartupReadiness(
            ready = false,
            reason = lastProbeReason ?: "Timed out after ${TV_STARTUP_TIMEOUT_MS / 1000}s waiting for localhost:$port"
        )
    }

    suspend fun launchFirstChannel(channelsToUse: List<Channel>, sessionKey: String): Boolean {
        if (channelsToUse.isEmpty()) {
            return false
        }

        if (startupLaunchInProgress || startupLaunchSessionKey == sessionKey) {
            Log.d("TVAutoplay", "Skipping autoplay launch for session=$sessionKey")
            return false
        }

        startupLaunchInProgress = true
        startupLaunchSessionKey = sessionKey
        return try {
            startupOutcome = TvStartupOutcome.Checking
            showLoading = true

            val readiness = ensureServerReady()
            if (!readiness.ready) {
                val reason = readiness.reason ?: "Server readiness timed out"
                Log.w("TVAutoplay", reason)
                startupOutcome = TvStartupOutcome.Timeout(reason)
                showLoading = false
                return false
            }

            delay(TV_AUTOPLAY_SETTLE_DELAY_MS)

            val candidates = channelsToUse.take(TV_STARTUP_MAX_FALLBACK_CHANNELS)
            for ((candidateIndex, candidate) in candidates.withIndex()) {
                Log.d(
                    "TVAutoplay",
                    "Autoplay attempt index=${candidateIndex + 1}/${candidates.size} channel=${candidate.channel_name}"
                )
                if (!probeStreamEndpoint(
                        url = candidate.channel_url,
                    isPreflightProbe = true
                    )) {
                    Log.w(
                        "TVAutoplay",
                        "Skipping candidate index=$candidateIndex due to preflight failure"
                    )
                    continue
                }
                val (channelWindow, relativeIndex) = omniBuildChannelInfoWindow(
                    context = context,
                    channels = channelsToUse,
                    basefinURL = basefinURL,
                    centerIndex = candidateIndex
                )
                val intent = Intent(context, ExoPlayJet::class.java).apply {
                    putExtra("zone", "TV")
                    if (candidate.channel_id.all { it.isDigit() }) putExtra("channel_list_kind", "jio")
                    putExtra("current_channel_index", relativeIndex)
                    putParcelableArrayListExtra("channel_list_data", channelWindow)
                    putExtra("video_url", candidate.channel_url)
                    putExtra(
                        "logo_url",
                        if (candidate.logoUrl.startsWith("http")) candidate.logoUrl else "http://localhost:$localPORT/jtvimage/${candidate.logoUrl}"
                    )
                    putExtra("ch_name", candidate.channel_name)
                }

                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                }

                val recentChannelsJson = preferenceManager.myPrefs.recentChannels
                val type = object : TypeToken<List<Channel>>() {}.type
                val recentChannels: MutableList<Channel> =
                    Gson().fromJson(recentChannelsJson, type) ?: mutableListOf()

                val existingIndex = recentChannels.indexOfFirst { it.channel_id == candidate.channel_id }

                if (existingIndex != -1) {
                    val existingChannel = recentChannels[existingIndex]
                    recentChannels.removeAt(existingIndex)
                    recentChannels.add(0, existingChannel)
                } else {
                    recentChannels.add(0, candidate)
                    if (recentChannels.size > 25) {
                        recentChannels.removeAt(recentChannels.size - 1)
                    }
                }
                preferenceManager.myPrefs.currChannelUrl = candidate.channel_url
                preferenceManager.myPrefs.recentChannels = Gson().toJson(recentChannels)
                preferenceManager.savePreferences()
                startupOutcome = TvStartupOutcome.Idle
                showLoading = false
                Log.d("TVAutoplay", "Autoplay launched channel index=$candidateIndex")
                return true
            }

            val failureReason = "All ${candidates.size} autoplay candidates failed preflight"
            Log.w("TVAutoplay", failureReason)
            startupOutcome = TvStartupOutcome.Failure(failureReason)
            showLoading = false
            false
        } catch (_: Exception) {
            val failureReason = "Autoplay launch failed unexpectedly"
            startupOutcome = TvStartupOutcome.Failure(failureReason)
            showLoading = false
            false
        } finally {
            startupLaunchInProgress = false
        }
    }

    suspend fun watchdogAutoplay(channels: List<Channel>, sessionKey: String): Boolean {
        repeat(5) {
            val channelsToUse = channels.ifEmpty { fetchFromBackend() }
            if (channelsToUse.isNotEmpty()) {
                if (launchFirstChannel(channelsToUse, sessionKey)) {
                    return true
                }
            }
            delay(2000)
        }
        if (startupOutcome is TvStartupOutcome.Checking) {
            startupOutcome = TvStartupOutcome.Failure("No playable channels became available during startup")
        }
        showLoading = false
        return false
    }

    suspend fun performReloadAttempt(): List<Channel> {
        reloadAttemptCount++
        showLoading = true
        val fetchedChannels = fetchFromBackend()
        filteredChannels.value = fetchedChannels
        showLoading = false
        return fetchedChannels
    }

    // Fetch and filter channels (cache/network), then gate autoplay through one startup session.
    LaunchedEffect(reloadTrigger, startupRetryToken, autoplayResumeToken) {
        val sessionKey = "$reloadTrigger:$startupRetryToken:$autoplayResumeToken"
        showLoading = true
        val sharedPref = context.getSharedPreferences(channelCachePrefsName, Context.MODE_PRIVATE)
        var cachedChannels: ChannelResponse? = null
        var hasValidCachedChannels = false

        val cachedJson = sharedPref.getString(channelCacheJsonKey, null)
        val cacheUpdatedAt = sharedPref.getLong(channelCacheUpdatedAtKey, 0L)
        val isCacheFresh = cacheUpdatedAt > 0L &&
            (System.currentTimeMillis() - cacheUpdatedAt) <= channelCacheTtlMs

        if (!cachedJson.isNullOrEmpty()) {
            try {
                cachedChannels = Gson().fromJson(cachedJson, ChannelResponse::class.java)
                hasValidCachedChannels = cachedChannels != null
                if (hasValidCachedChannels) {
                    channelsResponse.value = cachedChannels
                }
            } catch (_: Exception) {
                sharedPref.edit {
                    remove(channelCacheJsonKey)
                    remove(channelCacheUpdatedAtKey)
                }
            }
        }

        if (hasValidCachedChannels && isCacheFresh && cachedChannels != null) {
            val filtered = applyMainFilters(cachedChannels)
            filteredChannels.value = filtered
            fetched = true
        } else {
            var attempts = 0
            var success = false
            while (attempts < 2 && !success) {
                attempts++
                try {
                    val response = ChannelUtils.fetchChannels("$basefinURL/channels")
                    channelsResponse.value = response
                    if (response != null) {
                        val responseJsonString = Gson().toJson(response)
                        sharedPref.edit {
                            putString(channelCacheJsonKey, responseJsonString)
                            putLong(channelCacheUpdatedAtKey, System.currentTimeMillis())
                        }
                        val filtered = applyMainFilters(response)
                        filteredChannels.value = filtered
                        success = true
                    }
                } catch (_: Exception) {
                    // ignore, retry
                }
                if (!success) {
                    kotlinx.coroutines.delay(300)
                }
            }

            // If refresh fails, keep app usable by falling back to stale cache.
            if (!success && hasValidCachedChannels && cachedChannels != null) {
                val filtered = applyMainFilters(cachedChannels)
                filteredChannels.value = filtered
            }
            fetched = true
        }

        val canAutoplay = !autoplayLaunched

        if (preferenceManager.myPrefs.startTvAutomatically && canAutoplay) {
            var channelsForAutoplay = filteredChannels.value
            if (channelsForAutoplay.isEmpty()) {
                channelsForAutoplay = fetchFromBackend()
            }

            if (channelsForAutoplay.isNotEmpty()) {
                if (launchFirstChannel(channelsForAutoplay, sessionKey)) {
                    autoplayLaunched = true
                }
            } else {
                if (watchdogAutoplay(channelsForAutoplay, sessionKey)) {
                    autoplayLaunched = true
                }
            }
        } else {
            showLoading = false
            startupOutcome = TvStartupOutcome.Idle
        }

    }

    LaunchedEffect(showSecondLanguageAddonSelector, secondLanguageIdForUi) {
        if (!showSecondLanguageAddonSelector && selectedSecondLanguageAddonCategoryIds.isNotEmpty()) {
            selectedSecondLanguageAddonCategoryIds = emptySet()
            preferenceManager.myPrefs.filterCI2 = ""
            preferenceManager.savePreferences()
        }
    }

    // Re-filter channels when category, second-language addon, or freeOnly selection changes
    LaunchedEffect(selectedCategoryIds, selectedSecondLanguageAddonCategoryIds, secondLanguageIdForUi, freeOnly) {
        channelsResponse.value?.let { response ->
            val filtered = applyMainFilters(response)
            filteredChannels.value = filtered
        }
    }

    // Auto-retry loading channels: first wait briefly, then retry until channels arrive.
    LaunchedEffect(fetched, filteredChannels.value) {
        if (fetched && filteredChannels.value.isEmpty() && !autoRetryLoopRunning) {
            autoRetryLoopRunning = true
            try {
                var waitSeconds = 2
                while (filteredChannels.value.isEmpty()) {
                    for (i in waitSeconds downTo 1) {
                        if (filteredChannels.value.isNotEmpty()) break
                        autoLoadCountdown = i
                        waitingDots = ".".repeat(((waitSeconds - i) % 3) + 1)
                        delay(1000)
                    }

                    if (filteredChannels.value.isNotEmpty()) {
                        break
                    }

                    performReloadAttempt()
                    waitSeconds = 5
                }
            } finally {
                autoRetryLoopRunning = false
            }
        }
    }

    LaunchedEffect(focusedChannel) {
        delay(250L)
        selectedChannel = focusedChannel
    }

    // Fetch EPG data for selected channel
    LaunchedEffect(selectedChannel) {
        if (selectedChannel != null) {
            isEpgLoading = true
            epgError = false
            val epgURL = "$basefinURL/epg/${selectedChannel!!.channel_id}/0"
            Log.d("EPG_FETCH", epgURL)

            try {
                val fetchedEpg = ChannelUtils.fetchEpg(epgURL)
                if (fetchedEpg != null) {
                    epgData = fetchedEpg
                } else {
                    epgData = null
                    epgError = true
                }
            } catch (_: Exception) {
                epgData = null
                epgError = true
            } finally {
                isEpgLoading = false
            }
        } else {
            epgData = null
            epgError = false
            isEpgLoading = false
        }
    }

    // UI: Startup readiness, recovery and content.
    val currentStartupOutcome = startupOutcome
    if (currentStartupOutcome is TvStartupOutcome.Timeout || currentStartupOutcome is TvStartupOutcome.Failure) {
        val reason = if (currentStartupOutcome is TvStartupOutcome.Timeout) {
            currentStartupOutcome.reason
        } else {
            (currentStartupOutcome as TvStartupOutcome.Failure).reason
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (currentStartupOutcome is TvStartupOutcome.Timeout) {
                        "Startup timed out"
                    } else {
                        "Startup failed"
                    },
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = reason,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))
                ElevatedCard(
                    onClick = {
                        startupOutcome = TvStartupOutcome.Checking
                        showLoading = true
                        startupRetryToken++
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry startup"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry startup")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ElevatedCard(
                    onClick = {
                        startupOutcome = TvStartupOutcome.Idle
                        showLoading = !fetched
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Open channel list"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open channel list")
                    }
                }
            }
        }
    } else if (showLoading || currentStartupOutcome is TvStartupOutcome.Checking) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ContainedLoadingIndicator(modifier = Modifier.size(100.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (currentStartupOutcome is TvStartupOutcome.Checking) {
                        "Waiting for server readiness..."
                    } else {
                        "Loading channels..."
                    },
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }
        }
    } else if (fetched && filteredChannels.value.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No channels found",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = waitingDots, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Auto-retrying channel load in ${autoLoadCountdown}s...",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = Color.Blue
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Click Reload to retry now, or wait for auto-retry",
                    style = TextStyle(fontSize = 14.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Retry attempts: $reloadAttemptCount",
                    style = TextStyle(fontSize = 13.sp, color = Color.Gray)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Check your internet connection\n• Go to Main Screen for detailed information",
                    style = TextStyle(fontSize = 13.sp, color = Color.Gray)
                )
                Spacer(modifier = Modifier.height(24.dp))
                ElevatedCard(
                    onClick = {
                        coroutineScope.launch {
                            performReloadAttempt()
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reload"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reload App")
                    }
                }
            }
        }
    } else {
        // CATEGORY CHIPS WITH FREE ONLY FILTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sortedCategories) { categoryName ->
                    val categoryId = categoryMap[categoryName]
                    val isSelected = categoryId != null && selectedCategoryIds.contains(categoryId)
                    var isFocused by remember { mutableStateOf(false) }
                    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                    val chipBorderColor = if (isDark) Color(0xFF00E5FF) else Color(0xFFFFD700)

                    FilterChip(
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(2.dp, if (isFocused) chipBorderColor else Color.Transparent, RoundedCornerShape(8.dp)),
                        onClick = {
                            if (categoryName == "All") {
                                selectedCategoryIds = emptySet()
                            } else if (categoryId != null) {
                                selectedCategoryIds = if (isSelected) {
                                    selectedCategoryIds - categoryId
                                } else {
                                    selectedCategoryIds + categoryId
                                }
                            }
                            val updatedCI = selectedCategoryIds.joinToString(",")
                            preferenceManager.myPrefs.filterCI = updatedCI
                            preferenceManager.savePreferences()
                        },
                        label = { Text(categoryName) },
                        selected = if (categoryName == "All") {
                            selectedCategoryIds.isEmpty()
                        } else {
                            isSelected
                        },
                        leadingIcon = when {
                            categoryName == "All" && selectedCategoryIds.isEmpty() -> {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "All selected icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            }

                            isSelected -> {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Done icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            }

                            else -> null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox "Free only"
            var freeOnlyFocused by remember { mutableStateOf(false) }
            val isDark = MaterialTheme.colorScheme.background.red < 0.5f
            val focusBorderColor = if (isDark) Color(0xFF00E5FF) else Color(0xFFFFD700)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .onFocusChanged { freeOnlyFocused = it.isFocused }
                    .border(2.dp, if (freeOnlyFocused) focusBorderColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable {
                        freeOnly = !freeOnly
                        preferenceManager.myPrefs.freeOnly = freeOnly
                        preferenceManager.savePreferences()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Checkbox(
                    checked = freeOnly,
                    onCheckedChange = { checked ->
                        freeOnly = checked
                        preferenceManager.myPrefs.freeOnly = checked
                        preferenceManager.savePreferences()
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Free only",
                    style = TextStyle(fontSize = 14.sp, color = Color.White)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox "Catchup"
            var catchupFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .onFocusChanged { catchupFocused = it.isFocused }
                    .border(2.dp, if (catchupFocused) focusBorderColor else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable {
                        freeJioCatchup = !freeJioCatchup
                        preferenceManager.myPrefs.freeJioCatchup = freeJioCatchup
                        preferenceManager.savePreferences()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Checkbox(
                    checked = freeJioCatchup,
                    onCheckedChange = { checked ->
                        freeJioCatchup = checked
                        preferenceManager.myPrefs.freeJioCatchup = checked
                        preferenceManager.savePreferences()
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Catchup",
                    style = TextStyle(fontSize = 14.sp, color = Color.White)
                )
            }
        }

        if (showSecondLanguageAddonSelector) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(secondLanguageAddonCategoryIds) { categoryId ->
                    val label = secondLanguageAddonLabel(categoryId)
                    val isSelected = selectedSecondLanguageAddonCategoryIds.contains(categoryId)
                    var isFocused by remember { mutableStateOf(false) }
                    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                    val chipBorderColor = if (isDark) Color(0xFF00E5FF) else Color(0xFFFFD700)

                    FilterChip(
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(2.dp, if (isFocused) chipBorderColor else Color.Transparent, RoundedCornerShape(8.dp)),
                        onClick = {
                            selectedSecondLanguageAddonCategoryIds = if (isSelected) {
                                selectedSecondLanguageAddonCategoryIds - categoryId
                            } else {
                                selectedSecondLanguageAddonCategoryIds + categoryId
                            }
                            preferenceManager.myPrefs.filterCI2 =
                                selectedSecondLanguageAddonCategoryIds.joinToString(",")
                            preferenceManager.savePreferences()
                        },
                        label = { Text(label) },
                        selected = isSelected,
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Done icon",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        // EPG CARD (null-safe)
        if (isEpgLoading || epgData != null || epgError) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Loading EPG...",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                )
                            }
                        }

                        epgError -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No EPG available",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                )
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

        OmniTvChannelGridMain(
            context = context,
            filteredChannels = filteredChannels.value,
            selectedChannelSetter = { focusedChannel = it },
            localPORT = localPORT,
            preferenceManager = preferenceManager,
            onCatchupClick = { channel ->
                catchupChannelTarget = channel
            }
        )

        catchupChannelTarget?.let { channel ->
            CatchupOverlay(
                channel = channel,
                localPORT = localPORT,
                onClose = { catchupChannelTarget = null },
                context = context,
                preferenceManager = preferenceManager
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CatchupOverlay(
    channel: Channel,
    localPORT: Int,
    onClose: () -> Unit,
    context: Context,
    preferenceManager: SkySharedPref
) {
    var selectedOffset by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var epgList by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    BackHandler { onClose() }

    // Fetch EPG whenever the selected day offset changes
    LaunchedEffect(selectedOffset, channel.channel_id) {
        loading = true
        errorMsg = null
        try {
            withContext(Dispatchers.IO) {
                val urlString = "http://localhost:$localPORT/epg/${channel.channel_id}/$selectedOffset"
                val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val response = Gson().fromJson(json, EpgResponse::class.java)
                
                val currentTime = System.currentTimeMillis()
                
                // Parse program epochs (make sure they are in milliseconds)
                val parsedEpg = response.epg.map { program ->
                    val start = if (program.startEpoch < 100000000000L) program.startEpoch * 1000 else program.startEpoch
                    val end = if (program.endEpoch < 100000000000L) program.endEpoch * 1000 else program.endEpoch
                    program.copy(startEpoch = start, endEpoch = end)
                }

                // Filter out future shows (only keep shows that have already started or are currently live)
                val pastAndLiveShows = parsedEpg.filter { it.startEpoch <= currentTime }

                // Reorder: if offset is 0 (Today), find the live one and place it first
                val finalEpg = if (selectedOffset == 0) {
                    val liveShow = pastAndLiveShows.find { currentTime >= it.startEpoch && currentTime <= it.endEpoch }
                    if (liveShow != null) {
                        // Put live show at the beginning, followed by other past shows (newest first, which is typically reverse chronological)
                        val otherShows = pastAndLiveShows.filter { it.srno != liveShow.srno }.reversed()
                        listOf(liveShow) + otherShows
                    } else {
                        pastAndLiveShows.reversed()
                    }
                } else {
                    pastAndLiveShows.reversed()
                }

                withContext(Dispatchers.Main) {
                    epgList = finalEpg
                    loading = false
                }
            }
        } catch (e: Exception) {
            Log.e("CatchupOverlay", "Error fetching catchup EPG", e)
            withContext(Dispatchers.Main) {
                errorMsg = "Failed to load catchup guide"
                loading = false
            }
        }
    }

    // Prepare day selectors (Today = 0, Yesterday = -1, ..., 7 days ago = -7)
    val dayOffsets = (0 downTo -7).toList()
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val todayCal = Calendar.getInstance()

    val isTv = LocalConfiguration.current.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Sleek dark mode background
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlideImage(
                    model = if (channel.logoUrl.startsWith("http")) channel.logoUrl else "http://localhost:$localPORT/jtvimage/${channel.logoUrl}",
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${channel.channel_name} - Catchup Guide",
                    style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
            }

            // Horizontal Day Selector (Chips)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayOffsets) { offset ->
                    val cal = todayCal.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, offset)
                    val label = if (offset == 0) "Today" else if (offset == -1) "Yesterday" else dateFormat.format(cal.time)
                    val isSelected = offset == selectedOffset
                    
                    var isFocused by remember { mutableStateOf(false) }
                    val chipBorderColor = Color(0xFF00E5FF)

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedOffset = offset },
                        label = { Text(label, color = if (isSelected) Color.Black else Color.White) },
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(2.dp, if (isFocused) chipBorderColor else Color.Transparent, RoundedCornerShape(8.dp)),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF),
                            containerColor = Color(0xFF262626)
                        )
                    )
                }
            }

            // Guide Content
            when {
                loading -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
                errorMsg != null -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = Color.Red, fontSize = 16.sp)
                }
                epgList.isEmpty() -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No shows available for this day", color = Color.Gray, fontSize = 16.sp)
                }
                else -> {
                    // Vertical Grid of Catchup Shows
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isTv) 320.dp else 260.dp),
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(epgList) { index, program ->
                            val currentTime = System.currentTimeMillis()
                            val isLive = currentTime >= program.startEpoch && currentTime <= program.endEpoch
                            
                            CatchupTile(
                                program = program,
                                isLive = isLive,
                                isTv = isTv,
                                localPORT = localPORT,
                                onClick = {
                                    val videoUrl = if (isLive) {
                                        channel.channel_url
                                    } else {
                                        "http://localhost:$localPORT/catchup/render/${channel.channel_id}?start=${program.startEpoch}&end=${program.endEpoch}&srno=${program.srno}"
                                    }
                                    
                                    val intent = Intent(context, ExoPlayJet::class.java).apply {
                                        putExtra("video_url", videoUrl)
                                        putExtra("zone", "TV")
                                        if (channel.channel_id.all { it.isDigit() }) putExtra("channel_list_kind", "jio")
                                        putExtra("current_channel_index", -1)
                                        putExtra("logo_url", if (channel.logoUrl.startsWith("http")) channel.logoUrl else "http://localhost:$localPORT/jtvimage/${channel.logoUrl}")
                                        putExtra("ch_name", if (isLive) "[LIVE] ${program.showname}" else program.showname)
                                        
                                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CatchupTile(
    program: EpgProgram,
    isLive: Boolean,
    isTv: Boolean,
    localPORT: Int,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val primaryColor = Color(0xFF00E5FF)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(2.dp, if (focused) primaryColor else Color.Transparent, shape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (focused) Color(0xFF1F353D) else Color(0xFF1E1E1E)
        ),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Poster/Thumbnail
            val posterUrl = "http://localhost:$localPORT/jtvposter/${program.episodePoster}"
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                GlideImage(
                    model = posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isLive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(Color.Red, RoundedCornerShape(bottomEnd = 4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = program.showname,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${program.showtime} - ${program.endtime}",
                    color = if (isLive) primaryColor else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = program.description,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
