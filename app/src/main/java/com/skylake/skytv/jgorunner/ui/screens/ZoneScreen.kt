package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.ExoplayerActivityPass
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.components.ModeSelectionDialog2
import com.skylake.skytv.jgorunner.ui.dev.Recent_Layout
import com.skylake.skytv.jgorunner.ui.dev.Recent_LayoutTV
import com.skylake.skytv.jgorunner.ui.dev.SearchTabLayout
import com.skylake.skytv.jgorunner.ui.dev.SearchTabLayoutTV
import com.skylake.skytv.jgorunner.ui.dev.Main_Layout
import com.skylake.skytv.jgorunner.ui.dev.Main_LayoutTV
import com.skylake.skytv.jgorunner.ui.dev.Main_LayoutTV_3rd
import com.skylake.skytv.jgorunner.ui.dev.Main_Layout_3rd
import com.skylake.skytv.jgorunner.ui.dev.ChannelUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.ui.dev.M3UChannelExp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlin.random.Random

@SuppressLint("NewApi")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ZoneScreen(context: Context, onNavigate: (String) -> Unit) {

    data class TabItem(val text: String, val icon: ImageVector)

    var showModeDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        TabItem("TV", Icons.Default.Tv),
        TabItem("Recent", Icons.Default.Star)
//        TabItem("Search", Icons.Default.Search)
    )

    val isRemoteNavigation =
        context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
                Configuration.UI_MODE_TYPE_TELEVISION

    Log.d("ZoneScreen", "Running in TV Mode: $isRemoteNavigation")

    val preferenceManager = SkySharedPref.getInstance(context)
    val savedTabIndex = preferenceManager.myPrefs.selectedScreenTV?.toIntOrNull() ?: 0

    var selectedTabIndex by remember { mutableIntStateOf(savedTabIndex) }
    val tabFocusRequester = remember { FocusRequester() }
    var zoneAutoLoopEnabled by remember { mutableStateOf(preferenceManager.myPrefs.zoneAutoLoopEnabled) }

    // Mark that we're inside Zone screen for lifecycle scoping    
    LaunchedEffect(Unit) {
        preferenceManager.myPrefs.isInZoneScreen = true
        if (isRemoteNavigation) {
            preferenceManager.myPrefs.startTvAutomatically = true
            preferenceManager.myPrefs.tvPlayerActive = false
        }
        preferenceManager.savePreferences()
    }

    // On dispose, mark leaving Zone
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            preferenceManager.myPrefs.isInZoneScreen = false
            preferenceManager.savePreferences()
        }
    }
    
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var exitPressTime by remember { mutableStateOf(0L) }

    var firstLaunch by remember { mutableStateOf(true) }

    // Back press handler
    BackHandler {
        val now = System.currentTimeMillis()
        if (now - exitPressTime < 2000) {
            onNavigate("Home")
        } else {
            exitPressTime = now
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Press back again to exit",
                    actionLabel = "Exit",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNavigate("Home")
                }
            }
        }
    }

    // UI Glow Effect
    val glowColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
    val glowColor = remember { Animatable(glowColors[Random.nextInt(glowColors.size)]) }
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Log.d("_", innerPadding.toString())

            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { showModeDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Filter Icon",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "JTV-GO",
                    fontSize = 12.sp,
                    fontFamily = customFontFamily,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(
                        shadow = Shadow(
                            color = glowColor.value,
                            blurRadius = 30f
                        )
                    )
                )

                // Auto-play controls (interval + small toggle) only on TV tab
                if (selectedTabIndex == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Auto-play",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier // Removed padding
                        )

                        // Small numeric interval (seconds)
                        val initialInterval = preferenceManager.myPrefs.zoneAutoLoopIntervalSec
                            .coerceIn(5, 600)
                        var intervalText by remember { mutableStateOf(initialInterval.toString()) }

                        androidx.compose.material3.OutlinedTextField(
                            value = intervalText,
                            onValueChange = { newVal ->
                                val digits = newVal.filter { it.isDigit() }.take(3)
                                intervalText = digits
                                val parsed = digits.toIntOrNull()?.coerceIn(5, 600)
                                if (parsed != null) {
                                    preferenceManager.myPrefs.zoneAutoLoopIntervalSec = parsed
                                    preferenceManager.savePreferences()
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .size(55.dp, 60.dp) // Set both width and height
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        if (intervalText.isBlank()) {
                                            intervalText = "20"
                                            preferenceManager.myPrefs.zoneAutoLoopIntervalSec = 20
                                            preferenceManager.savePreferences()
                                        } else {
                                            val parsed = intervalText.toIntOrNull()?.coerceIn(5, 600) ?: 20
                                            intervalText = parsed.toString()
                                            preferenceManager.myPrefs.zoneAutoLoopIntervalSec = parsed
                                            preferenceManager.savePreferences()
                                        }
                                    }
                                },
                            textStyle = MaterialTheme.typography.bodySmall,
                            placeholder = { Text("sec", style = MaterialTheme.typography.bodySmall) },
                            label = { Text("sec", style = MaterialTheme.typography.bodySmall) },
                            enabled = !zoneAutoLoopEnabled // Disable input if toggle is enabled
                        )

                        Spacer(modifier = Modifier.size(28.dp, 28.dp)) // Increased space between input and toggle

                        androidx.compose.material3.Switch(
                            checked = zoneAutoLoopEnabled,
                            onCheckedChange = { enabled ->
                                zoneAutoLoopEnabled = enabled
                                preferenceManager.myPrefs.zoneAutoLoopEnabled = enabled
                                preferenceManager.savePreferences()
                            },
                            modifier = Modifier.size(5.dp) // Changed size to make the toggle smaller
                        )
                    }
                }

                IconButton(onClick = { onNavigate("SettingsTV") }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Tabs + Zone Auto-Loop Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRestorer()
                    .focusRequester(tabFocusRequester),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = "Tab Icon",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = tab.text)
                                }
                            }
                        )
                    }
                }
            
            Spacer(modifier = Modifier.weight(1f))
            }
        
            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    if (preferenceManager.myPrefs.customPlaylistSupport &&
                        !preferenceManager.myPrefs.showPLAYLIST
                    ) {
                        if (isRemoteNavigation) Main_LayoutTV_3rd(context)
                        else Main_Layout_3rd(context)
                    } else {
                        if (isRemoteNavigation) Main_LayoutTV(context)
                        else Main_Layout(context)
                    }
                }
                1 -> if (isRemoteNavigation) Recent_LayoutTV(context) else Recent_Layout(context)
