package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.LogoDev
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.Support
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.CastActivity
import com.skylake.skytv.jgorunner.activities.setup_wizard.SetupWizardActivity
import com.skylake.skytv.jgorunner.core.update.DownloadModelNew
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.LandingPage
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus
import com.skylake.skytv.jgorunner.ui.components.ButtonContent
import com.skylake.skytv.jgorunner.ui.components.PreReleaseBinary
import com.skylake.skytv.jgorunner.ui.components.ProgressPopup
import com.skylake.skytv.jgorunner.ui.components.performSelectedBinaryUpdate
import com.skylake.skytv.jgorunner.ui.tvhome.changeIconTOFirst
import com.skylake.skytv.jgorunner.ui.tvhome.changeIconToSecond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@Composable
fun DebugScreen(context: Context, onNavigate: (String) -> Unit) {
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))
    var isGlowing by remember { mutableStateOf(false) }
    val glowColors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Cyan,
        Color.Magenta
    )
    val glowColor = remember { Animatable(glowColors.first()) }
    val preferenceManager = SkySharedPref.getInstance(context)

    fun applySettings() {
        preferenceManager.savePreferences()
    }

    // Retrieve saved switch states
    var isSwitchForExp by remember {
        mutableStateOf(preferenceManager.myPrefs.expDebug)
    }

    val isCustomPlaylistEnabled = remember {
        mutableStateOf(preferenceManager.myPrefs.customPlaylistSupport)
    }

    val isGenericIcon = remember {
        mutableStateOf(preferenceManager.myPrefs.genericTvIcon)
    }

    // Experimental: Card UI experiment (single toggle)
    val isCardUiExperiment = remember { mutableStateOf(preferenceManager.myPrefs.cardUiExperiment) }

    var isUsingPreRelease by remember {
        mutableStateOf(preferenceManager.myPrefs.preRelease)
    }


    var showPreReleaseBinaryDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<DownloadModelNew?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }


    // Update shared preference when switch states change
    LaunchedEffect(isSwitchForExp) {
        preferenceManager.myPrefs.expDebug = isSwitchForExp
        applySettings()
    }

    LaunchedEffect(isUsingPreRelease) {
        preferenceManager.myPrefs.preRelease = isUsingPreRelease
        applySettings()
    }


    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            repeat(3) {
                val savedSwitchState = preferenceManager.myPrefs.serveLocal
                Log.d("PreferenceCheck", "isFlagSetForLOCAL: $savedSwitchState")
                isGlowing = savedSwitchState
                delay(10000)
            }
        }
    }

    LaunchedEffect(Unit) {
        var currentIndex = 0
        while (true) {
            val nextIndex = (currentIndex + 1) % glowColors.size
            glowColor.animateTo(
                targetValue = glowColors[nextIndex],
                animationSpec = tween(durationMillis = 1000)
            )
            currentIndex = nextIndex
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        item {
            Text(
                text = "JTV-GO SERVER",
                fontSize = 24.sp,
                fontFamily = customFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                style = if (isGlowing) {
                    TextStyle(
                        shadow = Shadow(
                            color = glowColor.value,
                            blurRadius = 30f,
                            offset = Offset(0f, 0f)
                        )
                    )
                } else {
                    TextStyle.Default
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button5(context, onNavigate)
                Button6(context)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button7(context, onNavigate)
                Button8(context)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button1(context, onNavigate)
                Button2(context, onNavigate)
                Button3(context)
                Button4(context)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            HorizontalDividerLineTr()
        }

        item {
            DebugSwitchItem(
                icon = Icons.Filled.Api,
                title = "Experimental Features",
                subtitle = if (isSwitchForExp) "Enabled experimental features" else "Disabled experimental features",
                isChecked = isSwitchForExp,
                onCheckedChange = { isChecked ->
                    isSwitchForExp = isChecked
                    val status = if (isSwitchForExp) "enabled" else "disabled"
                    Toast.makeText(context, "Experimental features $status", Toast.LENGTH_SHORT)
                        .show()
                })
        }

        if (isSwitchForExp) {
            item {
                DebugSwitchItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    title = "Custom Playlist Support",
                    subtitle = if (isCustomPlaylistEnabled.value)
                        "Enabled - You can load custom M3U playlists"
                    else
                        "Disabled - Using default channel list",
                    isChecked = isCustomPlaylistEnabled.value,
                    onCheckedChange = { checked ->
                        isCustomPlaylistEnabled.value = checked
                        preferenceManager.myPrefs.customPlaylistSupport = checked
                        applySettings()
                        Toast.makeText(
                            context,
                            "Custom Playlist Support ${if (checked) "enabled" else "disabled"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // Experimental: Card UI Experiment (single toggle)
            item {
                DebugSwitchItem(
                    icon = Icons.Filled.Terrain,
                    title = "Card UI (TV)",
                    subtitle = if (isCardUiExperiment.value)
                        "Enabled - Card UI options visible"
                    else
                        "Disabled - Card UI options hidden",
                    isChecked = isCardUiExperiment.value,
                    onCheckedChange = { checked ->
                        isCardUiExperiment.value = checked
                        preferenceManager.myPrefs.cardUiExperiment = checked
                        // On enable: only reveal dropdown (do not change layout)
                        // On disable: hide dropdown and reset to Default
                        if (!checked) {
                            preferenceManager.myPrefs.tvLayoutMode = "Default"
                        }
                        applySettings()
                        Toast.makeText(
                            context,
                            "Card UI experiment ${if (checked) "enabled" else "disabled"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            item {
                DebugSwitchItem(
                    icon = Icons.Default.Tv,
                    title = "Generic TV Icon",
                    subtitle = if (isGenericIcon.value)
                        "Enabled - Shows a generic TV icon"
                    else
                        "Disabled - Shows a default JTV-GO icon",
                    isChecked = isGenericIcon.value,
                    onCheckedChange = { checked ->
                        isGenericIcon.value = checked
                        preferenceManager.myPrefs.genericTvIcon = checked
                        applySettings()
                        if (checked) {
                            changeIconToSecond(context)
                        } else {
                            changeIconTOFirst(context)
                        }
                        Toast.makeText(
                            context,
                            "Generic TV Icon ${if (checked) "enabled" else "disabled"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // Experimental: PiP Toggle
            item {
                val enablePip = remember { mutableStateOf(preferenceManager.myPrefs.enablePip) }
                DebugSwitchItem(
                    icon = Icons.Default.PictureInPicture,
                    title = "Picture-in-Picture",
                    subtitle = if (enablePip.value) "Enabled: PiP button, Back-to-PiP, auto-PiP" else "Disabled: no PiP UI or actions",
                    isChecked = enablePip.value,
                    onCheckedChange = { checked ->
                        enablePip.value = checked
                        preferenceManager.myPrefs.enablePip = checked
                        applySettings()
                        // If PiP is being disabled, ensure any active PiP and playback are stopped
                        if (!checked) {
                            try {
                                PlayerCommandBus.requestStopPlayback()
                                PlayerCommandBus.requestClosePip()
                            } catch (_: Exception) { /* no-op */
                            }
                        }
                        Toast.makeText(
                            context,
                            "Picture-in-Picture Mode ${if (checked) "enabled" else "disabled"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }



            item {
                DebugItemCust(
                    icon = Icons.Default.LogoDev,
                    title = "Use Pre-release Binary",
                    subtitle = {
                        Text(
                            text = if (isUsingPreRelease)
                                "Using Pre-release Binary"
                            else
                                "Using Default Binary",
                            fontWeight = if (isUsingPreRelease) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUsingPreRelease) Color.Red else Color.Gray,
                            fontSize = 12.sp
                        )
                    },
                    onClick2 = {
                        Log.d("DIX", "PREBin Updater")
                        showPreReleaseBinaryDialog = true
                    },
                    enabled = true,
                    isChecked = isUsingPreRelease
                )
            }


        }

    }

    if (showPreReleaseBinaryDialog) {
        PreReleaseBinary(
            isVisible = true,
            onDismiss = { showPreReleaseBinaryDialog = false },
            onConfirm = { ctx, selectedRelease ->
                CoroutineScope(Dispatchers.Main).launch {
                    val success = performSelectedBinaryUpdate(ctx, selectedRelease) { status ->
                        downloadProgress = status
                    }
                    if (preferenceManager.myPrefs.jtvGoBinaryVersion?.contains(
                            "develop",
                            ignoreCase = true
                        ) == true
                    ) {
                        preferenceManager.myPrefs.preRelease = true
                        applySettings()
                        isUsingPreRelease = preferenceManager.myPrefs.preRelease
                    } else {
                        preferenceManager.myPrefs.preRelease = false
                        applySettings()
                    }
                    Toast.makeText(
                        ctx, if (success) {
                            "Download complete"
                        } else "Download failed", Toast.LENGTH_LONG
                    ).show()

                    downloadProgress = null
                }
                showPreReleaseBinaryDialog = false
            },
            onResetToStable = {
                preferenceManager.myPrefs.enableAutoUpdate = true
                preferenceManager.myPrefs.jtvGoBinaryName = null
                preferenceManager.myPrefs.jtvGoBinaryVersion = "v0.0.0"
                preferenceManager.savePreferences()
                showRestartDialog = true
            }
        )
    }

    if (downloadProgress != null) {
        ProgressPopup(
            fileName = downloadProgress!!.fileName,
            currentProgress = downloadProgress!!.progress,
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart Required") },
            text = { Text("A binary change has been detected. Restart the app now to apply the update?") },
            confirmButton = {
                Button(onClick = {
                    showRestartDialog = false
                    restartAppFast(context)
                }) {
                    Text("Restart")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showRestartDialog = false
                    showPreReleaseBinaryDialog = false
                }) {
                    Text("Later")
                }
            }
        )
    }
}

fun restartAppFast(context: Context) {
    try {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)

        if (intent != null) {
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            val mainIntent = Intent.makeRestartActivityTask(intent.component)
            context.startActivity(mainIntent)

            Thread.sleep(100)

            Runtime.getRuntime().exit(0)
        } else {
            Log.e("restartAppFast", "Launch intent is null")
        }
    } catch (e: Exception) {
        Log.e("restartAppFast", "Failed to restart app", e)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.Button1(context: Context, onNavigate: (String) -> Unit) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val preferenceManager = SkySharedPref.getInstance(context)

    Box(
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            }
            .background(
                color = buttonColor.value,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = {
                    handleButton1Click(context, onNavigate)
                },
                onLongClick = {
                    val current = preferenceManager.myPrefs.expDebug
                    preferenceManager.myPrefs.expDebug = !current
                    val status = if (!current) "enabled" else "disabled"
                    Toast
                        .makeText(context, "Experimental features $status!", Toast.LENGTH_SHORT)
                        .show()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        ButtonContent("Runner", Icons.AutoMirrored.Filled.DirectionsRun)
    }
}


@Composable
fun RowScope.Button2(context: Context, onNavigate: (String) -> Unit) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val borderColor = remember { mutableStateOf(Color.Transparent) }

    Button(
        onClick = {
            handleButton2Click(context, onNavigate)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    borderColor.value = Color.Green
                    colorSECOND
                } else {
                    borderColor.value = Color.Transparent
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Info & Log", Icons.Sharp.Info)
    }
}

@Composable
fun RowScope.Button3(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val colorBORDER = Color(0xFFFFD700)
    val isFocused = remember { mutableStateOf(false) }

    Button(
        onClick = { handleButton3Click(context) },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                buttonColor.value = if (focusState.isFocused) colorSECOND else colorPRIME
            },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused.value) BorderStroke(2.dp, colorBORDER) else null,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Support", Icons.Sharp.Support)
    }
}


@Composable
fun RowScope.Button4(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = { handleButton4Click(context) },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("GitHub", Icons.Default.Verified)
    }
}

@Composable
fun RowScope.Button5(context: Context, onNavigate: (String) -> Unit) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton5Click(context, onNavigate)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Login", Icons.Default.Verified)
    }
}

@Composable
fun RowScope.Button6(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton6Click(context)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Sonata UI", Icons.Default.PlayCircleOutline)
    }
}

@Composable
fun RowScope.Button7(context: Context, onNavigate: (String) -> Unit) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton7Click(context, onNavigate)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("TV UI beta", Icons.Default.Terrain)
    }
}

@Composable
fun RowScope.Button8(context: Context) {
    val colorPRIME = Color(0xFFA5D6A7)
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton8Click(context)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("CAST", Icons.Default.Cast)
    }
}


@Composable
fun DebugSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.25f)
            .clickable {
                if (enabled) onCheckedChange(!isChecked)
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = { onCheckedChange(it) }, enabled = enabled)
    }
}


@Composable
fun DebugItemCust(
    icon: ImageVector,
    title: String,
    subtitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
//    onClick2: (),
    isChecked: Boolean,
    enabled: Boolean = true,
    onClick2: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick2() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold)
            subtitle()
        }
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = { onClick2() }, enabled = enabled)
    }
}


