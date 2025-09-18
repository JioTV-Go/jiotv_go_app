package com.skylake.skytv.jgorunner.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.skylake.skytv.jgorunner.R
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.animation.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.Support
import androidx.compose.runtime.*
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.components.ButtonContent

import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.skylake.skytv.jgorunner.activities.CastActivity
import com.skylake.skytv.jgorunner.services.player.LandingPage
import com.skylake.skytv.jgorunner.ui.tvhome.changeIconTOFirst
import com.skylake.skytv.jgorunner.ui.tvhome.changeIconToSecond

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

    // Update shared preference when switch states change
    LaunchedEffect(isSwitchForExp) {
        preferenceManager.myPrefs.expDebug = isSwitchForExp
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
                onCheckedChange = { isChecked -> isSwitchForExp = isChecked
                    val status = if (isSwitchForExp) "enabled" else "disabled"
//                    if (isSwitchForExp) {
//                        changeIconToSecond(context)
//                    } else {
//                        changeIconTOFirst(context)
//                    }
                    Toast.makeText(context, "Experimental features $status", Toast.LENGTH_SHORT).show()
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
        }

    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
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
                    Toast.makeText(context, "Experimental features $status!", Toast.LENGTH_SHORT).show()
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
            handleButton2Click(context,onNavigate)
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
fun DebugItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontWeight = FontWeight.Bold)
                if (showBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(
                                color = Color.Red.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
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





fun handleButton1Click(context: Context, onNavigate: (String) -> Unit) {
    Toast.makeText(context, "Caution: Experimental!\n May be unstable.", Toast.LENGTH_SHORT).show()
    onNavigate("Runner")
}

fun handleButton2Click(context: Context, onNavigate: (String) -> Unit) {
    Toast.makeText(context, "Retrieving system information...", Toast.LENGTH_SHORT).show()
    onNavigate("Info")
}

fun handleButton3Click(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/3Uc1usW"))
    context.startActivity(intent)
}

fun handleButton4Click(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/JTV-GO-Server"))
    context.startActivity(intent)
}

fun handleButton5Click(context: Context, onNavigate: (String) -> Unit) {
    onNavigate("Login")
    Toast.makeText(context, "Pending Implementation", Toast.LENGTH_SHORT).show()
}

fun handleButton6Click(context: Context) {
//    Toast.makeText(context, "Demo Stream Playing", Toast.LENGTH_SHORT).show()
//    val intent = Intent(context, ExoplayerActivityPass::class.java)
//    intent.putExtra("video_url", "http://localhost:5350/live/144.m3u8")
//    intent.putExtra("zone", "TV")
//    intent.putExtra("logo_url", "TV")
//    intent.putExtra("ch_name", "Colors TV")
//    intent.putExtra("current_channel_index", 0)
//    context.startActivity(intent)

    val intent = Intent(context, LandingPage::class.java)
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