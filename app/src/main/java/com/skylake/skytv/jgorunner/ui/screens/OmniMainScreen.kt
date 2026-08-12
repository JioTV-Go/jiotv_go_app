package com.skylake.skytv.jgorunner.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skylake.skytv.jgorunner.activities.OmniPlayerActivity
import com.skylake.skytv.jgorunner.data.OmniFavoritesStore
import com.skylake.skytv.jgorunner.data.OmniRepository
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import com.skylake.skytv.jgorunner.ui.tvhome.EpgProgram
import com.skylake.skytv.jgorunner.ui.tvhome.EpgResponse
import com.skylake.skytv.jgorunner.activities.WebPlayerActivity
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun OmniMainScreen(context: Context, onNavigate: (String) -> Unit) {
    val prefManager = SkySharedPref.getInstance(context)
    val scope = rememberCoroutineScope()
    val favoriteStore = remember { OmniFavoritesStore(prefManager) }
    
    var channels by remember { mutableStateOf<List<OmniChannel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // We add a virtual category "Favorites"
    var selectedCategory by remember { mutableStateOf<String?>("All") }
    var favoriteUpdateTick by remember { mutableIntStateOf(0) }
    var freeOnly by remember { mutableStateOf(prefManager.myPrefs.freeOnly) }
    var freeJioCatchup by remember { mutableStateOf(prefManager.myPrefs.freeJioCatchup) }
    var catchupChannelTarget by remember { mutableStateOf<OmniChannel?>(null) }

    val categories = remember(channels, favoriteUpdateTick) {
        val list = mutableListOf("All")
        if (favoriteStore.load().isNotEmpty()) {
            list.add("Favorites")
        }
        list.addAll(channels.mapNotNull { it.group }.distinct().sorted())
        list
    }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            errorMessage = null
            val port = prefManager.myPrefs.jtvGoServerPort
            val fetched = OmniRepository(context).fetchChannels(port)
            if (fetched.isEmpty()) {
                errorMessage = "No channels found. Please make sure the server is running."
            } else {
                channels = fetched
            }
            isLoading = false
        }
    }

    val filtered = remember(channels, searchQuery, selectedCategory, freeOnly, favoriteUpdateTick) {
        channels.filter { ch ->
            val matchesSearch = searchQuery.isBlank() || 
                (ch.name?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (ch.group?.contains(searchQuery, ignoreCase = true) ?: false)
            
            val matchesCategory = when (selectedCategory) {
                "All", null -> true
                "Favorites" -> favoriteStore.isFavorite(ch)
                else -> ch.group == selectedCategory
            }
            matchesSearch && matchesCategory
        }
    }

    val isTv = com.skylake.skytv.jgorunner.utils.DeviceUtils.isTvDevice(context)
    val sidebarFocusRequester = remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else {
            onNavigate("Home")
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Sidebar for Categories
        Column(
            modifier = Modifier
                .width(if (isTv) 260.dp else 200.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text = "Omni TV",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            // Search Bar Component
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var freeOnlyFocused by remember { mutableStateOf(false) }
            val focusBorderColor = MaterialTheme.colorScheme.primary

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .onFocusChanged { freeOnlyFocused = it.isFocused }
                    .border(
                        2.dp,
                        if (freeOnlyFocused) focusBorderColor else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        freeOnly = !freeOnly
                        prefManager.myPrefs.freeOnly = freeOnly
                        prefManager.savePreferences()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Checkbox(
                    checked = freeOnly,
                    onCheckedChange = { checked ->
                        freeOnly = checked
                        prefManager.myPrefs.freeOnly = checked
                        prefManager.savePreferences()
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Free only",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            var catchupFocused by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Checkbox(
                    checked = freeJioCatchup,
                    onCheckedChange = { checked ->
                        freeJioCatchup = checked
                        prefManager.myPrefs.freeJioCatchup = checked
                        prefManager.savePreferences()
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Catchup",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Categories List
            LazyColumn(
                modifier = Modifier.weight(1f).focusRequester(sidebarFocusRequester),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    var isFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    isFocused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (cat) {
                                    "All" -> Icons.Default.Home
                                    "Favorites" -> Icons.Default.Favorite
                                    else -> Icons.Default.Folder
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cat,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Main Grid Area
        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    val port = prefManager.myPrefs.jtvGoServerPort
                                    val fetched = OmniRepository(context).fetchChannels(port)
                                    if (fetched.isEmpty()) {
                                        errorMessage = "No channels found. Please make sure the server is running."
                                    } else {
                                        channels = fetched
                                    }
                                    isLoading = false
                                }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                filtered.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (isTv) 130.dp else 100.dp),
                        modifier = Modifier.fillMaxSize().focusRequester(gridFocusRequester),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filtered, key = { _, ch -> ch.url ?: ch.name ?: "" }) { index, ch ->
                            var isFocused by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(if (isFocused) 1.08f else 1f)
                            val isFav = favoriteStore.isFavorite(ch)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .focusable()
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .clickable {
                                        if (freeJioCatchup) {
                                            catchupChannelTarget = ch
                                        } else {
                                            val intent = Intent(context, OmniPlayerActivity::class.java).apply {
                                                putExtra("channel_list", ArrayList(filtered))
                                                putExtra("channel_index", index)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 1.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(if (isTv) 72.dp else 56.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.05f))
                                        ) {
                                            if (!ch.logo.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = ch.logo,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LiveTv,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = ch.name ?: "Unknown",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (!ch.group.isNullOrBlank()) {
                                            Text(
                                                text = ch.group,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Top right indicator badges
                                    Row(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isFav) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Favorite",
                                                tint = Color.Red,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    catchupChannelTarget?.let { target ->
        CatchupOverlay(
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
            filteredChannels = filtered
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchupOverlay(
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

    // Fetch EPG whenever the selected day offset changes
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
            .zIndex(100f) // Draw on top of all Omni views
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = channel.logo ?: "",
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${channel.name ?: ""} - Catchup Guide",
                    style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                                isResolving = (resolvingProgramSrno == program.srno),
                                onClick = {
                                    if (isLive) {
                                        onPlayChannel(channel)
                                    } else {
                                        if (resolvingProgramSrno != null) return@CatchupTile
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
                                                // Fallback to WebPlayerActivity if resolution fails
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

suspend fun resolveCatchupStream(context: Context, renderUrl: String): ResolvedCatchupStream? {
    return withContext(Dispatchers.IO) {
        try {
            val connection = java.net.URL(renderUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val cleanHtml = html.replace("\\u0026", "&").replace("\\/", "/")
            
            // 1. Try to find DRM parameters
            // Example: await player.load("/render.mpd?auth=...");
            val playUrlRegex = """player\.load\(\s*["']([^"']+)["']\s*\)""".toRegex()
            val licenseUrlRegex = """const licenseUrl\s*=\s*["']([^"']+)["']""".toRegex()
            
            var playUrlMatch = playUrlRegex.find(cleanHtml)?.groupValues?.get(1)
            var licenseUrlMatch = licenseUrlRegex.find(cleanHtml)?.groupValues?.get(1)
            
            // 2. If not DRM, look for HLS
            // Example: src: "/catchup/stream/143?start=..."
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
            Log.e("CatchupOverlay", "Error resolving catchup stream", e)
            null
        }
    }
}

data class ResolvedCatchupStream(
    val playUrl: String,
    val licenseUrl: String?
)

@Composable
fun CatchupTile(
    program: EpgProgram,
    isLive: Boolean,
    isTv: Boolean,
    localPORT: Int,
    isResolving: Boolean,
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
            .clickable(enabled = !isResolving, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (focused) Color(0xFF1F353D) else Color(0xFF1E1E1E)
        ),
        shape = shape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTv) 110.dp else 90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E2E2E))
            ) {
                if (program.episodePoster.isNotBlank()) {
                    AsyncImage(
                        model = "http://localhost:$localPORT/jtvposter/${program.episodePoster}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                if (isResolving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isLive) {
                    Text(
                        text = "LIVE",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = program.showname,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (program.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${program.showtime} - ${program.endtime}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
