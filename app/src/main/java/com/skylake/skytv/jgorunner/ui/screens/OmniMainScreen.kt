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
import kotlinx.coroutines.launch

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
                                        val intent = Intent(context, OmniPlayerActivity::class.java).apply {
                                            putExtra("channel_list", ArrayList(filtered))
                                            putExtra("channel_index", index)
                                        }
                                        context.startActivity(intent)
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
}
