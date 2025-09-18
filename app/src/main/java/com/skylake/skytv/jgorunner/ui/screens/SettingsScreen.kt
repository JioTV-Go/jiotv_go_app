package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Attribution
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrowserUpdated
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mediation
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ResetTv
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.platform.LocalContext
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
import com.skylake.skytv.jgorunner.ui.components.BackupDialog
import com.skylake.skytv.jgorunner.ui.components.JTVModeSelectorPopup
import com.skylake.skytv.jgorunner.ui.components.ModeSelectionDialog
import com.skylake.skytv.jgorunner.ui.components.restoreBackup
import android.os.Process
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import kotlin.system.exitProcess


@SuppressLint("UnrememberedMutableState", "AutoboxingStateCreation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    activity: ComponentActivity,
    checkForUpdates: () -> Unit,
    onNavigate: (String) -> Unit,
    isSwitchOnForAutoStartForeground: Boolean,
    onAutoStartForegroundSwitch: (Boolean) -> Unit,
) {
    // Initialize SkySharedPref
    val jtvConfigurationManager = JTVConfigurationManager.getInstance(activity)
    val preferenceManager = SkySharedPref.getInstance(activity)
    val focusRequester = remember { FocusRequester() }
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))

    var showNewBadge by remember { mutableStateOf(true) }

    fun applySettings() {
        preferenceManager.savePreferences()
    }

    // Retrieve saved switch states
    val selectedIndex by remember {
        mutableIntStateOf(preferenceManager.myPrefs.operationMODE)
    }
    var isSwitchOnForLOCAL by remember {
        mutableStateOf(preferenceManager.myPrefs.serveLocal)
    }
    var isLoginCheckEnabled by remember {
        mutableStateOf(preferenceManager.myPrefs.loginChk)
    }
    var isSwitchOnForEPG by remember {
        mutableStateOf(jtvConfigurationManager.jtvConfiguration.epg)
    }
    var isSwitchOnForDRM by remember {
        mutableStateOf(jtvConfigurationManager.jtvConfiguration.drm)
    }
    var isSwitchOnForAutoStartServer by remember {
        mutableStateOf(preferenceManager.myPrefs.autoStartServer)
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
    var isSwitchDarkMode by remember {
        mutableStateOf(preferenceManager.myPrefs.darkMODE)
    }
    var selectedIPTVTime by remember {
        mutableIntStateOf(preferenceManager.myPrefs.iptvLaunchCountdown)
    }
    var portNumber by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort)
    }

    var showPortDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showOperationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showBackupDialog by remember { mutableStateOf(false) }
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

    LaunchedEffect(isSwitchDarkMode) {
        preferenceManager.myPrefs.darkMODE = isSwitchDarkMode
        applySettings()
    }

    LaunchedEffect(isLoginCheckEnabled) {
        preferenceManager.myPrefs.loginChk = isLoginCheckEnabled
        applySettings()
    }

    LaunchedEffect(isSwitchOnForEPG) {
        jtvConfigurationManager.jtvConfiguration.epg = isSwitchOnForEPG
        jtvConfigurationManager.saveJTVConfiguration()
    }

    LaunchedEffect(isSwitchOnForDRM) {
        jtvConfigurationManager.jtvConfiguration.drm = isSwitchOnForDRM
        jtvConfigurationManager.saveJTVConfiguration()
    }

    LaunchedEffect(selectedIndex) {
        preferenceManager.myPrefs.operationMODE = selectedIndex
        applySettings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {

        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontFamily = customFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            style =  TextStyle.Default,
            modifier = Modifier.padding(bottom = 10.dp)
        )



        // Content
        LazyColumn(
            modifier = Modifier.padding(top = 0.dp)
        ) {


            item {
                HorizontalDividerLineTr()
            }


            item {

                val opMode =
                    if (preferenceManager.myPrefs.operationMODE == 0) "Simple" else "Expert"
                SettingItem(icon = Icons.Filled.Hub,
                    title = "Operation Mode : $opMode",
                    subtitle = "Select mode for app operation",
                    onClick = { showOperationDialog = true })
            }

            item {
                HorizontalDividerLineTrx("App Start")
            }


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
                            onAutoStartForegroundSwitch(isChecked)
                        })
                }
            }

            item {
                HorizontalDividerLineTrx("Server Config")
            }


            item {
                SettingSwitchItem(icon = Icons.Filled.Public,
                    title = "Use Server Publicly",
                    subtitle = "Allow access to server from other devices",
                    isChecked = !isSwitchOnForLOCAL,
                    onCheckedChange = { isChecked -> isSwitchOnForLOCAL = !isChecked })
            }

            item {
                SettingSwitchItem(
                    icon = Icons.Filled.Security,
                    title = "DRM",
                    subtitle =  "DRM toggle [chrome/firefox only]",
                    isChecked = isSwitchOnForDRM,
                    onCheckedChange = { isChecked -> isSwitchOnForDRM = isChecked },
                )
            }

            // Port Number Setting
            item {
                SettingItem(icon = Icons.Filled.Attribution,
                    title = "Server Port",
                    subtitle = "Current Port: $portNumber",
                    onClick = { showPortDialog = true })
            }
            item {
                SettingSwitchItem(
                    icon = Icons.Filled.VerifiedUser,
                    title = "Login Checker",
                    subtitle = if (isLoginCheckEnabled) "Enabled login checker" else "Disabled login checker",
                    isChecked = isLoginCheckEnabled,
                    onCheckedChange = { isChecked -> isLoginCheckEnabled = isChecked }
                )
            }

            item {
                HorizontalDividerLineTrx("IPTV")
            }

            item {
                SettingSwitchItem(
                    icon = Icons.Filled.BrowserUpdated,
                    title = "Enable EPG",
                    subtitle = "Electronic program guide generation",
                    isChecked = isSwitchOnForEPG,
                    onCheckedChange = { isChecked -> isSwitchOnForEPG = isChecked },
                )
            }
            item {
                SettingSwitchItem(
                    icon = Icons.Filled.LiveTv,
                    title = "Auto IPTV",
                    subtitle = "Automatically start IPTV on app start",
                    isChecked = isSwitchOnForAutoIPTV,
                    onCheckedChange = { isChecked ->
                        isSwitchOnForAutoIPTV = isChecked
                        val iptv = preferenceManager.myPrefs.iptvAppName
                        if (isChecked && ( iptv == null || iptv == "No IPTV")) {
                            val intent = Intent(activity, AppListActivity::class.java)
                            activity.startActivity(intent)
                        }
                    }
                )
            }
            val iptvNameSaved = preferenceManager.myPrefs.iptvAppName
            val iptvPKGNameSaved = preferenceManager.myPrefs.iptvAppPackageName
            val result = if (iptvPKGNameSaved == "tvzone" || iptvPKGNameSaved == "webtv") {
                "No IPTV"
            } else {
                iptvNameSaved
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingItem(
                    icon = Icons.Filled.ResetTv,
                    title = "Select Start IPTV",
                    subtitle = if (iptvNameSaved.isNullOrEmpty() || iptvNameSaved == "No IPTV") {
                        "Choose your preferred IPTV to start"
                    } else {
                        "Selected IPTV: $result"
                    },
                    onClick = {
                        val intent = Intent(activity, AppListActivity::class.java)
                        activity.startActivity(intent)
                    }
                )
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
                        text = "IPTV Redirect Time: $selectedIPTVTime sec",
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(value = selectedIPTVTime.toFloat(),
                    onValueChange = { selectedIPTVTime = it.toInt() },
                    valueRange = 2f..10f,
                    steps = 3, // for 2, 4, 6, 8, 10 seconds
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester) // Focus for keyboard handling
                        .focusable()
                        .onKeyEvent { event ->
                            when (event.nativeKeyEvent.keyCode) {
                                Key.DirectionRight.nativeKeyCode -> {
                                    selectedIPTVTime = (selectedIPTVTime + 1).coerceAtMost(10)
                                    true
                                }

                                Key.DirectionLeft.nativeKeyCode -> {
                                    selectedIPTVTime = (selectedIPTVTime - 1).coerceAtLeast(2)
                                    true
                                }

                                else -> false
                            }
                        })

            }
            item {
                SettingItem(
                    icon = Icons.Filled.Mediation,
                    title = "Configure WEBTV filters",
                    subtitle = "Select default language, category, and quality.",
                    onClick = {
                        showModeDialog = true
                        showNewBadge = false
                    }
                )
            }

            item {
                HorizontalDividerLineTrx("Miscellaneous")
            }

            if (true) {
                item {
                    SettingSwitchItem(icon = Icons.Filled.DarkMode,
                        title = "Always Dark Mode",
                        subtitle = "Toggle between dark and auto",
                        isChecked = isSwitchDarkMode,
                        onCheckedChange = { isChecked -> isSwitchDarkMode = isChecked })
                }
            }

            item {
                SettingSwitchItem(icon = Icons.Filled.SoupKitchen,
                    title = "Check for Auto Updates",
                    subtitle = "Check for updates when the app starts",
                    isChecked = isSwitchOnCheckForUpdate,
                    onCheckedChange = { isChecked -> isSwitchOnCheckForUpdate = isChecked })
            }

            item {
                SettingItem(icon = Icons.Filled.ArrowCircleUp,
                    title = "Check for Updates Now",
                    subtitle = "Update to the latest binary and application version.",
                    onClick = {
                        checkForUpdates()
                    })
            }

            if (false) {
                item {
                    SettingItem(
                        icon = Icons.Filled.Backup,
                        title = "Backup & Restore",
                        subtitle = "Securely back up and restore your data.",
                        showBadge = showNewBadge,
                        onClick = {
                            showBackupDialog = true
                            showNewBadge = false
                        }
                    )
                }

            }

            item {
                SettingItem(icon = Icons.Filled.Insights,
                    title = "Device Info & Logs",
                    subtitle = "View technical data and logs for debugging.",
                    showBadge = showNewBadge,
                    onClick = {
                       onNavigate("Info")
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

        // safely clearing cache before port change
        val sharedPref = context.getSharedPreferences("channel_cache", Context.MODE_PRIVATE)
        try {
            with(sharedPref.edit()) {
                remove("channels_json")
                apply()
                Log.d("DIX-SetSec", "Cleared channel cache")
            }
        } catch (e: Exception) {
            Log.e("DIX-SetSec", "Error message", e)
        }

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

    ModeSelectionDialog(
        showDialog = showModeDialog,
        onDismiss = { showModeDialog = false },
        onSelectionsMade = { selectedQuality, selectedCategory, selectedLanguage ->

            preferenceManager.myPrefs.filterQ = selectedQuality
            preferenceManager.myPrefs.filterC = selectedCategory
            preferenceManager.myPrefs.filterL = selectedLanguage
            preferenceManager.savePreferences()

            println("Quality: $selectedQuality, Category: $selectedCategory, Language: $selectedLanguage")
            Toast.makeText(activity, "[#] Updated Filters", Toast.LENGTH_LONG).show()

        }
    )

    BackupDialog(
        showDialog = showBackupDialog,
        context = context,
        onDismiss = { showBackupDialog = false },
        onBackup = { backupFile ->
            println("Backup created at: ${backupFile.absolutePath}")
        },
        onRestore = { backupFile ->
            restoreBackup(context, backupFile)
            println("Restore completed from: ${backupFile.absolutePath}")
        }
    )

    JTVModeSelectorPopup(
        context = context,
        isVisible = showOperationDialog,
        preferenceManager = preferenceManager,
        onModeSelected = {
            showOperationDialog = false
        },
        onDismiss = {
            showOperationDialog = false
        }
    )

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

//fun restartApp(context: Context) {
//    val intent = Intent(context, MainActivity::class.java)
//    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//    context.startActivity(intent)
//    (context as? Activity)?.finish()
//}

@SuppressLint("ServiceCast")
fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.set(
        AlarmManager.RTC,
        System.currentTimeMillis() + 100,
        pendingIntent
    )
    Process.killProcess(Process.myPid())
    exitProcess(0)
}


fun resetFunc(context: Context) {
    Toast.makeText(context, "[#] Clearing files.", Toast.LENGTH_LONG).show()
    JTVConfigurationManager.getInstance(context).deleteJTVConfiguration()
    SkySharedPref.getInstance(context).clearPreferences()
    context.filesDir.deleteRecursively()
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
fun HorizontalDividerLine() {
    HorizontalDivider(
        thickness = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun HorizontalDividerLineTr() {
    HorizontalDivider(
        thickness = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

@Composable
fun HorizontalDividerLineTrx(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color.Gray
        )
    }
}
