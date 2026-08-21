package com.skylake.skytv.jgorunner.ui.screens

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.skylake.skytv.jgorunner.activities.OmniPlayerActivity
import com.skylake.skytv.jgorunner.activities.WebPlayerActivity
import com.skylake.skytv.jgorunner.data.OmniRepository
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.data.OmniFavoritesStore
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import com.skylake.skytv.jgorunner.ui.tvhome.EpgProgram
import com.skylake.skytv.jgorunner.ui.tvhome.EpgResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import com.skylake.skytv.jgorunner.services.BinaryService
import java.io.File
import com.skylake.skytv.jgorunner.utils.LogCollector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val FAVORITES_SERVER_URL = "favorite://omni"

data class OmniServer(
    val name: String,
    val url: String,
    val isFavoriteServer: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniMainScreen(context: Context, onNavigate: (String) -> Unit) {
    val prefManager = remember { SkySharedPref.getInstance(context) }
    val repository = remember { OmniRepository(context) }
    val port = prefManager.myPrefs.jtvGoServerPort
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }

    val favoriteServer = remember {
        OmniServer(
            name = "Favorites",
            url = FAVORITES_SERVER_URL,
            isFavoriteServer = true
        )
    }
    val freeJioServer = remember(port) {
        OmniServer(
            name = "Jio",
            url = "http://127.0.0.1:$port",
            isFavoriteServer = false
        )
    }
    val availableServers = remember(favoriteServer, freeJioServer) {
        listOf(favoriteServer, freeJioServer)
    }
    val autoOpenPref = prefManager.myPrefs.omniAutoOpenServer
    val initialServer = remember(autoOpenPref, favoriteServer, freeJioServer) {
        if (autoOpenPref == FAVORITES_SERVER_URL || autoOpenPref?.equals("Favorites", ignoreCase = true) == true) {
            favoriteServer
        } else {
            freeJioServer
        }
    }
    var currentServer by remember { mutableStateOf(initialServer) }
    var fullChannelList by remember { mutableStateOf<List<OmniChannel>>(emptyList()) }

    var channels by remember { mutableStateOf<List<OmniChannel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isSidebarVisible by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    var selectedCategories by remember {
        val json = prefManager.myPrefs.omniSelectedCategories ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
        mutableStateOf<Set<String>>(gson.fromJson(json, type) ?: emptySet())
    }
    var selectedLanguages by remember {
        val json = prefManager.myPrefs.omniSelectedLanguages ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
        mutableStateOf<Set<String>>(gson.fromJson(json, type) ?: emptySet())
    }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAutoOpenServerDialog by remember { mutableStateOf(false) }
    var autoOpenServerName by remember {
        mutableStateOf(if (prefManager.myPrefs.omniAutoOpenServer == FAVORITES_SERVER_URL || prefManager.myPrefs.omniAutoOpenServer?.equals("Favorites", ignoreCase = true) == true) "Favorites" else "Jio")
    }
    var showDefaultUiDialog by remember { mutableStateOf(false) }
    var defaultUiLabel by remember {
        mutableStateOf(
            omniDefaultUiLabel(
                prefManager.myPrefs.iptvAppPackageName,
                prefManager.myPrefs.iptvAppName
            )
        )
    }
    var hasAutoplayed by remember { mutableStateOf(false) }

    var freeOnly by remember { mutableStateOf(prefManager.myPrefs.freeOnly) }
    var freeJioCatchup by remember { mutableStateOf(prefManager.myPrefs.freeJioCatchup) }
    var catchupChannelTarget by remember { mutableStateOf<OmniChannel?>(null) }
    var isDarkMode by remember { mutableStateOf(prefManager.myPrefs.darkMODE) }

    var showImportDialog by remember { mutableStateOf(false) }
    var settingsUpdateTrigger by remember { mutableIntStateOf(0) }
    var showLogDialog by remember { mutableStateOf(false) }
    var favoriteRefreshTick by remember { mutableIntStateOf(0) }
    val favoritesStore = remember { OmniFavoritesStore(prefManager) }

    LaunchedEffect(currentServer) {
        hasAutoplayed = false
    }

    LaunchedEffect(isSidebarVisible) {
        if (isSidebarVisible) {
            favoriteRefreshTick++
        }
    }

    val searchFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    val firstSidebarFocusRequester = remember { FocusRequester() }

    val hasCategories = remember(channels) {
        channels.any { !it.group.isNullOrBlank() }
    }
    val hasLanguages = remember(channels) {
        channels.any { !it.language.isNullOrBlank() }
    }

    fun filterChannelList(sourceList: List<OmniChannel>): List<OmniChannel> {
        return sourceList.filter { channel ->
            val matchesSearch = searchQuery.isEmpty() ||
                channel.name?.contains(searchQuery, ignoreCase = true) == true ||
                channel.group?.contains(searchQuery, ignoreCase = true) == true

            val matchesCategory = selectedCategories.isEmpty() || selectedCategories.any { filter ->
                channel.group?.contains(filter, ignoreCase = true) == true ||
                filter.contains(channel.group.orEmpty(), ignoreCase = true)
            }

            val matchesLanguage = selectedLanguages.isEmpty() || selectedLanguages.any { filter ->
                channel.language?.contains(filter, ignoreCase = true) == true ||
                filter.contains(channel.language.orEmpty(), ignoreCase = true)
            }

            val matchesFreeOnly = !freeOnly || !channel.requiresSubscription

            matchesSearch && matchesCategory && matchesLanguage && matchesFreeOnly
        }
    }

    val filteredChannels = remember(channels, searchQuery, selectedCategories, selectedLanguages, freeOnly) {
        filterChannelList(channels)
    }

    fun triggerAutoplay(rawChannelList: List<OmniChannel>) {
        val channelList = filterChannelList(rawChannelList)
        val autoFirst = prefManager.myPrefs.omniAutoplayFirstChannel
        val autoLast = prefManager.myPrefs.omniAutoplayLastChannel
        val lastPlayedUrl = prefManager.myPrefs.currChannelUrl?.trim().orEmpty()
        val lastPlayedName = prefManager.myPrefs.currChannelName?.trim().orEmpty()

        com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Checking Autoplay -> autoFirst: $autoFirst, autoLast: $autoLast, hasAutoplayed: $hasAutoplayed, filtered channelList size: ${channelList.size}, lastChannel: '$lastPlayedName'")

        if (hasAutoplayed || channelList.isEmpty()) return
        if (!autoFirst && !autoLast) return

        val targetChannel = if (autoLast && (lastPlayedUrl.isNotBlank() || lastPlayedName.isNotBlank())) {
            channelList.find { ch ->
                (lastPlayedName.isNotBlank() && ch.name?.trim().equals(lastPlayedName, ignoreCase = true)) ||
                (lastPlayedUrl.isNotBlank() && (
                    ch.url?.equals(lastPlayedUrl, ignoreCase = true) == true ||
                    ch.m3u8Url?.equals(lastPlayedUrl, ignoreCase = true) == true ||
                    ch.mpdUrl?.equals(lastPlayedUrl, ignoreCase = true) == true ||
                    (ch.id != null && lastPlayedUrl.contains(ch.id!!))
                ))
            } ?: if (autoFirst) channelList.firstOrNull() else null
        } else if (autoFirst) {
            channelList.firstOrNull()
        } else null

        if (targetChannel != null) {
            hasAutoplayed = true
            val targetIndex = channelList.indexOf(targetChannel).coerceAtLeast(0)
            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Autoplaying channel '${targetChannel.name}' on server '${currentServer.name}' (index: $targetIndex in ${channelList.size} filtered channels)")
            com.skylake.skytv.jgorunner.data.OmniDataManager.currentChannelList = channelList
            try {
                com.skylake.skytv.jgorunner.services.player.PlayerCommandBus.requestClosePip()
            } catch (_: Exception) {}
            val intent = Intent(context, OmniPlayerActivity::class.java).apply {
                putExtra("channel_index", targetIndex)
            }
            context.startActivity(intent)
        } else {
            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Autoplay found no matching target channel in ${channelList.size} filtered channels")
        }
    }

    // Load channels on server change or favorites refresh
    LaunchedEffect(currentServer, favoriteRefreshTick) {
        if (currentServer.url == FAVORITES_SERVER_URL) {
            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Loading Favorites server channels")
            if (fullChannelList.isEmpty()) {
                try {
                    fullChannelList = withContext(Dispatchers.IO) { repository.fetchChannels(port) }
                } catch (_: Exception) {}
            }
            val favs = favoritesStore.load()
            if (fullChannelList.isNotEmpty()) {
                val favMap = favs.associateBy { it.name }
                channels = fullChannelList.filter { favMap.containsKey(it.name) }
            } else {
                channels = favs.map { fav ->
                    OmniChannel(
                        id = fav.id,
                        name = fav.name,
                        group = "Favorites",
                        language = "Hindi",
                        logo = null,
                        url = fav.url,
                        m3u8Url = fav.url,
                        mpdUrl = null,
                        licenseUrl = null,
                        requiresSubscription = false
                    )
                }
            }
            isLoading = false
            errorMessage = null
            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Loaded ${channels.size} favorite channels")
            triggerAutoplay(channels)
        } else {
            isLoading = true
            errorMessage = null
            try {
                com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Fetching channels from server: ${currentServer.name} (Port: $port)")
                val fetched = withContext(Dispatchers.IO) { repository.fetchChannels(port) }
                fullChannelList = fetched
                channels = fetched
                if (fetched.isEmpty()) {
                    errorMessage = "No channels found."
                    com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Channels fetch returned empty list.")
                } else {
                    com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Successfully loaded ${fetched.size} channels from ${currentServer.name}")
                    triggerAutoplay(fetched)
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load channels."
                com.skylake.skytv.jgorunner.utils.LogCollector.logError("Omni: Failed to load channels from server", e)
            }
            isLoading = false
        }
    }

    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(filteredChannels, isSidebarVisible, isSearchVisible) {
        if (filteredChannels.isNotEmpty() && !isSidebarVisible && !isSearchVisible) {
            delay(300)
            try { firstChannelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    BackHandler {
        when {
            isSearchVisible -> isSearchVisible = false
            isSidebarVisible -> isSidebarVisible = false
            else -> {
                com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Back pressed -> navigating to Home")
                onNavigate("Home")
            }
        }
    }

    val isTv = com.skylake.skytv.jgorunner.utils.DeviceUtils.isTvDevice(context)
    val drawerWidth = if (isTv) 300.dp else 244.dp

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1C1B1F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // --- SIDEBAR DRAWER ---
        AnimatedVisibility(
            visible = isSidebarVisible,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(2f),
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                    )
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Channel Settings",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Omni UI Settings",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                    var isRefreshFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            if (!isLoading) {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Force refreshing channels from server...")
                                        repository.clearCache()
                                        val fetched = withContext(Dispatchers.IO) { repository.fetchChannels(port, forceRefresh = true) }
                                        fullChannelList = fetched
                                        channels = fetched
                                        errorMessage = null
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage
                                        com.skylake.skytv.jgorunner.utils.LogCollector.logError("Omni: Refresh failed", e)
                                    }
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .onFocusChanged { isRefreshFocused = it.isFocused }
                            .background(if (isRefreshFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(10.dp))
                            .border(1.dp, if (isRefreshFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.Default.Refresh, "Refresh",
                            tint = if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    var isCloseFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { isSidebarVisible = false },
                        modifier = Modifier
                            .size(38.dp)
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .background(if (isCloseFocused) MaterialTheme.colorScheme.error.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(10.dp))
                            .border(1.dp, if (isCloseFocused) MaterialTheme.colorScheme.error else Color.Transparent, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    // ---- SERVERS ----
                    item { OmniDrawerSectionLabel("SERVERS") }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            availableServers.forEach { server ->
                                OmniServerListItem(
                                    server = server,
                                    isSelected = server.url == currentServer.url,
                                    onSelected = {
                                        if (currentServer.url != server.url) {
                                            currentServer = server
                                            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Switched server to: ${server.name} (${server.url})")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // ---- FILTERS ----
                    item { OmniDrawerSectionLabel("FILTERS") }
                    item {
                        OmniSettingsActionItem("Category Filter", Icons.Default.FilterList, enabled = hasCategories) { showCategoryDialog = true }
                    }
                    item {
                        OmniSettingsActionItem("Language Filter", Icons.Default.Language, enabled = hasLanguages) { showLanguageDialog = true }
                    }
                    item {
                        OmniSettingsActionItem("Clear Filters", Icons.Default.FilterAltOff, enabled = true) {
                            selectedCategories = emptySet()
                            selectedLanguages = emptySet()
                            prefManager.myPrefs.omniSelectedCategories = "[]"
                            prefManager.myPrefs.omniSelectedLanguages = "[]"
                            prefManager.savePreferences()
                            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Cleared filter selections")
                        }
                    }

                    // ---- PLAYBACK OPTIONS ----
                    item { OmniDrawerSectionLabel("PLAYBACK") }
                    item {
                        OmniSettingsActionItem("Auto Open: $autoOpenServerName", Icons.Default.Dns, enabled = true) {
                            showAutoOpenServerDialog = true
                        }
                    }
                    item {
                        var checked by remember(settingsUpdateTrigger) { mutableStateOf(prefManager.myPrefs.omniAutoplayFirstChannel) }
                        OmniSettingsToggle("Autoplay 1st CH", checked) {
                            checked = it
                            prefManager.myPrefs.omniAutoplayFirstChannel = it
                            prefManager.savePreferences()
                        }
                    }
                    item {
                        var checked by remember(settingsUpdateTrigger) { mutableStateOf(prefManager.myPrefs.omniAutoplayLastChannel) }
                        OmniSettingsToggle("Autoplay Last Played CH", checked) {
                            checked = it
                            prefManager.myPrefs.omniAutoplayLastChannel = it
                            prefManager.savePreferences()
                        }
                    }
                    item {
                        var pipChecked by remember(settingsUpdateTrigger) { mutableStateOf(prefManager.myPrefs.enablePip) }
                        OmniSettingsToggle("Enable PiP", pipChecked) {
                            pipChecked = it
                            prefManager.myPrefs.enablePip = it
                            prefManager.savePreferences()
                        }
                    }
                    item {
                        var swipeChecked by remember(settingsUpdateTrigger) { mutableStateOf(prefManager.myPrefs.omniEnableSwipeGestures) }
                        OmniSettingsToggle("Vol/Bright Gestures", swipeChecked) {
                            swipeChecked = it
                            prefManager.myPrefs.omniEnableSwipeGestures = it
                            prefManager.savePreferences()
                        }
                    }
                    item {
                        var doubleTapChecked by remember(settingsUpdateTrigger) { mutableStateOf(prefManager.myPrefs.omniEnableDoubleTapSeek) }
                        OmniSettingsToggle("Double-tap to Seek", doubleTapChecked) {
                            doubleTapChecked = it
                            prefManager.myPrefs.omniEnableDoubleTapSeek = it
                            prefManager.savePreferences()
                        }
                    }
                    item {
                        var animChecked by remember(settingsUpdateTrigger) { mutableStateOf(prefManager.myPrefs.omniAnimationEnabled) }
                        OmniSettingsToggle("Animations", animChecked) {
                            animChecked = it
                            prefManager.myPrefs.omniAnimationEnabled = it
                            prefManager.savePreferences()
                        }
                    }

                    // ---- SYSTEM ----
                    item { OmniDrawerSectionLabel("SYSTEM") }
                    item {
                        OmniSettingsToggle("Day / Night Mode", isDarkMode) { checked ->
                            isDarkMode = checked
                            prefManager.myPrefs.darkMODE = checked
                            prefManager.savePreferences()
                            (context as? com.skylake.skytv.jgorunner.activities.MainActivity)?.isSwitchDarkMode = checked
                        }
                    }
                    item {
                        var autobootChecked by remember(settingsUpdateTrigger) { mutableStateOf(prefManager.myPrefs.omniAutoStartAppOnBoot) }
                        OmniSettingsToggle("Autostart on Boot", autobootChecked) { checked ->
                            autobootChecked = checked
                            prefManager.myPrefs.omniAutoStartAppOnBoot = checked
                            prefManager.savePreferences()
                            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Autostart on Boot set to $checked")
                        }
                    }
                    item {
                        OmniSettingsActionItem("Default UI: $defaultUiLabel", Icons.Default.DisplaySettings, enabled = true) {
                            showDefaultUiDialog = true
                        }
                    }
                    item {
                        OmniSettingsActionItem("App Logs", Icons.Default.BugReport, enabled = true) {
                            showLogDialog = true
                        }
                    }
                    item {
                        OmniSettingsActionItem("Reset Channel Settings", Icons.Default.RestartAlt, enabled = true) {
                            prefManager.myPrefs.freeOnly = true
                            prefManager.myPrefs.freeJioCatchup = false
                            prefManager.myPrefs.omniAutoplayFirstChannel = false
                            prefManager.myPrefs.omniAutoplayLastChannel = false
                            prefManager.myPrefs.enablePip = false
                            prefManager.myPrefs.darkMODE = true
                            prefManager.myPrefs.omniEnableSwipeGestures = true
                            prefManager.myPrefs.omniEnableDoubleTapSeek = true
                            prefManager.myPrefs.omniAnimationEnabled = true
                            prefManager.myPrefs.omniSelectedCategories = "[]"
                            prefManager.myPrefs.omniSelectedLanguages = "[]"
                            prefManager.savePreferences()
                            repository.clearCache()

                            freeOnly = true
                            freeJioCatchup = false
                            isDarkMode = true
                            selectedCategories = emptySet()
                            selectedLanguages = emptySet()
                            settingsUpdateTrigger++

                            Toast.makeText(context, "Settings reset & cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        // Dim scrim behind drawer on phones
        if (!isTv && isSidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isSidebarVisible = false }
            )
        }

        // --- MAIN CHANNEL GRID ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (isTv && isSidebarVisible) drawerWidth else 0.dp)
        ) {
            // Top bar: search toggle / server name / menu icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                AnimatedContent(
                    targetState = isSearchVisible,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.9f))
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "search-bar",
                    modifier = Modifier.fillMaxWidth()
                ) { searchActive ->
                    if (searchActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var isBackFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    isSearchVisible = false
                                    searchQuery = ""
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .onFocusChanged { isBackFocused = it.isFocused }
                                    .background(if (isBackFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                                    .border(1.dp, if (isBackFocused) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .border(
                                        1.dp,
                                        if (isSearchFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search channels...",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            ),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { isSearchFocused = it.isFocused }
                                                .focusRequester(searchFocusRequester)
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        var isClearFocused by remember { mutableStateOf(false) }
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .onFocusChanged { isClearFocused = it.isFocused }
                                                .background(if (isClearFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                                                .border(1.dp, if (isClearFocused) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isSidebarVisible) {
                                var isMenuFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { isSidebarVisible = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .onFocusChanged { isMenuFocused = it.isFocused }
                                        .background(if (isMenuFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isMenuFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Menu, "Expand", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(
                                text = "Jio",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            /*
                            var isImportFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showImportDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .onFocusChanged { isImportFocused = it.isFocused }
                                    .background(if (isImportFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                                    .border(1.dp, if (isImportFocused) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Import Credentials",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            */



                            Spacer(modifier = Modifier.weight(1f))

                            var isSearchIconFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { isSearchVisible = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .onFocusChanged { isSearchIconFocused = it.isFocused }
                                    .background(if (isSearchIconFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                                    .border(1.dp, if (isSearchIconFocused) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Category/Language filter pills and Free/Catchup checkboxes below the title bar
            if (channels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OmniFilterPill(
                        label = if (selectedCategories.isEmpty()) "Category" else "Category (${selectedCategories.size})",
                        active = selectedCategories.isNotEmpty(),
                        onClick = { showCategoryDialog = true },
                        enabled = hasCategories
                    )
                    OmniFilterPill(
                        label = if (selectedLanguages.isEmpty()) "Language" else "Language (${selectedLanguages.size})",
                        active = selectedLanguages.isNotEmpty(),
                        onClick = { showLanguageDialog = true },
                        enabled = hasLanguages
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Compact Free checkbox
                    var freeFocused by remember { mutableStateOf(false) }
                    val focusBorderColor = MaterialTheme.colorScheme.primary
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .onFocusChanged { freeFocused = it.isFocused }
                            .border(
                                2.dp,
                                if (freeFocused) focusBorderColor else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                freeOnly = !freeOnly
                                prefManager.myPrefs.freeOnly = freeOnly
                                prefManager.savePreferences()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides 0.dp
                        ) {
                            Checkbox(
                                checked = freeOnly,
                                onCheckedChange = { checked ->
                                    freeOnly = checked
                                    prefManager.myPrefs.freeOnly = checked
                                    prefManager.savePreferences()
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Free",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Compact Catchup checkbox
                    var catchupFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .onFocusChanged { catchupFocused = it.isFocused }
                            .border(
                                2.dp,
                                if (catchupFocused) focusBorderColor else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                freeJioCatchup = !freeJioCatchup
                                prefManager.myPrefs.freeJioCatchup = freeJioCatchup
                                prefManager.savePreferences()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides 0.dp
                        ) {
                            Checkbox(
                                checked = freeJioCatchup,
                                onCheckedChange = { checked ->
                                    freeJioCatchup = checked
                                    prefManager.myPrefs.freeJioCatchup = checked
                                    prefManager.savePreferences()
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Catchup",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Channel count / status
            if (channels.isNotEmpty() && !isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredChannels.size} channels",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // --- CHANNEL CONTENT GRID ---
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                currentServer.url == FAVORITES_SERVER_URL && channels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No favorites yet",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Long-press any channel on Jio to add it to your favorites.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                errorMessage != null && channels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(16.dp))
                            var isRetryFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Retrying channel fetch from server: ${currentServer.name}")
                                            channels = withContext(Dispatchers.IO) { repository.fetchChannels(port) }
                                            errorMessage = null
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage
                                            com.skylake.skytv.jgorunner.utils.LogCollector.logError("Omni: Retry failed", e)
                                        }
                                        isLoading = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRetryFocused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .onFocusChanged { isRetryFocused = it.isFocused }
                                    .border(1.dp, if (isRetryFocused) MaterialTheme.colorScheme.primary else Color.Transparent, ButtonDefaults.shape)
                            ) {
                                Text("Retry", color = if (isRetryFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                filteredChannels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No channels match the current filter or search.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isTv) 112.dp else 100.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(filteredChannels) { index, channel ->
                            OmniChannelGridItem(
                                channel = channel,
                                modifier = if (index == 0) Modifier.focusRequester(firstChannelFocusRequester) else Modifier,
                                onSelected = {
                                    prefManager.myPrefs.currChannelName = channel.name
                                    prefManager.myPrefs.currChannelUrl = channel.url ?: channel.m3u8Url ?: channel.mpdUrl
                                    prefManager.savePreferences()
                                    if (freeJioCatchup) {
                                        catchupChannelTarget = channel
                                    } else {
                                        com.skylake.skytv.jgorunner.data.OmniDataManager.currentChannelList = filteredChannels
                                        if (com.skylake.skytv.jgorunner.services.player.PlayerCommandBus.isInPipMode) {
                                            try {
                                                com.skylake.skytv.jgorunner.services.player.PlayerCommandBus.requestClosePip()
                                            } catch (_: Exception) {}
                                        }
                                        val intent = Intent(context, OmniPlayerActivity::class.java).apply {
                                            putExtra("channel_index", index)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Catchup overlay — must be inside the same Box to actually overlay the grid
        catchupChannelTarget?.let { target ->
            OmniCatchupOverlay(
                channel = target,
                localPORT = port,
                onClose = { catchupChannelTarget = null },
                context = context,
                preferenceManager = prefManager,
                onPlayChannel = { resolvedChannel ->
                    com.skylake.skytv.jgorunner.data.OmniDataManager.currentChannelList = listOf(resolvedChannel)
                    if (com.skylake.skytv.jgorunner.services.player.PlayerCommandBus.isInPipMode) {
                        try {
                            com.skylake.skytv.jgorunner.services.player.PlayerCommandBus.requestClosePip()
                        } catch (_: Exception) {}
                    }
                    val intent = Intent(context, OmniPlayerActivity::class.java).apply {
                        putExtra("channel_index", 0)
                    }
                    context.startActivity(intent)
                },
                filteredChannels = filteredChannels
            )
        }
    }

    // Category Filter Dialog
    if (showCategoryDialog) {
        val categoriesList = remember(channels) {
            channels.mapNotNull { it.group }.distinct().sorted()
        }
        MultiSelectFilterDialog(
            title = "Categories",
            options = categoriesList,
            selectedOptions = selectedCategories,
            onDismiss = { showCategoryDialog = false },
            onConfirm = {
                selectedCategories = it
                prefManager.myPrefs.omniSelectedCategories = gson.toJson(it)
                prefManager.savePreferences()
                showCategoryDialog = false
            }
        )
    }

    // Language Filter Dialog
    if (showLanguageDialog) {
        val defaultLangs = listOf("Hindi", "English", "Tamil", "Telugu", "Malayalam", "Kannada", "Bengali", "Marathi", "Gujarati", "Punjabi", "Urdu", "Odia", "Assamese")
        val availableLangs = remember(channels) {
            val detected = channels.flatMap { it.language?.split(",")?.map { l -> l.trim() } ?: emptyList() }
            (detected + defaultLangs).filter { it.isNotEmpty() }.distinct().sorted()
        }
        MultiSelectFilterDialog(
            title = "Languages",
            options = availableLangs,
            selectedOptions = selectedLanguages,
            onDismiss = { showLanguageDialog = false },
            onConfirm = {
                selectedLanguages = it
                prefManager.myPrefs.omniSelectedLanguages = gson.toJson(it)
                prefManager.savePreferences()
                showLanguageDialog = false
            }
        )
    }

    if (showAutoOpenServerDialog) {
        AutoOpenServerDialog(
            currentAutoOpen = autoOpenServerName,
            onDismiss = { showAutoOpenServerDialog = false },
            onSelect = { selectedServer ->
                autoOpenServerName = selectedServer
                prefManager.myPrefs.omniAutoOpenServer = selectedServer
                prefManager.savePreferences()
                settingsUpdateTrigger++
                com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Auto Open Server set to $selectedServer")
                showAutoOpenServerDialog = false
            }
        )
    }

    if (showDefaultUiDialog) {
        OmniDefaultUiDialog(
            currentPackage = prefManager.myPrefs.iptvAppPackageName,
            onDismiss = { showDefaultUiDialog = false },
            onSelect = { pkg, label ->
                // Same three preferences AppListActivity writes, so the picker in Settings
                // and this one stay interchangeable.
                prefManager.myPrefs.iptvAppPackageName = pkg
                prefManager.myPrefs.iptvAppName = label
                prefManager.myPrefs.iptvAppLaunchActivity = ""
                prefManager.savePreferences()

                defaultUiLabel = omniDefaultUiLabel(pkg, label)
                settingsUpdateTrigger++
                com.skylake.skytv.jgorunner.utils.LogCollector.log("Omni: Default UI set to $label ($pkg)")
                Toast.makeText(context, "Default UI: $label - applies on next app start", Toast.LENGTH_SHORT).show()
                showDefaultUiDialog = false
            }
        )
    }

    if (showLogDialog) {
        LogViewerDialog(
            onDismiss = { showLogDialog = false },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("App Logs", LogCollector.getLogs())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onClear = {
                LogCollector.clear()
                Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
            }
        )
    }


    if (showImportDialog) {
        OmniImportCredentialsDialog(
            context = context,
            onDismiss = { showImportDialog = false }
        )
    }


}

// ──────────────────────────────────────────────────────────────────────────────
// Compact channel grid item
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OmniChannelGridItem(
    channel: OmniChannel,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1.0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .border(2.dp, if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onSelected() }
            .padding(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    contentScale = ContentScale.Fit
                )
                if (channel.name?.contains("HD", ignoreCase = true) == true) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                    ) {
                        Text(
                            "HD",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = channel.name ?: "Unknown Channel",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Omni filter dropdown pill
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OmniFilterPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .height(28.dp)
            .then(if (enabled) Modifier.onFocusChanged { focused = it.isFocused } else Modifier)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (active || focused) primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (active || focused) 2.dp else 1.dp,
            if (active || focused) primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                color = contentColor
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Drawer section label
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OmniDrawerSectionLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.weight(1f))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.weight(1f))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Settings toggle row (Switch)
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OmniSettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (isFocused) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.scale(0.7f),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Settings action item (clickable button with icon)
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OmniSettingsActionItem(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
            .then(if (enabled) Modifier.onFocusChanged { isFocused = it.isFocused } else Modifier)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled && isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (enabled && isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            color = if (isFocused) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Multi select filter dialog (mirrors OmniFilterDialog exactly)
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun MultiSelectFilterDialog(
    title: String,
    options: List<String>,
    selectedOptions: Set<String>,
    singleSelect: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    onReset: (() -> Unit)? = null
) {
    var currentSelection by remember { mutableStateOf(selectedOptions) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222222),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Cyan,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(options) { option ->
                        FilterItemRow(
                            label = option,
                            isSelected = currentSelection.contains(option),
                            onToggle = { selected ->
                                if (singleSelect) {
                                    if (selected) {
                                        onConfirm(setOf(option))
                                    } else {
                                        onConfirm(emptySet())
                                    }
                                } else {
                                    currentSelection = if (selected) {
                                        currentSelection + option
                                    } else {
                                        currentSelection - option
                                    }
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onReset != null) {
                        var isResetFocused by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = onReset,
                            modifier = Modifier
                                .onFocusChanged { isResetFocused = it.isFocused }
                                .background(if (isResetFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                                .border(1.dp, if (isResetFocused) Color.Red else Color.Transparent, RoundedCornerShape(4.dp))
                        ) {
                            Text("Reset", color = Color.Red)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    var isCancelFocused by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .onFocusChanged { isCancelFocused = it.isFocused }
                            .background(if (isCancelFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                            .border(1.dp, if (isCancelFocused) Color.Cyan else Color.Transparent, RoundedCornerShape(4.dp))
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                    if (!singleSelect) {
                        Spacer(modifier = Modifier.width(16.dp))
                        var isApplyFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = { onConfirm(currentSelection) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isApplyFocused) Color.White else Color.Cyan),
                            modifier = Modifier
                                .onFocusChanged { isApplyFocused = it.isFocused }
                                .border(1.dp, if (isApplyFocused) Color.Cyan else Color.Transparent, ButtonDefaults.shape)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterItemRow(
    label: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused) Color.Cyan.copy(alpha = 0.1f) else Color.Transparent)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, if (isFocused) Color.Cyan else Color.Transparent, RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onToggle(!isSelected) }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.Cyan,
                uncheckedColor = if (isFocused) Color.White else Color.Gray
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isFocused) Color.Cyan else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Catchup overlay
// ──────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniCatchupOverlay(
    channel: OmniChannel,
    localPORT: Int,
    onClose: () -> Unit,
    context: Context,
    preferenceManager: SkySharedPref,
    onPlayChannel: (OmniChannel) -> Unit,
    filteredChannels: List<OmniChannel>
) {
    var selectedOffset by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var epgList by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var resolvingProgramSrno by remember { mutableStateOf<Long?>(null) }

    val coroutineScope = rememberCoroutineScope()

    BackHandler { onClose() }

    LaunchedEffect(selectedOffset, channel.id) {
        loading = true
        errorMsg = null
        try {
            withContext(Dispatchers.IO) {
                val channelId = channel.id ?: ""
                val urlString = "http://127.0.0.1:$localPORT/epg/$channelId/$selectedOffset"
                val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val response = Gson().fromJson(json, EpgResponse::class.java)

                val currentTime = System.currentTimeMillis()
                val parsedEpg = response.epg.map { program ->
                    val start = if (program.startEpoch < 100000000000L) program.startEpoch * 1000 else program.startEpoch
                    val end = if (program.endEpoch < 100000000000L) program.endEpoch * 1000 else program.endEpoch
                    program.copy(startEpoch = start, endEpoch = end)
                }
                val pastAndLive = parsedEpg.filter { it.startEpoch <= currentTime }
                var finalEpg = if (selectedOffset == 0) {
                    val liveShow = pastAndLive.find { currentTime >= it.startEpoch && currentTime <= it.endEpoch }
                    if (liveShow != null) {
                        val otherShows = pastAndLive.filter { it.srno != liveShow.srno }.reversed()
                        listOf(liveShow) + otherShows
                    } else {
                        val liveTvProgram = EpgProgram(
                            srno = -1L,
                            showId = "live_fallback",
                            showtime = "LIVE",
                            showname = "LIVE TV",
                            description = "Watch Live Stream",
                            duration = 0,
                            endtime = "",
                            channel_name = channel.name ?: "",
                            episodeThumbnail = "",
                            episodePoster = "",
                            startEpoch = System.currentTimeMillis() - 1000,
                            endEpoch = System.currentTimeMillis() + 3600 * 1000
                        )
                        listOf(liveTvProgram) + pastAndLive.reversed()
                    }
                } else {
                    pastAndLive.reversed()
                }

                if (finalEpg.isEmpty()) {
                    finalEpg = listOf(
                        EpgProgram(
                            srno = -1L,
                            showId = "live_fallback",
                            showtime = "LIVE",
                            showname = "LIVE TV",
                            description = "Watch Live Stream",
                            duration = 0,
                            endtime = "",
                            channel_name = channel.name ?: "",
                            episodeThumbnail = "",
                            episodePoster = "",
                            startEpoch = System.currentTimeMillis() - 1000,
                            endEpoch = System.currentTimeMillis() + 3600 * 1000
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    epgList = finalEpg
                    loading = false
                }
            }
        } catch (e: Exception) {
            Log.e("OmniCatchup", "Error fetching EPG", e)
            val fallbackLive = EpgProgram(
                srno = -1L,
                showId = "live_fallback",
                showtime = "LIVE",
                showname = "LIVE TV",
                description = "Watch Live Stream (EPG unavailable)",
                duration = 0,
                endtime = "",
                channel_name = channel.name ?: "",
                episodeThumbnail = "",
                episodePoster = "",
                startEpoch = System.currentTimeMillis() - 1000,
                endEpoch = System.currentTimeMillis() + 3600 * 1000
            )
            withContext(Dispatchers.Main) {
                epgList = listOf(fallbackLive)
                errorMsg = null
                loading = false
            }
        }
    }

    val dayOffsets = (0 downTo -7).toList()
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val todayCal = Calendar.getInstance()
    val isTv = LocalConfiguration.current.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .zIndex(100f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                var isBackFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .onFocusChanged { isBackFocused = it.isFocused }
                        .background(if (isBackFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                        .border(1.dp, if (isBackFocused) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = channel.logo ?: "",
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name ?: "",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Catchup Guide",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(dayOffsets) { offset ->
                    val cal = todayCal.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, offset)
                    val label = when (offset) {
                        0 -> "Today"
                        -1 -> "Yesterday"
                        else -> dateFormat.format(cal.time)
                    }
                    val isSelected = offset == selectedOffset
                    var isFocused by remember { mutableStateOf(false) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedOffset = offset },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
                            .border(2.dp, if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            when {
                loading -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                errorMsg != null -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
                epgList.isEmpty() -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No shows available for this day", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isTv) 320.dp else 280.dp),
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(epgList) { _, program ->
                            val currentTime = System.currentTimeMillis()
                            val isLive = currentTime >= program.startEpoch && currentTime <= program.endEpoch
                            OmniCatchupTile(
                                program = program,
                                isLive = isLive,
                                isTv = isTv,
                                localPORT = localPORT,
                                isResolving = (resolvingProgramSrno == program.srno),
                                onClick = {
                                    if (isLive) {
                                        onPlayChannel(channel)
                                    } else {
                                        if (resolvingProgramSrno != null) return@OmniCatchupTile
                                        resolvingProgramSrno = program.srno
                                        coroutineScope.launch {
                                            val videoUrl = "http://127.0.0.1:$localPORT/catchup/render/${channel.id}?start=${program.startEpoch}&end=${program.endEpoch}&srno=${program.srno}"
                                            val resolved = resolveCatchupStream(context, videoUrl)
                                            resolvingProgramSrno = null
                                            if (resolved != null) {
                                                val catchupChannel = OmniChannel(
                                                    id = channel.id,
                                                    name = "[Catchup] ${program.showname}",
                                                    group = channel.group,
                                                    logo = channel.logo,
                                                    url = resolved.playUrl,
                                                    m3u8Url = if (!resolved.playUrl.contains(".mpd")) resolved.playUrl else null,
                                                    mpdUrl = if (resolved.playUrl.contains(".mpd")) resolved.playUrl else null,
                                                    licenseUrl = resolved.licenseUrl,
                                                    headers = (channel.headers ?: emptyMap()) + mapOf("catchup_web_url" to videoUrl)
                                                )
                                                onPlayChannel(catchupChannel)
                                            } else {
                                                val intent = Intent(context, WebPlayerActivity::class.java).apply {
                                                    putExtra("startup_url", videoUrl)
                                                    putExtra("target_channel_id", channel.id ?: "")
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OmniCatchupTile(
    program: EpgProgram,
    isLive: Boolean,
    isTv: Boolean,
    localPORT: Int,
    isResolving: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(enabled = !isResolving, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                             else MaterialTheme.colorScheme.surface
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (focused) 6.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTv) 100.dp else 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                if (program.episodePoster.isNotBlank()) {
                    AsyncImage(
                        model = "http://127.0.0.1:$localPORT/jtvposter/${program.episodePoster}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                if (isLive) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(3.dp)
                    ) {
                        Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                    }
                }
                if (isResolving) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.showname,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (program.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = program.description,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${program.showtime} – ${program.endtime}",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

suspend fun resolveCatchupStream(context: Context, renderUrl: String): ResolvedCatchupStream? {
    return withContext(Dispatchers.IO) {
        try {
            val connection = java.net.URL(renderUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val cleanHtml = html.replace("\\u0026", "&").replace("\\/", "/")

            val playUrlRegex = """player\.load\(\s*["']([^"']+)["']\s*\)""".toRegex()
            val licenseUrlRegex = """const licenseUrl\s*=\s*["']([^"']+)["']""".toRegex()

            var playUrlMatch = playUrlRegex.find(cleanHtml)?.groupValues?.get(1)
            var licenseUrlMatch = licenseUrlRegex.find(cleanHtml)?.groupValues?.get(1)

            if (playUrlMatch == null) {
                val hlsRegex = """src:\s*["']([^"']+)["']""".toRegex()
                playUrlMatch = hlsRegex.find(cleanHtml)?.groupValues?.get(1)
            }

            if (playUrlMatch != null) {
                val localBase = "http://127.0.0.1:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}"
                val absolutePlayUrl = if (playUrlMatch.startsWith("/")) "$localBase$playUrlMatch" else playUrlMatch
                val absoluteLicenseUrl = licenseUrlMatch?.let {
                    if (it.isBlank()) null
                    else if (it.startsWith("/")) "$localBase$it"
                    else it
                }
                ResolvedCatchupStream(absolutePlayUrl, absoluteLicenseUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("OmniCatchup", "Error resolving catchup stream", e)
            null
        }
    }
}

data class ResolvedCatchupStream(
    val playUrl: String,
    val licenseUrl: String?
)

// ──────────────────────────────────────────────────────────────────────────────
// Import Credentials Dialog & Parser
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OmniImportCredentialsDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    var importContent by remember { mutableStateOf("") }
    var isImportFocused by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val prefManager = remember { SkySharedPref.getInstance(context) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val content = inputStream.bufferedReader().use { it.readText() }
                        inputStream.close()
                        importContent = content
                        statusMessage = "Credentials loaded! Click Save & Login."
                    }
                } catch (e: Exception) {
                    statusMessage = "Error reading file: ${e.message}"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Import Jio Credentials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Paste TOML/JSON credentials or pick file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Credentials File")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = importContent,
                        onValueChange = { importContent = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged { isImportFocused = it.isFocused },
                        singleLine = false,
                        maxLines = 15
                    )
                    if (importContent.isEmpty()) {
                        Text(
                            text = "Paste TOML [data] block or JSON credentials here...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (importContent.isBlank()) {
                                statusMessage = "Please paste credentials TOML/JSON"
                            } else {
                                val success = saveJioCredentials(context, importContent)
                                if (success) {
                                    statusMessage = "Credentials saved! Restarting server..."
                                    Toast.makeText(context, "Credentials imported! Restarting server...", Toast.LENGTH_LONG).show()

                                    // Stop binary service
                                    val stopIntent = Intent(context, BinaryService::class.java).apply {
                                        action = BinaryService.ACTION_STOP_BINARY
                                    }
                                    context.startService(stopIntent)

                                    // Restart binary service after short delay
                                    scope.launch(Dispatchers.IO) {
                                        var waited = 0
                                        while (BinaryService.isRunning && waited < 4000) {
                                            delay(100)
                                            waited += 100
                                        }
                                        val startIntent = Intent(context, BinaryService::class.java).apply {
                                            putExtra(
                                                "binaryFileLocation",
                                                prefManager.myPrefs.jtvGoBinaryName?.let {
                                                    File(context.filesDir, it).absolutePath
                                                }
                                            )
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(startIntent)
                                        } else {
                                            context.startService(startIntent)
                                        }
                                    }
                                    onDismiss()
                                } else {
                                    statusMessage = "Failed to parse credentials"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save & Login")
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusMessage!!.contains("saved")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun saveJioCredentials(context: Context, input: String): Boolean {
    return try {
        val trimmed = input.trim()
        val keyMap = mutableMapOf<String, String>()

        if (trimmed.startsWith("{")) {
            val gson = Gson()
            val parsedMap = gson.fromJson(trimmed, Map::class.java)
            parsedMap.forEach { (k, v) ->
                if (k != null && v != null) {
                    keyMap[k.toString()] = v.toString()
                }
            }
        } else {
            trimmed.lines().forEach { line ->
                val lineTrimmed = line.trim()
                if (lineTrimmed.isNotEmpty() && !lineTrimmed.startsWith("#") && !lineTrimmed.startsWith("[") && lineTrimmed.contains("=")) {
                    val parts = lineTrimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        var value = parts[1].trim()
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length - 1)
                        }
                        keyMap[key] = value
                    }
                }
            }
        }

        if (keyMap.isEmpty()) return false

        val accessToken = keyMap["accessToken"] ?: keyMap["access_token"] ?: ""
        val ssoToken = keyMap["ssoToken"] ?: keyMap["sso_token"] ?: ""
        val crm = keyMap["crm"] ?: keyMap["crm_id"] ?: keyMap["subscriber_id"] ?: ""
        val deviceId = keyMap["deviceId"] ?: keyMap["device_id"] ?: ""
        val refreshToken = keyMap["refreshToken"] ?: keyMap["refresh_token"] ?: ""
        val uniqueId = keyMap["uniqueId"] ?: keyMap["unique_id"] ?: ""
        val lastSSOTokenRefreshTime = keyMap["lastSSOTokenRefreshTime"] ?: keyMap["last_sso_token_refresh_time"] ?: "${System.currentTimeMillis() / 1000}"
        val lastTokenRefreshTime = keyMap["lastTokenRefreshTime"] ?: keyMap["last_token_refresh_time"] ?: "${System.currentTimeMillis() / 1000}"

        if (accessToken.isNotEmpty()) { keyMap["accessToken"] = accessToken; keyMap["access_token"] = accessToken }
        if (ssoToken.isNotEmpty()) { keyMap["ssoToken"] = ssoToken; keyMap["sso_token"] = ssoToken }
        if (crm.isNotEmpty()) { keyMap["crm"] = crm; keyMap["crm_id"] = crm }
        if (deviceId.isNotEmpty()) { keyMap["deviceId"] = deviceId; keyMap["device_id"] = deviceId }
        if (refreshToken.isNotEmpty()) { keyMap["refreshToken"] = refreshToken; keyMap["refresh_token"] = refreshToken }
        if (uniqueId.isNotEmpty()) { keyMap["uniqueId"] = uniqueId; keyMap["unique_id"] = uniqueId }
        keyMap["lastSSOTokenRefreshTime"] = lastSSOTokenRefreshTime
        keyMap["lastTokenRefreshTime"] = lastTokenRefreshTime

        val gson = Gson()
        val jsonContent = gson.toJson(keyMap)

        val dir1 = File(context.filesDir, ".jiotv_go")
        if (!dir1.exists()) dir1.mkdirs()
        File(dir1, "jiotv_credentials_v2.json").writeText(jsonContent)
        File(dir1, "credentials.json").writeText(jsonContent)

        File(context.filesDir, "jiotv_credentials_v2.json").writeText(jsonContent)
        File(context.filesDir, "credentials.json").writeText(jsonContent)

        val tomlContent = buildString {
            append("[data]\n")
            keyMap.forEach { (k, v) ->
                append("  $k = \"$v\"\n")
            }
        }
        val loginDir = File(context.filesDir.parent, "files")
        loginDir.mkdirs()
        File(loginDir, "store_v4.toml").writeText(tomlContent)
        File(context.filesDir, "store_v4.toml").writeText(tomlContent)

        SkySharedPref.getInstance(context).reloadPreferences()

        true
    } catch (e: Exception) {
        Log.e("OmniImport", "Error saving imported credentials", e)
        false
    }
}

@Composable
fun LogViewerDialog(onDismiss: () -> Unit, onCopy: () -> Unit, onClear: () -> Unit) {
    var refreshTick by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Logs", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) },
        text = {
            val logs = remember(refreshTick) { LogCollector.getLogs() }
            Box(modifier = Modifier.height(300.dp).fillMaxWidth().background(Color.Black).padding(8.dp)) {
                val scrollState = rememberScrollState()
                Text(
                    text = if (logs.isBlank()) "No logs yet." else logs,
                    color = Color.Green,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(scrollState)
                )
            }
        },
        confirmButton = {
             Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                var isClearLogsFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        onClear()
                        refreshTick++
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .onFocusChanged { isClearLogsFocused = it.isFocused }
                        .background(if (isClearLogsFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                        .border(1.dp, if (isClearLogsFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear logs",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                var isCopyFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isCopyFocused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .onFocusChanged { isCopyFocused = it.isFocused }
                        .border(1.dp, if (isCopyFocused) MaterialTheme.colorScheme.primary else Color.Transparent, ButtonDefaults.shape)
                ) {
                    Text("Copy", color = if (isCopyFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                var isCloseFocused by remember { mutableStateOf(false) }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .onFocusChanged { isCloseFocused = it.isFocused }
                        .background(if (isCloseFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                        .border(1.dp, if (isCloseFocused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(4.dp))
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        dismissButton = null,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        titleContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun OmniServerListItem(
    server: OmniServer,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (isFocused) 1.05f else 1.0f, label = "serverScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelected() }
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (server.isFavoriteServer) Icons.Default.Star else Icons.Default.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = server.name,
                color = if (isFocused || isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Display name for whatever is currently set as the startup target. The built-in UIs are
 * pseudo-packages rather than installed apps, so they are matched by package id; anything
 * else is a real IPTV app and keeps its own name.
 */
fun omniDefaultUiLabel(pkg: String?, name: String?): String = when (pkg) {
    "omni" -> "Omni UI"
    "tvzone" -> "New TV UI"
    "webtv" -> "WEB TV"
    null, "" -> "None"
    else -> name ?: "External app"
}

/**
 * Single-select picker for the UI the app opens into. Writes the same preference the
 * "Select IPTV Player" screen writes, so the two stay in sync.
 */
@Composable
fun OmniDefaultUiDialog(
    currentPackage: String?,
    onDismiss: () -> Unit,
    onSelect: (pkg: String, label: String) -> Unit
) {
    val options = listOf(
        Triple("Omni UI", "omni", Icons.Default.GridView),
        Triple("New TV UI", "tvzone", Icons.Default.Tv),
        Triple("None (App Home)", "", Icons.Default.Home)
    )
    var selectedPkg by remember { mutableStateOf(currentPackage ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Default UI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "Choose which screen the app opens on launch:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { (label, pkg, icon) ->
                    val isSelected = selectedPkg == pkg
                    var isItemFocused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .onFocusChanged { isItemFocused = it.isFocused }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isItemFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isItemFocused) MaterialTheme.colorScheme.primary
                                else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedPkg = pkg
                                onSelect(pkg, label)
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedPkg = pkg
                                onSelect(pkg, label)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // An external IPTV app matches none of the rows above; say so rather than
                // showing an empty selection.
                if (options.none { it.second == selectedPkg }) {
                    Text(
                        text = "Currently: ${omniDefaultUiLabel(currentPackage, null)} (an installed app). Picking an option above replaces it.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        titleContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun AutoOpenServerDialog(
    currentAutoOpen: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val servers = listOf("Jio", "Favorites")
    var selected by remember { mutableStateOf(if (currentAutoOpen.equals("Favorites", ignoreCase = true) || currentAutoOpen == "favorite://omni") "Favorites" else "Jio") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Auto Open Server", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "Choose which server opens automatically when launching the Omni UI:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                servers.forEach { serverName ->
                    val isSelected = selected == serverName
                    var isItemFocused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .onFocusChanged { isItemFocused = it.isFocused }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isItemFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isItemFocused) MaterialTheme.colorScheme.primary
                                else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selected = serverName
                                onSelect(serverName)
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (serverName == "Favorites") Icons.Default.Star else Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = serverName,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selected = serverName
                                onSelect(serverName)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        titleContentColor = MaterialTheme.colorScheme.onBackground
    )
}




