package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.Favorites_Layout
import com.skylake.skytv.jgorunner.ui.tvhome.Main_Layout
import com.skylake.skytv.jgorunner.ui.tvhome.Main_Layout_3rd
import com.skylake.skytv.jgorunner.ui.tvhome.Recent_Layout
import com.skylake.skytv.jgorunner.ui.tvhome.SearchTabLayout
import com.skylake.skytv.jgorunner.ui.tvhome.components.TvScreenMenu
import com.skylake.skytv.jgorunner.ui.tvhome.TvViewModel
import com.skylake.skytv.jgorunner.utils.HandleTvBackKey
import com.skylake.skytv.jgorunner.utils.RememberBackPressManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@SuppressLint("NewApi")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ZoneScreen(context: Context, onNavigate: (String) -> Unit) {

    data class TabItem(val text: String, val icon: ImageVector)

    var showModeDialog by remember { mutableStateOf(false) }

    var reloadChannelsTrigger by remember { mutableIntStateOf(0) }

    val tvViewModel: TvViewModel = viewModel()


    val tabs = listOf(
        TabItem("TV", Icons.Default.Tv),
        TabItem("Favorite", Icons.Default.Star),
//        TabItem("Recent", Icons.Default.History),
        TabItem("Search", Icons.Default.Search)
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

    var firstLaunch by remember { mutableStateOf(true) }
    val GoldColor = Color(0xFFFFD700)

    fun isTelevision(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    val isTv = isTelevision()

//    val isSessionConnected = remember { mutableStateOf(false) }
//
//    val castContext = remember {
//        CastContext.getSharedInstance(context)
//    }
//
//    val sessionManagerListener = remember {
//        object : SessionManagerListener<CastSession> {
//            override fun onSessionStarted(session: CastSession, sessionId: String) {
//                isSessionConnected.value = true
//            }
//
//            override fun onSessionEnded(session: CastSession, error: Int) {
//                isSessionConnected.value = false
//            }
//
//            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
//                isSessionConnected.value = true
//            }
//
//            override fun onSessionStarting(session: CastSession) {}
//            override fun onSessionStartFailed(session: CastSession, error: Int) {}
//            override fun onSessionEnding(session: CastSession) {}
//            override fun onSessionResuming(session: CastSession, sessionId: String) {}
//            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
//            override fun onSessionSuspended(session: CastSession, reason: Int) {}
//        }
//    }
//
//    DisposableEffect(castContext) {
//        val sessionManager = castContext.sessionManager
//        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
//        onDispose {
//            sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
//        }
//    }
//
//    LaunchedEffect(Unit) {
//        isSessionConnected.value = castContext.sessionManager.currentCastSession?.isConnected == true
//    }

    // Back press handler
    HandleTvBackKey {
        onNavigate("Home")
    }

    RememberBackPressManager(
        timeoutMs = 2000L,
        onExit = { onNavigate("Home") },
        showHint = {
            snackbarHostState.currentSnackbarData?.dismiss()
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Press back again to exit",
                    duration = SnackbarDuration.Short
                )
            }
        }
    )

    // UI Glow Effect
    val glowColors =
        listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                // LEFT: Filter
                IconButton(
                    onClick = { showModeDialog = true },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Filter Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // CENTER: Title (Perfect Center)
                Text(
                    text = "JTV-GO",
                    fontSize = 12.sp,
                    fontFamily = customFontFamily,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        shadow = Shadow(
                            color = glowColor.value,
                            blurRadius = 30f
                        )
                    )
                )

                // RIGHT: Settings
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    val castGlowColor = if (isSessionConnected.value) Color.Green else Color.Transparent

//                    if (false)
//                        AndroidView(
//                            modifier = Modifier
//                                .then(
//                                    if (isSessionConnected.value) {
//                                        Modifier
//                                            .shadow(
//                                                elevation = 8.dp,
//                                                shape = MaterialTheme.shapes.small,
//                                                ambientColor = castGlowColor,
//                                                spotColor = castGlowColor
//                                            )
//                                    } else Modifier
//                                ),
//                            factory = { ctx ->
//                                val themedContext = ContextThemeWrapper(
//                                    ctx,
//                                    R.style.Theme_JGO
//                                )
//
//                                MediaRouteButton(
//                                    themedContext,
//                                    null,
//                                    androidx.mediarouter.R.attr.mediaRouteButtonStyle
//                                ).apply {
//                                    CastButtonFactory.setUpMediaRouteButton(themedContext, this)
//                                }
//                            }
//                        )

                    IconButton(
                        onClick = { onNavigate("SettingsTV") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Settings,
                            contentDescription = "Settings Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (preferenceManager.myPrefs.customPlaylistSupport &&
                !preferenceManager.myPrefs.showPLAYLIST
            ) {
                Main_Layout_3rd(context, reloadTrigger = reloadChannelsTrigger)
            } else if (!preferenceManager.myPrefs.showAllTabs) {
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
                            .focusRequester(tabFocusRequester),
//                            .focusable(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                            tabs.forEachIndexed { index, tab ->
                                var isFocused by remember { mutableStateOf(false) }
                                val isActive = index == selectedTabIndex || isFocused

                                Tab(
                                    modifier = Modifier
                                        .onFocusChanged { focusState ->
                                            isFocused = focusState.isFocused
                                        }
                                        .scale(if (isFocused) 1.05f else 1.0f) // Subtle focus scale for TV
                                        .border(
                                            border = if (isFocused) BorderStroke(
                                                2.dp,
                                                GoldColor
                                            ) else BorderStroke(0.dp, Color.Transparent),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .background(
                                            color = when {
                                                isFocused -> GoldColor.copy(alpha = 0.25f)
                                                index == selectedTabIndex -> MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.15f
                                                )

                                                else -> Color.Transparent
                                            },
                                            shape = MaterialTheme.shapes.small
                                        ),
                                    selected = index == selectedTabIndex,
                                    onClick = {
                                        selectedTabIndex = index
                                    },
                                    text = {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.text,
                                                modifier = Modifier.size(20.dp),
                                                tint = when {
                                                    isFocused -> GoldColor
                                                    isActive -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = tab.text,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    fontSize = 12.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                    color = when {
                                                        isFocused -> GoldColor
                                                        index == selectedTabIndex -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }



                    // Tab Content
                    when (selectedTabIndex) {
                        0 -> Main_Layout(context, reloadTrigger = reloadChannelsTrigger)
                        1 -> Favorites_Layout(context, viewModel = tvViewModel, basefinURL = tvViewModel.basefinURL)
//                        2 -> Recent_Layout(context, viewModel = tvViewModel, basefinURL = tvViewModel.basefinURL)
                        2 -> SearchTabLayout(context, tabFocusRequester, tvViewModel)
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
            Log.d(
                "ZoneScreen",
                "Qualities: $selectedQualities, Categories: $selectedCategories, Languages: $selectedLanguages"
            )
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