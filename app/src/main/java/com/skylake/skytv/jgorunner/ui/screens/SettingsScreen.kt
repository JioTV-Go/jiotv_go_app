package com.skylake.skytv.jgorunner.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ResetTv
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.AppListActivity
import com.skylake.skytv.jgorunner.activities.MainActivity
import com.skylake.skytv.jgorunner.core.data.JTVConfigurationManager
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    activity: ComponentActivity,
    checkForUpdates: () -> Unit,
    requestOverlayPermission: () -> Unit
) {
    // Initialize SkySharedPref
    val jtvConfigurationManager = JTVConfigurationManager.getInstance(activity)
    val preferenceManager = SkySharedPref.getInstance(activity)
    val focusRequester = remember { FocusRequester() }
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))

    fun applySettings() {
        preferenceManager.savePreferences()
    }
    // Retrieve saved switch states
    var isSwitchOnForLOCAL by remember {
        mutableStateOf(preferenceManager.myPrefs.serveLocal)
    }
    var isSwitchOnForEPG by remember {
        mutableStateOf(jtvConfigurationManager.jtvConfiguration.epg)
    }
    var isSwitchOnForAutoStartServer by remember {
        mutableStateOf(preferenceManager.myPrefs.autoStartServer)
    }
    var isSwitchOnForAutoStartForeground by remember {
        mutableStateOf(preferenceManager.myPrefs.autoStartOnBootForeground)
    }
    var isSwitchOnForAutoStartOnBoot by remember {
        mutableStateOf(preferenceManager.myPrefs.autoStartOnBoot)
    }
    var isSwitchOnForAutoIPTV by remember {
        mutableStateOf(preferenceManager.myPrefs.autoStartIPTV)
    }
    var isSwitchOnCheckForUpdate by remember {
        mutableStateOf(preferenceManager.myPrefs.enableAutoUpdate)
    }
    var selectedIPTVTime by remember {
        mutableIntStateOf(preferenceManager.myPrefs.iptvLaunchCountdown)
    }
    var portNumber by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort)
    }

    var showPortDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showRestartAppDialog by remember { mutableStateOf(false) }

    // Update shared preference when switch states change
    LaunchedEffect(isSwitchOnForLOCAL) {
        preferenceManager.myPrefs.serveLocal = isSwitchOnForLOCAL
        applySettings()
    }

    LaunchedEffect(isSwitchOnForAutoStartServer) {
        preferenceManager.myPrefs.autoStartServer = isSwitchOnForAutoStartServer
        applySettings()
    }

    LaunchedEffect(isSwitchOnForAutoStartOnBoot) {
        preferenceManager.myPrefs.autoStartOnBoot = isSwitchOnForAutoStartOnBoot
        applySettings()
    }

    LaunchedEffect(isSwitchOnForAutoIPTV) {
        preferenceManager.myPrefs.autoStartIPTV = isSwitchOnForAutoIPTV
        applySettings()
    }

    LaunchedEffect(selectedIPTVTime) {
        preferenceManager.myPrefs.iptvLaunchCountdown = selectedIPTVTime
        applySettings()
    }

    // Update port number when changed
    LaunchedEffect(portNumber) {
        if (portNumber !in 1000..9999) {
            portNumber = 5350
        }
        preferenceManager.myPrefs.jtvGoServerPort = portNumber
        applySettings()
    }

    LaunchedEffect(isSwitchOnCheckForUpdate) {
        preferenceManager.myPrefs.enableAutoUpdate = isSwitchOnCheckForUpdate
        applySettings()
    }

    LaunchedEffect(isSwitchOnForAutoStartForeground) {
        if (isSwitchOnForAutoStartForeground){
            requestOverlayPermission()
            return@LaunchedEffect
        }
        preferenceManager.myPrefs.autoStartOnBootForeground = isSwitchOnForAutoStartForeground
        applySettings()
    }

    LaunchedEffect(isSwitchOnForEPG) {
        jtvConfigurationManager.jtvConfiguration.epg = isSwitchOnForEPG
        jtvConfigurationManager.saveJTVConfiguration()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text(text = "Settings", fontSize = 30.sp, fontFamily = customFontFamily) },
        )

        // Content
        LazyColumn(
            modifier = Modifier.padding(top = 0.dp)
        ) {
            item {
                SettingSwitchItem(icon = Icons.Filled.Pix,
                    title = "Auto Start Server",
                    subtitle = "Automatically start server on app start",
                    isChecked = isSwitchOnForAutoStartServer,
                    onCheckedChange = { isChecked -> isSwitchOnForAutoStartServer = isChecked })
            }
            item {
                SettingSwitchItem(icon = Icons.Filled.Stream,
                    title = "Auto Start on Boot",
                    subtitle = "Automatically start server on boot",
                    isChecked = isSwitchOnForAutoStartOnBoot,
                    onCheckedChange = { isChecked -> isSwitchOnForAutoStartOnBoot = isChecked })
            }

            // Conditionally render the Boot switch
            if (isSwitchOnForAutoStartOnBoot) {
                item {
                    val stateBG =
                        if (isSwitchOnForAutoStartForeground) "foreground" else "background"
                    SettingSwitchItem(icon = Icons.Filled.CenterFocusStrong,
                        title = "Server Start Mode",
                        subtitle = "The server will start in $stateBG mode",
                        isChecked = isSwitchOnForAutoStartForeground,
                        onCheckedChange = { isChecked ->
                            isSwitchOnForAutoStartForeground = isChecked
                        })
                }
            }
            item {
                SettingSwitchItem(icon = Icons.Filled.Public,
                    title = "Use Server Publicly",
                    subtitle = "Allow access to server from other devices",
                    isChecked = !isSwitchOnForLOCAL,
                    onCheckedChange = { isChecked -> isSwitchOnForLOCAL = !isChecked })
            }
            // Port Number Setting
            item {
                SettingItem(icon = Icons.Filled.Settings,
                    title = "Server Port",
                    subtitle = "Current Port: $portNumber",
                    onClick = { showPortDialog = true })
            }
            item {
                SettingSwitchItem(
                    icon = Icons.Filled.Info,
                    title = "Enable EPG",
                    subtitle = "Electronic program guide generation",
                    isChecked = isSwitchOnForEPG,
                    onCheckedChange = { isChecked -> isSwitchOnForEPG = isChecked },
                )
            }
            item {
                SettingSwitchItem(icon = Icons.Filled.LiveTv,
                    title = "Auto IPTV",
                    subtitle = "Automatically start IPTV on app start",
                    isChecked = isSwitchOnForAutoIPTV,
                    onCheckedChange = { isChecked -> isSwitchOnForAutoIPTV = isChecked })
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingItem(icon = Icons.Filled.ResetTv,
                    title = "Select Start IPTV",
                    subtitle = "Choose your preferred IPTV to start",
                    onClick = {
                        val intent = Intent(activity, AppListActivity::class.java)
                        activity.startActivity(intent)
                    })
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timelapse,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "IPTV Redirect Time: ${selectedIPTVTime / 1000} sec",
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(value = selectedIPTVTime.toFloat(),
                    onValueChange = { selectedIPTVTime = it.toInt() },
                    valueRange = 2000f..10000f,
                    steps = 5, // for 2, 4, 6, 8, 10 seconds
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester) // Focus for keyboard handling
                        .focusable()
                        .onKeyEvent { event ->
                            when (event.nativeKeyEvent.keyCode) {
                                Key.DirectionRight.nativeKeyCode -> {
                                    selectedIPTVTime = (selectedIPTVTime + 1000).coerceAtMost(10000)
                                    true
                                }

                                Key.DirectionLeft.nativeKeyCode -> {
                                    selectedIPTVTime = (selectedIPTVTime - 1000).coerceAtLeast(2000)
                                    true
                                }

                                else -> false
                            }
                        })

            }
            item {
                SettingSwitchItem(icon = Icons.Filled.SoupKitchen,
                    title = "Check for updates",
                    subtitle = "Check for updates when the app starts",
                    isChecked = isSwitchOnCheckForUpdate,
                    onCheckedChange = { isChecked -> isSwitchOnCheckForUpdate = isChecked })
            }
            item {
                SettingItem(icon = Icons.Filled.ArrowCircleUp,
                    title = "Update Binary",
                    subtitle = "Update to the latest binary version.",
                    onClick = {
                        checkForUpdates()
                    })
            }
            item {
                SettingItem(icon = Icons.Filled.RestartAlt,
                    title = "Reset All Settings",
                    subtitle = "Useful for troubleshooting and resolving issues.",
                    onClick = {
                        showRestartAppDialog = true
                    })
            }
        }
    }

    // Port Number Dialog
    if (showPortDialog) {
        Dialog(
            onDismissRequest = { showPortDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Set Server Port", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    var currentPort by remember { mutableStateOf(portNumber.toString()) }
                    TextField(
                        value = currentPort,
                        onValueChange = { currentPort = it },
                        label = { Text(text = "Port Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                        ), // Numeric keyboard + Done action
                        keyboardActions = KeyboardActions(onDone = {
                            if (currentPort.toIntOrNull() != null && currentPort.toIntOrNull() in 1000..9999) {
                                portNumber = currentPort.toInt()
                                showPortDialog = false
                                showRestartDialog = true
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { showPortDialog = false }) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            if (currentPort.toIntOrNull() != null && currentPort.toIntOrNull() in 1000..9999) {
                                portNumber = currentPort.toInt()
                                showPortDialog = false
                                showRestartDialog = true
                            }
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }


    // Restart Required Dialog
    if (showRestartDialog) {
        Dialog(
            onDismissRequest = { showRestartDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Restart Required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The server port has been changed. Please restart the device for the changes to take effect.",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { showRestartDialog = false }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    // Restart App Dialog
    if (showRestartAppDialog) {
        Dialog(
            onDismissRequest = { showRestartAppDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Restart Required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The app needs to be restarted to reset all app settings. Please restart the app.",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = {
                            showRestartAppDialog = false
                            resetFunc(activity)
                            restartApp(activity)
                        }) {
                            Text("Restart")
                        }
                    }
                }
            }
        }
    }
}

fun restartApp(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    (context as? Activity)?.finish()
}


fun resetFunc(context: Context) {
    Toast.makeText(context, "[#] Clearing files.", Toast.LENGTH_LONG).show()

    // Delete folder in external storage
    val externalStoragePath = Environment.getExternalStorageDirectory().path
    val folder = File("$externalStoragePath/.jiotv_go")

    if (folder.exists() && folder.isDirectory) {
        folder.deleteRecursively()
    }

    // Delete binary file in internal storage
    val binaryFile = File(context.filesDir, "majorbin")
    if (binaryFile.exists()) {
        binaryFile.delete()
    } else {
        Toast.makeText(context, "Reset successfully.", Toast.LENGTH_SHORT).show()
    }
}


@Composable
fun SettingSwitchItem(
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
fun SettingItem(
    icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit
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
        Column {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