fun handleButton1Click(context: Context, onNavigate: (String) -> Unit) {
    Toast.makeText(context, "Caution: Experimental!\n May be unstable.", Toast.LENGTH_SHORT).show()
    onNavigate("Runner")
}

fun handleButton2Click(context: Context, onNavigate: (String) -> Unit) {
    Toast.makeText(context, "Retrieving system information...", Toast.LENGTH_SHORT).show()
    onNavigate("Info")
}

fun handleButton3Click(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, "https://bit.ly/3Uc1usW".toUri())
    context.startActivity(intent)
}

fun handleButton4Click(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, "https://bit.ly/JTV-GO-Server".toUri())
    context.startActivity(intent)
}

fun handleButton5Click(context: Context, onNavigate: (String) -> Unit) {
    onNavigate("Login")
    Toast.makeText(context, "Pending Implementation", Toast.LENGTH_SHORT).show()
}

fun handleButton6Click(context: Context) {
//    Toast.makeText(context, "Demo Stream Playing", Toast.LENGTH_SHORT).show()
//    val intent = Intent(context, ExoplayerActivity::class.java)
//    intent.putExtra("video_url", "http://localhost:5350/live/144.m3u8")
//    intent.putExtra("zone", "TV")
//    intent.putExtra("logo_url", "TV")
//    intent.putExtra("ch_name", "Colors TV")
//    intent.putExtra("current_channel_index", 0)
//    context.startActivity(intent)

    val intent = Intent(context, SetupWizardActivity::class.java)
    context.startActivity(intent)
//
}

fun handleButton7Click(context: Context, onNavigate: (String) -> Unit) {
    onNavigate("Zone")
    Toast.makeText(context, "Pending Implementation", Toast.LENGTH_SHORT).show()
}

fun handleButton8Click(context: Context) {
    val intent = Intent(context, CastActivity::class.java)
    context.startActivity(intent)
}