//                2 -> if (isRemoteNavigation) SearchTabLayoutTV(context, tabFocusRequester) else SearchTabLayout(context, tabFocusRequester)
            }
        }
    }

    // Mode Dialog
    ModeSelectionDialog2(
        showDialog = showModeDialog,
        onDismiss = { showModeDialog = false },
        onReset = {
            showModeDialog = false
            Toast.makeText(context, "Refreshing Channels", Toast.LENGTH_LONG).show()
            selectedTabIndex = savedTabIndex
        },
        onSelectionsMade = { selectedQualities, selectedCategories, _, selectedLanguages, _ ->
            Log.d("ZoneScreen", "Qualities: $selectedQualities, Categories: $selectedCategories, Languages: $selectedLanguages")
            Toast.makeText(context, "Refreshing Channels", Toast.LENGTH_LONG).show()
            selectedTabIndex = savedTabIndex
        },
        context = context
    )

    LaunchedEffect(Unit) {
        tabFocusRequester.requestFocus()

        if (firstLaunch && tabs.size > 1) {
            firstLaunch = false
            val originalIndex = selectedTabIndex
            selectedTabIndex = (originalIndex + 1) % tabs.size
            kotlinx.coroutines.delay(50)
            selectedTabIndex = originalIndex
        }
    }


    // Scoped auto-play loop for Zone TV tab only
    LaunchedEffect(selectedTabIndex, zoneAutoLoopEnabled, preferenceManager.myPrefs.zoneAutoLoopIntervalSec) {
        if (selectedTabIndex != 0) return@LaunchedEffect
        if (!zoneAutoLoopEnabled) return@LaunchedEffect

        val port = preferenceManager.myPrefs.jtvGoServerPort
        val baseUrl = "http://localhost:$port"

        while (isActive && selectedTabIndex == 0 && zoneAutoLoopEnabled) {
            try {
                // respect user suppress window and current player state
                val suppressUntil = preferenceManager.myPrefs.autoPlaySuppressUntil
                val nowTs = System.currentTimeMillis()
                if (suppressUntil > nowTs || preferenceManager.myPrefs.tvPlayerActive) {
                    kotlinx.coroutines.delay(2000)
                    continue
                }

                // If experimental custom playlist is enabled, prefer it for autoplay
                val useCustom = preferenceManager.myPrefs.customPlaylistSupport && !preferenceManager.myPrefs.showPLAYLIST
                if (useCustom) {
                    val json = preferenceManager.myPrefs.channelListJson
                    val channels: List<M3UChannelExp> = try {
                        if (!json.isNullOrBlank()) {
                            val type = object : TypeToken<List<M3UChannelExp>>() {}.type
                            Gson().fromJson<List<M3UChannelExp>>(json, type)
                        } else emptyList()
                    } catch (_: Exception) { emptyList() }

                    val allChannels = channels.distinctBy { it.url }
                    if (allChannels.isNotEmpty()) {
                        val selectedCategory = preferenceManager.myPrefs.lastSelectedCategoryExp ?: "All"
                        val filtered = if (selectedCategory == "All") allChannels else allChannels.filter { it.category == selectedCategory }
                        val effectiveList = if (filtered.isNotEmpty()) filtered else allChannels

                        val prefCurrUrl = preferenceManager.myPrefs.currChannelUrl
                        val selected = if (!prefCurrUrl.isNullOrEmpty()) {
                            effectiveList.find { it.url == prefCurrUrl } ?: effectiveList.first()
                        } else effectiveList.first()
                        val selectedIndex = effectiveList.indexOf(selected).coerceAtLeast(0)

                        try {
                            val intent = Intent(context, com.skylake.skytv.jgorunner.services.player.ExoPlayJet::class.java).apply {
                                putExtra("zone", "TV")
                                putParcelableArrayListExtra(
                                    "channel_list_data",
                                    java.util.ArrayList(effectiveList.map { ch ->
                                        com.skylake.skytv.jgorunner.activities.ChannelInfo(
                                            ch.url,
                                            ch.logo ?: "",
                                            ch.name
                                        )
                                    })
                                )
                                putExtra("current_channel_index", selectedIndex)
                                putExtra("video_url", selected.url)
                                putExtra("logo_url", selected.logo ?: "")
                                putExtra("ch_name", selected.name)
                            }

                            preferenceManager.myPrefs.tvPlayerActive = true
                            preferenceManager.savePreferences()
                            ContextCompat.startActivity(context, intent, null)
                        } catch (_: Exception) { }
                    }
                } else {
                    // Default zone UI: fetch channels from local server and autoplay
                    val response = ChannelUtils.fetchChannels("$baseUrl/channels")
                    val categoryIds = preferenceManager.myPrefs.filterCI
                        ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.filter { it != 0 }
                        ?.takeIf { it.isNotEmpty() }
                    val languageIds = preferenceManager.myPrefs.filterLI
                        ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.filter { it != 0 }
                        ?.takeIf { it.isNotEmpty() }

                    val list = ChannelUtils.filterChannels(
                        response,
                        languageIds = languageIds,
                        categoryIds = categoryIds,
                        isHD = null
                    )
                    if (list.isNotEmpty()) {
                        val prefCurrUrl = preferenceManager.myPrefs.currChannelUrl
                        val selected = if (!prefCurrUrl.isNullOrEmpty()) {
                            list.find { it.channel_url == prefCurrUrl } ?: list.first()
                        } else list.first()
                        val selectedIndex = list.indexOf(selected).coerceAtLeast(0)

                        try {
                            val intent = Intent(context, com.skylake.skytv.jgorunner.services.player.ExoPlayJet::class.java).apply {
                                putExtra("zone", "TV")
                                putParcelableArrayListExtra(
                                    "channel_list_data",
                                    java.util.ArrayList(list.map { ch ->
                                        com.skylake.skytv.jgorunner.activities.ChannelInfo(
                                            ch.channel_url ?: "",
                                            "http://localhost:$port/jtvimage/${ch.logoUrl ?: ""}",
                                            ch.channel_name ?: ""
                                        )
                                    })
                                )
                                putExtra("current_channel_index", selectedIndex)
                                putExtra("video_url", selected.channel_url ?: "")
                                putExtra("logo_url", "http://localhost:$port/jtvimage/${selected.logoUrl ?: ""}")
                                putExtra("ch_name", selected.channel_name ?: "")
                            }

                            preferenceManager.myPrefs.tvPlayerActive = true
                            preferenceManager.savePreferences()
                            ContextCompat.startActivity(context, intent, null)
                        } catch (_: Exception) { }
                    }
                }
            } catch (_: Exception) { }

            // Wait user-defined interval (min 5s, max 600s)
            val intervalMs = (preferenceManager.myPrefs.zoneAutoLoopIntervalSec
                .coerceIn(5, 600)) * 1000L
            kotlinx.coroutines.delay(intervalMs)
        }
    }}

