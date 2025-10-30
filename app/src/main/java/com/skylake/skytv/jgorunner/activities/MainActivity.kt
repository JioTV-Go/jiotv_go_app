package com.skylake.skytv.jgorunner.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skylake.skytv.jgorunner.BuildConfig
import com.skylake.skytv.jgorunner.activities.setup_wizard.SetupWizardActivity
import com.skylake.skytv.jgorunner.core.checkServerStatus
import com.skylake.skytv.jgorunner.core.data.JTVConfigurationManager
import com.skylake.skytv.jgorunner.core.execution.runBinary
import com.skylake.skytv.jgorunner.core.execution.stopBinary
import com.skylake.skytv.jgorunner.core.update.ApplicationUpdater
import com.skylake.skytv.jgorunner.core.update.BinaryUpdater
import com.skylake.skytv.jgorunner.core.update.DownloadModelNew
import com.skylake.skytv.jgorunner.core.update.DownloadProgress
import com.skylake.skytv.jgorunner.core.update.SemanticVersionNew
import com.skylake.skytv.jgorunner.core.update.Status
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.BinaryService
import com.skylake.skytv.jgorunner.services.player.LandingPage
import com.skylake.skytv.jgorunner.ui.components.BottomNavigationBar
import com.skylake.skytv.jgorunner.ui.components.CustPopup
import com.skylake.skytv.jgorunner.ui.components.JTVModeSelectorPopup
import com.skylake.skytv.jgorunner.ui.components.LoginPopup
import com.skylake.skytv.jgorunner.ui.components.ProgressPopup
import com.skylake.skytv.jgorunner.ui.components.RedirectPopup
import com.skylake.skytv.jgorunner.ui.screens.CastScreen
import com.skylake.skytv.jgorunner.ui.screens.DebugScreen
import com.skylake.skytv.jgorunner.ui.screens.HomeScreen
import com.skylake.skytv.jgorunner.ui.screens.InfoScreen
import com.skylake.skytv.jgorunner.ui.screens.LoginScreen
import com.skylake.skytv.jgorunner.ui.screens.LoginScreenPop
import com.skylake.skytv.jgorunner.ui.screens.RunnerScreen
import com.skylake.skytv.jgorunner.ui.screens.SettingsScreen
import com.skylake.skytv.jgorunner.ui.screens.ZoneScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.Inet4Address
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "JTVGo::MainActivity"
    }

    private var selectedBinaryName by mutableStateOf("JTV-GO SERVER")
    private lateinit var preferenceManager: SkySharedPref

    // SharedPreferences for saving binary selection
    private var outputText by mutableStateOf("ℹ️ Output logs")
    private var currentScreen by mutableStateOf("Home") // Manage current screen

    private val executor = Executors.newSingleThreadExecutor()
    private var showBinaryUpdatePopup by mutableStateOf(false)
    private var showAppUpdatePopup by mutableStateOf(false)
    private var showLoginPopup by mutableStateOf(false)
    private var showLoginPopupD by mutableStateOf(false)
    private var isServerRunning by mutableStateOf(false)

    private var isGlowBox by mutableStateOf(false)

    private var showOverlayPermissionPopup by mutableStateOf(false)

    private var showRedirectPopup by mutableStateOf(false)
    private var shouldLaunchIPTV by mutableStateOf(false)
    private var countdownJob: Job? = null

    private var downloadProgress by mutableStateOf<DownloadProgress?>(null)
    private var isSwitchOnForAutoStartForeground by mutableStateOf(false)

    private var showOperationDialog by mutableStateOf(false)

    private var isSwitchDarkMode by mutableStateOf(false)

    override fun onStart() {
        super.onStart()
        runOnceAfterAppUpgrade()
        preferenceManager = SkySharedPref.getInstance(this)
        BinaryUpdater.init(this)

// DEL----------------------------------------------------------

//        val intent = Intent(this, SetupWizardActivity::class.java)
//        this.startActivity(intent)
//        currentScreen = "Debug"

// DEL----------------------------------------------------------

        val appPackageName = preferenceManager.myPrefs.iptvAppPackageName

        if (!appPackageName.isNullOrEmpty()) {
            if (appPackageName == "tvzone") {
                preferenceManager.myPrefs.autoStartIPTV = false
                currentScreen = "Zone"
            }
        }

        if (preferenceManager.myPrefs.jtvGoBinaryVersion?.contains(
                "develop",
                ignoreCase = true
            ) == true
        ) {
            preferenceManager.myPrefs.preRelease = true
            preferenceManager.savePreferences()
        } else {
            preferenceManager.myPrefs.preRelease = false
            preferenceManager.savePreferences()
        }

        if (preferenceManager.myPrefs.iptvLaunchCountdown == 0) {
            preferenceManager.myPrefs.iptvLaunchCountdown = 4
            preferenceManager.myPrefs.enableAutoUpdate = true
            preferenceManager.myPrefs.loginChk = true
            preferenceManager.myPrefs.jtvGoServerPort = 5350
            preferenceManager.myPrefs.jtvGoBinaryVersion = "v0.0.0"
            preferenceManager.myPrefs.filterQ = ""
            preferenceManager.myPrefs.filterL = ""
            preferenceManager.myPrefs.filterC = ""
            preferenceManager.myPrefs.filterLX = ""
            preferenceManager.myPrefs.filterCX = ""
            preferenceManager.myPrefs.filterLI = ""
            preferenceManager.myPrefs.filterCI = ""
            preferenceManager.myPrefs.filterQX = "auto"
            preferenceManager.myPrefs.operationMODE = -1
            preferenceManager.myPrefs.selectedScreenTV = "0"
            preferenceManager.myPrefs.selectedRemoteNavTV = "0"
            preferenceManager.savePreferences()
        }

        JTVConfigurationManager.getInstance(this).saveJTVConfiguration()
        isServerRunning = BinaryService.isRunning

        if (preferenceManager.myPrefs.setupPending) {
            val intent = Intent(this, SetupWizardActivity::class.java)
            this.startActivity(intent)

            if (isServerRunning) {
                stopBinary(context = this, onBinaryStopped = {
                    isServerRunning = false
                    outputText = "Server stopped"
                })
            }

            countdownJob?.cancel()
            countdownJob = null
            preferenceManager.myPrefs.autoStartServer = false
            preferenceManager.savePreferences()
            finish()
            return
        }

        if (isServerRunning) {
            BinaryService.instance?.binaryOutput?.observe(this) {
                outputText = it
            }
        }

        // Check if server should start automatically
        val isFlagSetForAutoStartServer = preferenceManager.myPrefs.autoStartServer
        if (isFlagSetForAutoStartServer) {
            Log.d(TAG, "Starting server automatically")
            val arguments = emptyArray<String>()
            runBinary(
                activity = this,
                arguments = arguments,
                onRunSuccess = {
                    onJTVServerRun()
                },
                onOutput = { output ->
                    Log.d(TAG, output)
                    outputText = output
                }
            )
        }
    }

    private fun runOnceAfterAppUpgrade() {
        val skySharedPref = SkySharedPref.getInstance(this)
        val prefs = getSharedPreferences("app_update_prefs", MODE_PRIVATE)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        val lastVersion = prefs.getLong("last_version_code", -1L)

        if (currentVersion > lastVersion) {
            Log.d("App-DIX", "App updated")


            val backupPrefs = skySharedPref.myPrefs.copy()

            skySharedPref.clearPreferences()
            skySharedPref.myPrefs = skySharedPref.myPrefs.copy(
                serveLocal = backupPrefs.serveLocal,
                autoStartServer = backupPrefs.autoStartServer,
                autoStartOnBoot = backupPrefs.autoStartOnBoot,
                autoStartOnBootForeground = backupPrefs.autoStartOnBootForeground,
                autoStartIPTV = backupPrefs.autoStartIPTV,
                enableAutoUpdate = backupPrefs.enableAutoUpdate,
                jtvGoServerPort = backupPrefs.jtvGoServerPort,
                jtvGoBinaryName = backupPrefs.jtvGoBinaryName,
                jtvGoBinaryVersion = backupPrefs.jtvGoBinaryVersion,
                jtvConfigLocation = backupPrefs.jtvConfigLocation,
                iptvAppName = backupPrefs.iptvAppName,
                iptvAppPackageName = backupPrefs.iptvAppPackageName,
                iptvAppLaunchActivity = backupPrefs.iptvAppLaunchActivity,
                iptvLaunchCountdown = backupPrefs.iptvLaunchCountdown,
                overlayPermissionAttempts = backupPrefs.overlayPermissionAttempts,
                loginChk = backupPrefs.loginChk,
                recentChannelsJson = backupPrefs.recentChannelsJson,
                currentPort = backupPrefs.currentPort,
                filterQ = backupPrefs.filterQ,
                filterL = backupPrefs.filterL,
                filterC = backupPrefs.filterC,
                filterQX = backupPrefs.filterQX,
                filterLX = backupPrefs.filterLX,
                filterCX = backupPrefs.filterCX,
                operationMODE = backupPrefs.operationMODE,
                darkMODE = backupPrefs.darkMODE,
                filterLI = backupPrefs.filterLI,
                filterCI = backupPrefs.filterCI,
                recentChannels = backupPrefs.recentChannels,
                selectedScreenTV = backupPrefs.selectedScreenTV,
                selectedRemoteNavTV = backupPrefs.selectedRemoteNavTV,

//                custURL = backupPrefs.custURL,
//                channelListJson = backupPrefs.channelListJson,
//                expDebug = backupPrefs.expDebug,
//                lastSelectedCategoryExp = backupPrefs.lastSelectedCategoryExp,
//                showPLAYLIST = backupPrefs.showPLAYLIST,

            )
            skySharedPref.savePreferences()

//            skySharedPref.myPrefs = backupPrefs // for all
//            skySharedPref.savePreferences()

            prefs.edit {
                putLong("last_version_code", currentVersion)
            }

        }
    }


    override fun onResume() {
        super.onResume()

        if (isServerRunning)
            onJTVServerRun()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissions()
        preferenceManager = SkySharedPref.getInstance(this)

        isSwitchDarkMode = preferenceManager.myPrefs.darkMODE

        isSwitchOnForAutoStartForeground = preferenceManager.myPrefs.autoStartOnBootForeground
        if (!checkOverlayPermission() && isSwitchOnForAutoStartForeground) {
            isSwitchOnForAutoStartForeground = false
            preferenceManager.myPrefs.autoStartOnBootForeground = false
            preferenceManager.savePreferences()
        }

        // Request Ignore Battery Optimizations if not already granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intentSettings =
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        addCategory(Intent.CATEGORY_DEFAULT)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                startActivity(intentSettings)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                binaryStoppedReceiver,
                IntentFilter(BinaryService.ACTION_BINARY_STOPPED),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                binaryStoppedReceiver,
                IntentFilter(BinaryService.ACTION_BINARY_STOPPED)
            )
        }


        val sharedPref = getSharedPreferences("channel_cache", MODE_PRIVATE)
        try {
            with(sharedPref.edit()) {
                remove("channels_json")
                apply()
                Log.d("DIXf", "Cleared channel cache")
            }
        } catch (e: Exception) {
            Log.e("DIXf", "Error message", e)
        }


        // Register the OnBackPressedCallback
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        setContent {
            JGOTheme(themeOverride = isSwitchDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentScreen != "Zone") {
                            BottomNavigationBar(
                                currentScreen = currentScreen,
                                setCurrentScreen = { currentScreen = it }
                            )
                        }
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            "Home" -> HomeScreen(
                                title = selectedBinaryName,
                                titleShouldGlow = isGlowBox,
                                isServerRunning = isServerRunning,
                                publicJTVServerURL = getPublicJTVServerURL(context = this@MainActivity),
                                outputText = outputText,
                                onRunServerButtonClick = {
                                    runBinary(
                                        activity = this@MainActivity,
                                        arguments = emptyArray(),
                                        onRunSuccess = {
                                            onJTVServerRun()
                                        },
                                        onOutput = { output ->
                                            outputText = output
                                        }
                                    )
                                },
                                onStopServerButtonClick = {
                                    stopBinary(
                                        context = this@MainActivity,
                                        onBinaryStopped = {
                                            isGlowBox = false
                                            isServerRunning = false
                                            outputText = "Server stopped"
                                        }
                                    )
                                },
                                onRunIPTVButtonClick = {
                                    iptvRedirectFunc2()
//                                    launchIPTV()
                                },
                                onWebTVButtonClick = {
                                    val intent =
                                        Intent(this@MainActivity, WebPlayerActivity::class.java)
                                    startActivity(intent)
                                },
                                onDebugButtonClick = {
                                    currentScreen = "Zone"
//                                    val intent =
//                                        Intent(this@MainActivity, CastActivity::class.java)
//                                    startActivity(intent)
                                },
                                onExitButtonClick = {
                                    stopBinary(
                                        context = this@MainActivity,
                                        onBinaryStopped = {
                                            isGlowBox = false
                                            isServerRunning = false
                                            outputText = "Server stopped"
                                            finish()
                                            Process.killProcess(Process.myPid())
                                            exitProcess(0)
                                        }
                                    )
                                },
                            )

                            "Settings" -> SettingsScreen(
                                activity = this@MainActivity,
                                checkForUpdates = { checkForUpdates(true) },
                                onNavigate = { title -> currentScreen = title },
                                isSwitchOnForAutoStartForeground = isSwitchOnForAutoStartForeground,
                                onAutoStartForegroundSwitch = {
                                    if (it) {
                                        requestOverlayPermission()
                                    } else {
                                        preferenceManager.myPrefs.autoStartOnBootForeground = false
                                        preferenceManager.savePreferences()
                                        isSwitchOnForAutoStartForeground = false
                                    }
                                })

                            "SettingsTV" -> SettingsScreen(
                                activity = this@MainActivity,
                                checkForUpdates = { checkForUpdates(true) },
                                onNavigate = { title -> currentScreen = title },
                                isSwitchOnForAutoStartForeground = isSwitchOnForAutoStartForeground,
                                onAutoStartForegroundSwitch = {
                                    if (it) {
                                        requestOverlayPermission()
                                    } else {
                                        preferenceManager.myPrefs.autoStartOnBootForeground = false
                                        preferenceManager.savePreferences()
                                        isSwitchOnForAutoStartForeground = false
                                    }
                                })

                            "Info" -> InfoScreen(context = this@MainActivity)
                            "Debug" -> DebugScreen(
                                context = this@MainActivity,
                                onNavigate = { title -> currentScreen = title })

                            "Runner" -> RunnerScreen(context = this@MainActivity)
                            "Login" -> LoginScreen(context = this@MainActivity)
                            "Cast" -> CastScreen(context = this@MainActivity)
                            "Zone" -> ZoneScreen(
                                context = this@MainActivity,
                                onNavigate = { title -> currentScreen = title })
                        }

                        // Show the redirect popup
                        RedirectPopup(
                            appIPTV = preferenceManager.myPrefs.iptvAppName,
                            appIPTVpkg = preferenceManager.myPrefs.iptvAppPackageName,
                            isVisible = showRedirectPopup,
                            countdownTime = preferenceManager.myPrefs.iptvLaunchCountdown,
                            context = this@MainActivity,
                            onUserCancel = {
                                showRedirectPopup = false
                                shouldLaunchIPTV = false
                            },
                            onTimeOut = {
                                showRedirectPopup = false
                            }
                        )

                        LoginPopup(
                            isVisible = showLoginPopup,
                            title = "Login Required",
                            text = "Please log in using WebTV to access the server",
                            confirmButtonText = "Login",
                            dismissButtonText = "Cancel",
                            onConfirm = {
//                                currentScreen = "Login"
                                showLoginPopupD = true
                                showLoginPopup = false

//                                onNavigate("Runner")
                                return@LoginPopup
                            },
                            onDismiss = {
                                showLoginPopup = false
                                return@LoginPopup
                            },
                            onSettingsClick = {
                                showLoginPopup = false
                                val intent =
                                    Intent(this@MainActivity, WebPlayerActivity::class.java)
                                startActivity(intent)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Opening WEBTV",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d("DIX", "Opening WEBTV")
                                return@LoginPopup
                            }
                        )

                        LoginScreenPop(
                            showDialog = showLoginPopupD,
                            onDismissRequest = {
                                showLoginPopupD = false
                            },
                            context = this@MainActivity
                        )

                        CustPopup(
                            isVisible = showBinaryUpdatePopup,
                            title = "Binary Update Available",
                            text = "A new version of the binary is available. Update now?",
                            confirmButtonText = "Update",
                            dismissButtonText = "Later",
                            onConfirm = {
                                performBinaryUpdate()
                                showBinaryUpdatePopup = false
                            },
                            onDismiss = {
                                showBinaryUpdatePopup = false
                            }
                        )

                        CustPopup(
                            isVisible = showAppUpdatePopup,
                            title = "App Update Available",
                            text = "A new version of the app is available. Update now!",
                            confirmButtonText = "Update",
                            dismissButtonText = "Later",
                            onConfirm = {
                                performAppUpdate()
                                showAppUpdatePopup = true
                            },
                            onDismiss = null
                        )

                        if (downloadProgress != null) {
                            ProgressPopup(
                                fileName = downloadProgress!!.fileName,
                                currentProgress = downloadProgress!!.progress,
                            )
                        }

                        CustPopup(
                            isVisible = showOverlayPermissionPopup,
                            title = "Request Permission",
                            text = "Draw over other apps permission is required for the app to function properly.",
                            confirmButtonText = "Grant",
                            dismissButtonText = "Dismiss",
                            onConfirm = {
                                showOverlayPermissionPopup = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:$packageName".toUri()
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                }
                            },
                            onDismiss = {
                                isSwitchOnForAutoStartForeground = false
                                showOverlayPermissionPopup = false
                                Toast.makeText(
                                    this@MainActivity,
                                    "Permission is required to continue",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        JTVModeSelectorPopup(
                            isVisible = showOperationDialog,
                            onDismiss = {
                                showOperationDialog = false
                            },
                            onModeSelected = {
                                showOperationDialog = false
                            },
                            preferenceManager = preferenceManager,
                            context = this@MainActivity
                        )


                    }
                }
            }

            LaunchedEffect(Unit) {
                val currentBinaryVersion = preferenceManager.myPrefs.jtvGoBinaryVersion
                if (currentBinaryVersion == null || preferenceManager.myPrefs.enableAutoUpdate)
                    checkForUpdates()
            }
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when (currentScreen) {
                "Settings" -> {
                    currentScreen = "Home"
                }

                "SettingsTV" -> {
                    currentScreen = "Zone"
                }

                "Debug" -> {
                    currentScreen = "Home"
                }

                "Info" -> {
                    currentScreen = "Home"
                }

                "Runner" -> {
                    currentScreen = "Debug"
                }

                "Login" -> {
                    currentScreen = "Debug"
                }

                "Zone" -> {
                    currentScreen = "Debug"
                }

                else -> {
                    // Let the system handle the back press
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.remove()
        unregisterReceiver(binaryStoppedReceiver)
    }

    private fun checkForUpdates(forceCheck: Boolean = false) {
        // Binary update check
        CoroutineScope(Dispatchers.IO).launch {
            val currentBinaryVersion = preferenceManager.myPrefs.jtvGoBinaryVersion
            val currentBinaryName = preferenceManager.myPrefs.jtvGoBinaryName
            if (currentBinaryName.isNullOrEmpty() || currentBinaryVersion.isNullOrEmpty()) {
                performBinaryUpdate()
                return@launch
            }

            // Switch to stable release on update check
            if (currentBinaryVersion.contains("develop", ignoreCase = true) && forceCheck) {
                performBinaryUpdate()
                return@launch
            }

            if (!preferenceManager.myPrefs.enableAutoUpdate && !forceCheck)
                return@launch

            val latestBinaryReleaseInfo = BinaryUpdater.fetchLatestReleaseInfo()
            Log.d("DIX", "Current binary version: $currentBinaryVersion")
            Log.d("DIX", "Latest binary version: ${latestBinaryReleaseInfo?.version}")

            Log.d(TAG, "$latestBinaryReleaseInfo,--,$currentBinaryVersion")

            if (latestBinaryReleaseInfo?.version?.compareTo(
                    SemanticVersionNew.parse(
                        currentBinaryVersion
                    )
                ) == 1
            ) {
                showBinaryUpdatePopup = true
                Log.d("DIX", "Binary update available")
            } else {
                if (forceCheck) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            this@MainActivity,
                            "No binary updates available",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }

        // App Update check
        CoroutineScope(Dispatchers.IO).launch {
            if (!preferenceManager.myPrefs.enableAutoUpdate && !forceCheck)
                return@launch

            val currentAppVersion = BuildConfig.VERSION_NAME
            val latestAppVersion = ApplicationUpdater.fetchLatestReleaseInfo()
            if (latestAppVersion?.version?.compareTo(SemanticVersionNew.parse(currentAppVersion)) == 1) {
                showAppUpdatePopup = true
                Log.d("DIX", "App update available")
            } else {
                if (forceCheck) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            this@MainActivity,
                            "No app updates available",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

    private fun performBinaryUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            val latestBinaryReleaseInfo = BinaryUpdater.fetchLatestReleaseInfo()
            if (latestBinaryReleaseInfo == null || latestBinaryReleaseInfo.downloadUrl.isEmpty()) {
                return@launch
            }

            val previousBinaryName = preferenceManager.myPrefs.jtvGoBinaryName
            if (!previousBinaryName.isNullOrEmpty()) {
                val previousBinaryFile = filesDir.resolve(previousBinaryName)
                if (previousBinaryFile.exists()) {
                    previousBinaryFile.delete()
                }
            }
            preferenceManager.myPrefs.jtvGoBinaryName = null
            preferenceManager.myPrefs.jtvGoBinaryVersion = "v0.0.0"
            preferenceManager.savePreferences()

            downloadFile(
                url = latestBinaryReleaseInfo.downloadUrl,
                fileName = latestBinaryReleaseInfo.name,
                path = filesDir.absolutePath,
                onDownloadStatusUpdate = { DownloadModelNew ->
                    when (DownloadModelNew.status) {
                        Status.CANCELLED -> {
                            this@MainActivity.downloadProgress = null
                        }

                        Status.FAILED -> {
                            Log.e("DIX", "Download failed")
                            Log.e("DIX", DownloadModelNew.failureReason)
                            this@MainActivity.downloadProgress = null
                        }

                        Status.SUCCESS -> {
                            preferenceManager.myPrefs.jtvGoBinaryVersion =
                                latestBinaryReleaseInfo.version.toString()
                            preferenceManager.myPrefs.jtvGoBinaryName = latestBinaryReleaseInfo.name
                            preferenceManager.savePreferences()
                            this@MainActivity.downloadProgress = null

                            Handler(Looper.getMainLooper()).postDelayed({
                                if (preferenceManager.myPrefs.operationMODE == 0) {
                                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    this@MainActivity.startActivity(intent)
                                    Process.killProcess(Process.myPid())
                                }
                            }, 500)


                        }

                        else -> {
                            this@MainActivity.downloadProgress = DownloadProgress(
                                fileName = DownloadModelNew.fileName,
                                progress = DownloadModelNew.progress
                            )
                        }
                    }
                }
            )
        }
    }

    private fun downloadFile(
        url: String,
        fileName: String,
        path: String = filesDir.absolutePath,
        onDownloadStatusUpdate: (DownloadModelNew) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize OkHttpClient
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()

                // Build the request
                val request = Request.Builder()
                    .url(url)
                    .build()

                // Execute the request
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onDownloadStatusUpdate(
                            DownloadModelNew(Status.FAILED, fileName, 0, "HTTP ${response.code}")
                        )
                        return@use
                    }

                    // Create the file output stream
                    val file = File(path, fileName)
                    response.body?.byteStream()?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            val contentLength = response.body?.contentLength() ?: -1L

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                val progress = if (contentLength > 0) {
                                    (totalBytesRead * 100 / contentLength).toInt()
                                } else {
                                    -1
                                }
                                onDownloadStatusUpdate(
                                    DownloadModelNew(Status.IN_PROGRESS, fileName, progress, "")
                                )
                            }
                        }
                    }

                    // File download completed successfully
                    onDownloadStatusUpdate(
                        DownloadModelNew(Status.SUCCESS, fileName, 100, "")
                    )
                }
            } catch (e: Exception) {
                onDownloadStatusUpdate(
                    DownloadModelNew(Status.FAILED, fileName, 0, e.message ?: "Unknown error")
                )
            }
        }
    }


    private fun performAppUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            val latestAppVersion = ApplicationUpdater.fetchLatestReleaseInfo()
            if (latestAppVersion == null || latestAppVersion.downloadUrl.isEmpty()) {
                return@launch
            }

            ApplicationUpdater.downloadAppUpdate(
                context = this@MainActivity,
                downloadUrl = latestAppVersion.downloadUrl,
                fileName = latestAppVersion.name,
                onProgress = { progress ->
                    downloadProgress = progress
                }
            )
        }
    }

    private fun onJTVServerRun() {
        // Check server status
        val port = preferenceManager.myPrefs.jtvGoServerPort
        CoroutineScope(Dispatchers.IO).launch {
            checkServerStatus(
                port = port,
                onLoginSuccess = {
                    isServerRunning = true
                    isGlowBox = true

                    if (preferenceManager.myPrefs.autoStartIPTV) {
                        countdownJob?.cancel() // Cancel any existing countdown job

                        var countdownTime = preferenceManager.myPrefs.iptvLaunchCountdown
                        countdownJob = CoroutineScope(Dispatchers.Main).launch {
                            showRedirectPopup = (currentScreen != "Zone") &&
                                    !preferenceManager.myPrefs.iptvAppPackageName.isNullOrEmpty()


                            shouldLaunchIPTV = true

                            while (countdownTime > 0) {
                                delay(1000)
                                countdownTime--
                            }

                            showRedirectPopup = false

                            if (shouldLaunchIPTV) {
                                startIPTV2()
//                                launchIPTV()
                            }
                        }
                    }
                },
                onLoginFailure = {
                    isGlowBox = false
                    isServerRunning = true
                    if (preferenceManager.myPrefs.loginChk) {
                        showLoginPopup = true
                    } else {
                        if (preferenceManager.myPrefs.autoStartIPTV) {
                            countdownJob?.cancel() // Cancel any existing countdown job

                            var countdownTime = preferenceManager.myPrefs.iptvLaunchCountdown
                            countdownJob = CoroutineScope(Dispatchers.Main).launch {
                                showRedirectPopup = (currentScreen != "Zone") &&
                                        !preferenceManager.myPrefs.iptvAppPackageName.isNullOrEmpty()
                                shouldLaunchIPTV = true

                                while (countdownTime > 0) {
                                    delay(1000)
                                    countdownTime--
                                }

                                showRedirectPopup = false

                                if (shouldLaunchIPTV) {
                                    startIPTV2()
//                                    launchIPTV()
                                }
                            }
                        }
                    }
                },
                onServerDown = {
                    CoroutineScope(Dispatchers.Main).launch {
                        isServerRunning = false
                        isGlowBox = false
                        Toast.makeText(this@MainActivity, "Server is down", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false

        if (!notificationGranted) {
            Toast.makeText(
                this,
                "Notification permission is required to show alerts",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Notification permission request
    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private fun getPublicJTVServerURL(context: Context): String {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val savedPortNumber = preferenceManager.myPrefs.jtvGoServerPort
        val isPublic = !preferenceManager.myPrefs.serveLocal

        // If not public, always return localhost URL
        if (!isPublic)
            return "http://localhost:$savedPortNumber/playlist.m3u"

        val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork
        } else {
            @Suppress("deprecation")
            val networks = connectivityManager.allNetworks
            if (networks.isNotEmpty()) networks[0] else null
        }

        if (activeNetwork != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            // Check if the network is Wi-Fi or Ethernet
            if (networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            ) {

                val linkProperties: LinkProperties? =
                    connectivityManager.getLinkProperties(activeNetwork)
                val ipAddresses = linkProperties?.linkAddresses
                    ?.filter { it.address is Inet4Address } // Filter for IPv4 addresses
                    ?.map { it.address.hostAddress }
                val ipAddress = ipAddresses?.firstOrNull() // Get the first IPv4 address

                if (ipAddress != null)
                    return "http://$ipAddress:$savedPortNumber/playlist.m3u"
            }

            // Check if the network is mobile data
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "http://localhost:$savedPortNumber/playlist.m3u"
                }
            }
        }

        // No active network
        return "Connect to internet"
    }

    // Receiver to handle binary stop action
    private val binaryStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BinaryService.ACTION_BINARY_STOPPED) {
                isServerRunning = false
                outputText = "Server stopped"
            }
        }
    }


    private fun startIPTV2() {
        val appPackageName = preferenceManager.myPrefs.iptvAppPackageName
        val appName = preferenceManager.myPrefs.iptvAppName

        if (appPackageName.isNullOrEmpty() || currentScreen == "Zone") {
            Log.d("DIX", "IPTV not set or already on Zone screen")
            return
        }

        executor.execute {
            try {
                when (appPackageName) {
                    "webtv" -> runOnUiThread {
                        Log.d("DIX", "Opening WEBTV")
                        toast("Opening WEBTV")
                        startActivity(Intent(this, WebPlayerActivity::class.java))
                    }

                    "tvzone" -> runOnUiThread {
                        Log.d("DIX", "Opening TVZone")
                        toast("Starting TV")
                        currentScreen = "Zone"
                    }

                    "sonata" -> {
//                        toast("Starting Sonata")
                        Log.d("DIX", "Opening Sonata")
                        val intent = Intent(this, LandingPage::class.java)
                        this.startActivity(intent)
                    }

                    else -> {
                        val launchIntent = packageManager.getLaunchIntentForPackage(appPackageName)
                        runOnUiThread {
                            if (launchIntent != null) {
                                Log.d("DIX", "Opening $appName")
                                toast("Opening $appName")
                                startActivity(launchIntent)
                            } else {
                                Log.d("DIX", "Cannot find: $appPackageName")
                                toast("Cannot find the specified application")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DIX", "Error starting IPTV", e)
                runOnUiThread { toast("Error starting IPTV") }
            }
        }
    }


    private fun iptvRedirectFunc2() {
        val appPackageName = preferenceManager.myPrefs.iptvAppPackageName
        val appLaunchActivity = preferenceManager.myPrefs.iptvAppLaunchActivity
        val appName = preferenceManager.myPrefs.iptvAppName

        if (appPackageName.isNullOrEmpty() || currentScreen == "Zone") {
            toast("IPTV app not selected")
            Log.d("DIX", "IPTV app not selected or already on Zone screen")
            startActivity(Intent(this, AppListActivity::class.java))
            return
        }

        Log.d("DIX", "IPTV Package: $appPackageName")

        try {
            when (appPackageName) {
                "webtv" -> {
                    startActivity(Intent(this, WebPlayerActivity::class.java))
                    toast("Starting: $appName")
                    Log.d("DIX", "Opening WebTV")
                }

                "tvzone" -> {
                    toast("Starting TV")
                    Log.d("DIX", "Opening TVZone")
                    currentScreen = "Zone"
                }

                "sonata" -> {
                    toast("Starting Sonata")
                    Log.d("DIX", "Opening Sonata")
                    val intent = Intent(this, LandingPage::class.java)
                    this.startActivity(intent)
                }

                else -> {
                    if (appLaunchActivity.isNullOrEmpty()) {
                        toast("App launch activity not found")
                        Log.d("DIX", "Launch activity not set for $appPackageName")
                        return
                    }

                    Log.d("DIX", "Launch Activity: $appLaunchActivity")

                    val launchIntent = Intent().apply {
                        component = ComponentName(appPackageName, appLaunchActivity)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    if (launchIntent.resolveActivity(packageManager) != null) {
                        startActivity(launchIntent)
                        toast("Starting: $appName")
                        Log.d("DIX", "Launching $appName via $appLaunchActivity")
                    } else {
                        toast("App not found")
                        Log.d("DIX", "Failed to resolve app: $appPackageName / $appLaunchActivity")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DIX", "Error launching IPTV app", e)
            toast("Error launching IPTV app")
        }
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (!Settings.canDrawOverlays(this)) {
            isSwitchOnForAutoStartForeground = false
            Toast.makeText(this, "Overlay permission Denied!", Toast.LENGTH_SHORT)
                .show()
        } else {
            preferenceManager.myPrefs.autoStartOnBootForeground = true
            preferenceManager.savePreferences()
            isSwitchOnForAutoStartForeground = true
        }
    }

    private fun checkOverlayPermission(): Boolean {
        if (preferenceManager.myPrefs.overlayPermissionAttempts == 3) {
            return true
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        try {
            val overlayPermissionAttempts = preferenceManager.myPrefs.overlayPermissionAttempts

            when {
                overlayPermissionAttempts < 1 -> {
                    showOverlayPermissionPopup = true
                    incrementAndSaveAttempts(overlayPermissionAttempts)
                    return
                }

                overlayPermissionAttempts == 2 -> {
                    grantPermissionAndSave()
                    incrementAndSaveAttempts(overlayPermissionAttempts)
                    showToast("Turning ON foreground run forcefully. It may not work. Warning!")
                    return
                }

                overlayPermissionAttempts > 3 -> {
                    showOverlayPermissionPopup = true
                    resetAttempts()
                    showToast("Too many attempts, resetting.")
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    grantPermissionAndSave()
                } else {
                    incrementAndSaveAttempts(overlayPermissionAttempts)
                    Log.d(
                        "OverlayPermission",
                        "Overlay permission not granted, incrementing attempt count. $overlayPermissionAttempts"
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("OverlayPermission", "Error requesting overlay permission: ${e.message}")
            showToast("Error requesting overlay permission: ${e.message}")
        }
    }

    private fun incrementAndSaveAttempts(attempts: Int) {
        preferenceManager.myPrefs.overlayPermissionAttempts = attempts + 1
        preferenceManager.savePreferences()
    }

    private fun resetAttempts() {
        preferenceManager.myPrefs.overlayPermissionAttempts = 0
        preferenceManager.savePreferences()
    }

    private fun grantPermissionAndSave() {
        preferenceManager.myPrefs.autoStartOnBootForeground = true
        preferenceManager.savePreferences()
        isSwitchOnForAutoStartForeground = true
        Log.i("OverlayPermission", "Overlay permission granted.")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("OverlayPermission", message)
    }


}