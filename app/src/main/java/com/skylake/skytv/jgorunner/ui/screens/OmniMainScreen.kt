package com.skylake.skytv.jgorunner.ui.screens

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.skylake.skytv.jgorunner.activities.OmniPlayerActivity
import com.skylake.skytv.jgorunner.activities.WebPlayerActivity

import com.skylake.skytv.jgorunner.data.OmniRepository
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import com.skylake.skytv.jgorunner.ui.tvhome.EpgProgram
import com.skylake.skytv.jgorunner.ui.tvhome.EpgResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun OmniMainScreen(context: Context, onNavigate: (String) -> Unit) {
    val prefManager = remember { SkySharedPref.getInstance(context) }
    val repository = remember { OmniRepository(context) }
    val port = prefManager.myPrefs.jtvGoServerPort
    val scope = rememberCoroutineScope()

    var channels by remember { mutableStateOf<List<OmniChannel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isSidebarVisible by remember { mutableStateOf(true) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var favoriteUpdateTick by remember { mutableIntStateOf(0) }
    var freeOnly by remember { mutableStateOf(prefManager.myPrefs.freeOnly) }
    var freeJioCatchup by remember { mutableStateOf(prefManager.myPrefs.freeJioCatchup) }
    var catchupChannelTarget by remember { mutableStateOf<OmniChannel?>(null) }

    val searchFocusRequester = remember { FocusRequester() }
    val firstChannelFocusRequester = remember { FocusRequester() }
    val firstSidebarFocusRequester = remember { FocusRequester() }

    // All unique category groups from channels
    val categories = remember(channels, favoriteUpdateTick) {
        val list = mutableListOf<String?>("All")
        channels.mapNotNull { it.group }.distinct().sorted().forEach { list.add(it) }
        list
    }

    val filteredChannels = remember(channels, searchQuery, selectedCategory, freeOnly) {
        channels.filter { channel ->
            val matchesSearch = searchQuery.isEmpty() ||
                channel.name?.contains(searchQuery, ignoreCase = true) == true ||
                channel.group?.contains(searchQuery, ignoreCase = true) == true

            val matchesCategory = selectedCategory == null || selectedCategory == "All" ||
                channel.group?.equals(selectedCategory, ignoreCase = true) == true

            val matchesFreeOnly = !freeOnly || channel.url?.contains("/live/") == true

            matchesSearch && matchesCategory && matchesFreeOnly
        }
    }

    // Load channels on first compose
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val fetched = withContext(Dispatchers.IO) { repository.fetchChannels(port) }
            channels = fetched
            if (fetched.isEmpty()) errorMessage = "No channels found."
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load channels."
        }
        isLoading = false
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
            !isSidebarVisible -> isSidebarVisible = true
            else -> onNavigate("back")
        }
    }

    val isTv = com.skylake.skytv.jgorunner.utils.DeviceUtils.isTvDevice(context)
    val drawerWidth = if (isTv) 300.dp else 244.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                            text = "Omni TV",
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
                                        channels = withContext(Dispatchers.IO) { repository.fetchChannels(port) }
                                        errorMessage = null
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage
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
                    // ---- CATEGORIES ----
                    item { OmniDrawerSectionLabel("CATEGORIES") }
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat || (cat == "All" && selectedCategory == null)
                        var isFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (isFocused) 1.05f else 1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp, horizontal = 2.dp)
                                .scale(scale)
                                .onFocusChanged { isFocused = it.isFocused }
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent
                                )
                                .border(
                                    2.dp,
                                    if (isFocused) MaterialTheme.colorScheme.primary
                                    else if (isSelected) MaterialTheme.colorScheme.outline
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedCategory = if (cat == "All") null else cat
                                    isSidebarVisible = false
                                }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = cat ?: "All",
                                color = if (isFocused || isSelected) MaterialTheme.colorScheme.onBackground
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // ---- PLAYBACK OPTIONS ----
                    item { OmniDrawerSectionLabel("PLAYBACK") }
                    item {
                        var checked by remember { mutableStateOf(freeOnly) }
                        OmniSettingsToggle("Free only", checked) {
                            checked = it
                            freeOnly = it
                            prefManager.myPrefs.freeOnly = it
                            prefManager.savePreferences()
                        }
                    }
                    item {
                        var checked by remember { mutableStateOf(freeJioCatchup) }
                        OmniSettingsToggle("Catchup mode", checked) {
                            checked = it
                            freeJioCatchup = it
                            prefManager.myPrefs.freeJioCatchup = it
                            prefManager.savePreferences()
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
                                text = selectedCategory ?: "Omni TV",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // Filter pills (Free / Catchup)
                            if (freeOnly) {
                                OmniFilterPill(label = "Free", active = true, onClick = {
                                    freeOnly = false
                                    prefManager.myPrefs.freeOnly = false
                                    prefManager.savePreferences()
                                })
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (freeJioCatchup) {
                                OmniFilterPill(label = "Catchup", active = true, onClick = {
                                    freeJioCatchup = false
                                    prefManager.myPrefs.freeJioCatchup = false
                                    prefManager.savePreferences()
                                })
                                Spacer(modifier = Modifier.width(4.dp))
                            }

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

            // --- CHANNEL CONTENT ---
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
                                            channels = withContext(Dispatchers.IO) { repository.fetchChannels(port) }
                                            errorMessage = null
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage
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
                else -> {
                    // Adaptive grid: same as CloudMainScreen — Adaptive(112dp TV / 100dp phone)
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
                                    if (freeJioCatchup) {
                                        catchupChannelTarget = channel
                                    } else {
                                        val intent = Intent(context, OmniPlayerActivity::class.java).apply {
                                            putExtra("channel_list", ArrayList(filteredChannels))
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
    }

    // Catchup overlay — same as CloudMainScreen
    catchupChannelTarget?.let { target ->
        OmniCatchupOverlay(
            channel = target,
            localPORT = prefManager.myPrefs.jtvGoServerPort,
            onClose = { catchupChannelTarget = null },
            context = context,
            preferenceManager = prefManager,
            onPlayChannel = { resolvedChannel ->
                val intent = Intent(context, OmniPlayerActivity::class.java).apply {
                    putExtra("channel_list", arrayListOf(resolvedChannel))
                    putExtra("channel_index", 0)
                }
                context.startActivity(intent)
            },
            filteredChannels = filteredChannels
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Channel grid item — mirrors ChannelGridItemCompact exactly
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
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                    ) {
                        Text(
                            "HD",
                            color = MaterialTheme.colorScheme.onError,
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
// Small filter pill — mirrors CloudFilterPill exactly
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun OmniFilterPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .height(28.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() },
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
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = contentColor)
            Icon(Icons.Filled.Close, contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Drawer section label — mirrors DrawerSectionLabel exactly
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
// Settings toggle row — mirrors SettingsToggleRefreshed exactly
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
// Catchup overlay — adapted from CloudMainScreen's CatchupOverlay
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
                val urlString = "http://localhost:$localPORT/epg/$channelId/$selectedOffset"
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
                val finalEpg = if (selectedOffset == 0) {
                    val liveShow = pastAndLive.find { currentTime >= it.startEpoch && currentTime <= it.endEpoch }
                    if (liveShow != null) listOf(liveShow) + pastAndLive.filter { it.srno != liveShow.srno }.reversed()
                    else pastAndLive.reversed()
                } else {
                    pastAndLive.reversed()
                }

                withContext(Dispatchers.Main) {
                    epgList = finalEpg
                    loading = false
                }
            }
        } catch (e: Exception) {
            Log.e("OmniCatchup", "Error fetching EPG", e)
            withContext(Dispatchers.Main) {
                errorMsg = "Failed to load catchup guide"
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
            // Header
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

            // Day chips
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

            // Program list / grid
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
                                            val videoUrl = "http://localhost:$localPORT/catchup/render/${channel.id}?start=${program.startEpoch}&end=${program.endEpoch}&srno=${program.srno}"
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

// ──────────────────────────────────────────────────────────────────────────────
// Catchup tile — mirrors CatchupTile in CloudMainScreen
// ──────────────────────────────────────────────────────────────────────────────
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
                        model = "http://localhost:$localPORT/jtvposter/${program.episodePoster}",
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

// ──────────────────────────────────────────────────────────────────────────────
// Native catchup stream resolver — same logic as CloudMainScreen
// ──────────────────────────────────────────────────────────────────────────────
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
                val localBase = "http://localhost:${SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort}"
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
