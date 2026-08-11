package com.skylake.skytv.jgorunner.ui.tvhome

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.skylake.skytv.jgorunner.utils.HandleTvBackKey
import com.skylake.skytv.jgorunner.utils.RememberBackPressManager
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * OmniTvLayout - Self-contained TV UI matching jiotv_go_app's ZoneScreen.
 * Contains: Top bar (Filter, Title, Settings) + Tabs + Main_Layout content.
 */
@SuppressLint("NewApi")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OmniTvLayout(context: Context, reloadTrigger: Int) {

    data class TabItem(val text: String, val icon: ImageVector)

    var showModeDialog by remember { mutableStateOf(false) }
    var reloadChannelsTrigger by remember { mutableIntStateOf(reloadTrigger) }

    val preferenceManager = SkySharedPref.getInstance(context)
    val savedTabIndex = preferenceManager.myPrefs.selectedScreenTV?.toIntOrNull() ?: 0

    val tabs = listOf(
        TabItem("TV", Icons.Default.Tv),
        TabItem("Favorite", Icons.Default.Star),
        TabItem("Search", Icons.Default.Search)
    )

    var selectedTabIndex by remember { mutableIntStateOf(savedTabIndex) }
    val tabFocusRequester = remember { FocusRequester() }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    HandleTvBackKey { /* handled by BackHandler in MainActivity */ }

    RememberBackPressManager(
        timeoutMs = 2000L,
        onExit = { /* handled by BackHandler in MainActivity */ },
        showHint = {
            snackbarHostState.currentSnackbarData?.dismiss()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Press back again to exit", duration = SnackbarDuration.Short)
            }
        }
    )

    val glowColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
    val glowColor = remember { Animatable(glowColors[Random.nextInt(glowColors.size)]) }
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))

    LaunchedEffect(Unit) {
        if (!preferenceManager.myPrefs.homeIptvEnabled) {
            preferenceManager.myPrefs.homeIptvEnabled = true
            preferenceManager.savePreferencesQuick()
        }
    }

    LaunchedEffect(selectedTabIndex) {
        preferenceManager.myPrefs.selectedScreenTV = selectedTabIndex.toString()
        preferenceManager.savePreferences()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                    text = "OMNI UI",
                    fontSize = 12.sp,
                    fontFamily = customFontFamily,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(shadow = Shadow(color = glowColor.value, blurRadius = 30f))
                )

                IconButton(onClick = { /* Settings handled externally */ }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.TwoTone.Settings,
                        contentDescription = "Settings Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Tabs Row
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
                        var isFocused by remember { mutableStateOf(false) }
                        val isActive = index == selectedTabIndex || isFocused

                        Tab(
                            modifier = Modifier
                                .onFocusChanged { isFocused = it.isFocused }
                                .background(
                                    color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                ),
                            selected = index == selectedTabIndex,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.text,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.text,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontSize = 12.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = if (isFocused && index != selectedTabIndex) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Tab Content - calls the copied jiotv_go_app layouts
            when (selectedTabIndex) {
                0 -> OmniTvMain_Layout(context, reloadTrigger = reloadChannelsTrigger)
                1 -> OmniTvRecent_Layout(context)
                2 -> OmniTvSearchTabLayout(context, tabFocusRequester)
            }
        }
    }

    // Mode Dialog - calls the copied jiotv_go_app TvScreenMenu
    OmniTvScreenMenu(
        showDialog = showModeDialog,
        onDismiss = { showModeDialog = false },
        onReset = {
            showModeDialog = false
            Toast.makeText(context, "Refreshing Channels", Toast.LENGTH_LONG).show()
            selectedTabIndex = preferenceManager.myPrefs.selectedScreenTV?.toIntOrNull() ?: 0
        },
        onSelectionsMade = { selectedQualities, selectedCategories, _, selectedLanguages, _ ->
            Toast.makeText(context, "Refreshing Channels", Toast.LENGTH_LONG).show()
            selectedTabIndex = preferenceManager.myPrefs.selectedScreenTV?.toIntOrNull() ?: 0
            reloadChannelsTrigger++
        },
        context = context
    )

    LaunchedEffect(Unit) { reloadChannelsTrigger++ }
}
