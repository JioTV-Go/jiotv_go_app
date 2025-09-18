package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Animatable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.components.TvScreenMenu
import com.skylake.skytv.jgorunner.ui.tvhome.Main_Layout
import com.skylake.skytv.jgorunner.ui.tvhome.Recent_Layout
import com.skylake.skytv.jgorunner.ui.tvhome.Main_Layout_3rd
import com.skylake.skytv.jgorunner.ui.tvhome.depreciated.SearchTabLayout
import com.skylake.skytv.jgorunner.ui.tvhome.depreciated.SearchTabLayoutTV
import kotlinx.coroutines.launch
import kotlin.random.Random

@SuppressLint("NewApi")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ZoneScreen(context: Context, onNavigate: (String) -> Unit) {

    data class TabItem(val text: String, val icon: ImageVector)

    var showModeDialog by remember { mutableStateOf(false) }

    var reloadChannelsTrigger by remember { mutableStateOf(0) }


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

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var exitPressTime by remember { mutableLongStateOf(0L) }

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

                IconButton(onClick = { onNavigate("SettingsTV") }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (preferenceManager.myPrefs.customPlaylistSupport &&
                !preferenceManager.myPrefs.showPLAYLIST) {

                Main_Layout_3rd(context, reloadTrigger = reloadChannelsTrigger)

            } else if (!preferenceManager.myPrefs.showRecentTab) {

                Main_Layout(context, reloadTrigger = reloadChannelsTrigger)

            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tabs Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .focusRestorer()
                            .focusRequester(tabFocusRequester)
                            .focusable(),
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
                    }

                    // Tab Content
                    when (selectedTabIndex) {
                        0 -> Main_Layout(context, reloadTrigger = reloadChannelsTrigger)
                        1 -> Recent_Layout(context)
                        2 -> if (isRemoteNavigation) SearchTabLayoutTV(context, tabFocusRequester) else SearchTabLayout(context, tabFocusRequester)
                    }
                }
            }

        }
    }

    // Mode Dialog
    TvScreenMenu(
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

            reloadChannelsTrigger++
        },
        context = context
    )

    if (firstLaunch) {
        firstLaunch = false
        reloadChannelsTrigger++
    }

}
