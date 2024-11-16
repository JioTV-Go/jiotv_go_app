package com.skylake.skytv.jgorunner.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.ketch.DownloadModel
import com.ketch.Ketch
import com.ketch.Status
import com.skylake.skytv.jgorunner.BuildConfig
import com.skylake.skytv.jgorunner.core.checkServerStatus
import com.skylake.skytv.jgorunner.core.data.JTVConfigurationManager
import com.skylake.skytv.jgorunner.core.execution.runBinary
import com.skylake.skytv.jgorunner.core.execution.stopBinary
import com.skylake.skytv.jgorunner.core.update.ApplicationUpdater
import com.skylake.skytv.jgorunner.core.update.BinaryUpdater
import com.skylake.skytv.jgorunner.core.update.downloadFile
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.BinaryService
import com.skylake.skytv.jgorunner.ui.components.BottomNavigationBar
import com.skylake.skytv.jgorunner.ui.components.CustPopup
import com.skylake.skytv.jgorunner.ui.components.ProgressPopup
import com.skylake.skytv.jgorunner.ui.components.RedirectPopup
import com.skylake.skytv.jgorunner.ui.screens.DebugScreen
import com.skylake.skytv.jgorunner.ui.screens.HomeScreen
import com.skylake.skytv.jgorunner.ui.screens.InfoScreen
import com.skylake.skytv.jgorunner.ui.screens.LoginScreen
import com.skylake.skytv.jgorunner.ui.screens.RunnerScreen
import com.skylake.skytv.jgorunner.ui.screens.SettingsScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cthing.versionparser.semver.SemanticVersion
import java.net.Inet4Address
import java.util.concurrent.Executors


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
    private var isServerRunning by mutableStateOf(false)

    private var isGlowBox by mutableStateOf(false)

    private var showOverlayPermissionPopup by mutableStateOf(false)

    private var showRedirectPopup by mutableStateOf(false)
    private var shouldLaunchIPTV by mutableStateOf(false)
    private var countdownJob: Job? = null

    private var downloadModel by mutableStateOf<DownloadModel?>(null)

    override fun onStart() {
        super.onStart()
        preferenceManager = SkySharedPref.getInstance(this)

        JTVConfigurationManager.getInstance(this).saveJTVConfiguration()
        isServerRunning = BinaryService.isRunning
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissions()

        selectedBinaryName = "JTV-GO SERVER"

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

        // Register the OnBackPressedCallback
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        preferenceManager = SkySharedPref.getInstance(this)

        setContent {
            JGOTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavigationBar(setCurrentScreen = { currentScreen = it }) },
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
                                    iptvRedirectFunc()
                                },
                                onWebTVButtonClick = {
                                    val intent =
                                        Intent(this@MainActivity, WebPlayerActivity::class.java)
                                    startActivity(intent)
                                },
                                onDebugButtonClick = {
                                    currentScreen = "Debug"
                                },
                                onExitButtonClick = {
                                    stopBinary(
                                        context = this@MainActivity,
                                        onBinaryStopped = {
                                            isGlowBox = false
                                            isServerRunning = false
                                            outputText = "Server stopped"
                                            finish()
                                        }
                                    )
                                },
                            )

                            "Settings" -> SettingsScreen(
                                activity = this@MainActivity,
                                checkForUpdates = { checkForUpdates() },
                                requestOverlayPermission = { requestOverlayPermission() })

                            "Info" -> InfoScreen(context = this@MainActivity)
                            "Debug" -> DebugScreen(
                                context = this@MainActivity,
                                onNavigate = { title -> currentScreen = title })

                            "Runner" -> RunnerScreen(context = this@MainActivity)
                            "Login" -> LoginScreen(context = this@MainActivity)
                        }

                        // Show the redirect popup
                        RedirectPopup(
                            appIPTV = preferenceManager.myPrefs.iptvAppName,
                            appIPTVpkg = preferenceManager.myPrefs.iptvAppPackageName,
                            isVisible = showRedirectPopup,
                            countdownTime = preferenceManager.myPrefs.iptvLaunchCountdown,
                            context = this@MainActivity,
                            onDismiss = {
                                showRedirectPopup = false

                                // Cancel IPTV launch if dismissed
                                shouldLaunchIPTV = false
                            }
                        )

                        CustPopup(
                            isVisible = showLoginPopup,
                            title = "Login Required",
                            text = "Please log in using WebTV, to access the server.",
                            confirmButtonText = "Login",
                            dismissButtonText = "Cancel",
                            onConfirm = {
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
                                return@CustPopup
                            },
                            onDismiss = {
                                showLoginPopup = false
                                return@CustPopup
                            }
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
                            text = "A new version of the app is available. Update now?",
                            confirmButtonText = "Update",
                            dismissButtonText = "Later",
                            onConfirm = {
                                performAppUpdate()
                                showAppUpdatePopup = false
                            },
                            onDismiss = {
                                showAppUpdatePopup = false
                            }
                        )

                        if (downloadModel != null) {
                            ProgressPopup(
                                fileName = downloadModel!!.fileName,
                                currentProgress = downloadModel!!.progress,
                                onCancel = {
                                    // Cancel the download
                                    Ketch.builder().build(this@MainActivity)
                                        .cancel(downloadModel!!.id)
                                    downloadModel = null
                                }
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
                                        Uri.parse("package:$packageName")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                }
                            },
                            onDismiss = {
                                showOverlayPermissionPopup = false
                                Toast.makeText(
                                    this@MainActivity,
                                    "Permission is required to continue",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@CustPopup
                            }
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

                "Debug" -> {
                    currentScreen = "Home"
                }

                "Info" -> {
                    currentScreen = "Debug"
                }

                "Runner" -> {
                    currentScreen = "Debug"
                }

                "Login" -> {
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

    private fun checkForUpdates() {
        // Binary update check
        CoroutineScope(Dispatchers.IO).launch {
            val currentBinaryVersion = preferenceManager.myPrefs.jtvGoBinaryVersion
            if (currentBinaryVersion.isNullOrEmpty()) {
                showBinaryUpdatePopup = true
                return@launch
            }

            val latestBinaryReleaseInfo = BinaryUpdater.fetchLatestReleaseInfo()
            Log.d("DIX", "Current binary version: $currentBinaryVersion")
            Log.d("DIX", "Latest binary version: ${latestBinaryReleaseInfo?.version}")

            if (latestBinaryReleaseInfo?.version?.compareTo(
                    SemanticVersion.parse(
                        currentBinaryVersion
                    )
                ) == 1
            ) {
                showBinaryUpdatePopup = true
                Log.d("DIX", "Binary update available")
            }
        }

        // App Update check
        CoroutineScope(Dispatchers.IO).launch {
            if (!preferenceManager.myPrefs.enableAutoUpdate)
                return@launch

            val currentAppVersion = BuildConfig.VERSION_NAME
            val latestAppVersion = ApplicationUpdater.fetchLatestReleaseInfo()
            if (latestAppVersion?.version?.compareTo(SemanticVersion.parse(currentAppVersion)) == 1) {
                showAppUpdatePopup = true
                Log.d("DIX", "App update available")
            }
        }
    }

    private fun performBinaryUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            val latestBinaryReleaseInfo = BinaryUpdater.fetchLatestReleaseInfo()
            if (latestBinaryReleaseInfo == null || latestBinaryReleaseInfo.downloadUrl.isEmpty()) {
                return@launch
            }

            downloadStuff(
                url = latestBinaryReleaseInfo.downloadUrl,
                fileName = latestBinaryReleaseInfo.name,
                path = filesDir.absolutePath,
                onDownloadCompleted = {
                    // Delete previous binary
                    val previousBinaryName = preferenceManager.myPrefs.jtvGoBinaryName
                    if (!previousBinaryName.isNullOrEmpty()) {
                        val previousBinaryFile = filesDir.resolve(previousBinaryName)
                        if (previousBinaryFile.exists()) {
                            previousBinaryFile.delete()
                        }
                    }

                    preferenceManager.myPrefs.jtvGoBinaryVersion =
                        latestBinaryReleaseInfo.version.toString()
                    preferenceManager.myPrefs.jtvGoBinaryName = latestBinaryReleaseInfo.name
                    preferenceManager.savePreferences()
                }
            )
        }
    }

    private fun performAppUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            val latestAppVersion = ApplicationUpdater.fetchLatestReleaseInfo()
            if (latestAppVersion == null || latestAppVersion.downloadUrl.isEmpty()) {
                return@launch
            }

            downloadStuff(
                url = latestAppVersion.downloadUrl,
                fileName = latestAppVersion.name,
                path = filesDir.absolutePath,
                onDownloadCompleted = {
                    // Install the downloaded APK
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(
                        Uri.parse("file://${filesDir.absolutePath}/${latestAppVersion.name}"),
                        "application/vnd.android.package-archive"
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            )
        }
    }

    private fun downloadStuff(
        url: String,
        fileName: String,
        path: String,
        onDownloadCompleted: () -> Unit
    ) {
        downloadFile(
            activity = this@MainActivity,
            url = url,
            fileName = fileName,
            path = path,
            onDownloadStatusUpdate = { downloadModel ->
                when (downloadModel.status) {
                    Status.QUEUED -> {}
                    Status.STARTED -> {
                        this.downloadModel = downloadModel
                    }

                    Status.CANCELLED -> {
                        this.downloadModel = null
                    }

                    Status.FAILED -> {
                        Log.e("DIX", "Download failed")
                        Log.e("DIX", downloadModel.failureReason)
                        this.downloadModel = null
                    }

                    Status.PAUSED -> {}
                    Status.DEFAULT -> {}
                    Status.PROGRESS -> {
                        this.downloadModel = downloadModel
                    }

                    Status.SUCCESS -> {
                        this.downloadModel = null
                        onDownloadCompleted()
                    }
                }
            }
        )
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
                            showRedirectPopup = true
                            shouldLaunchIPTV = true

                            while (countdownTime > 0) {
                                delay(1000)
                                countdownTime--
                            }

                            showRedirectPopup = false

                            if (shouldLaunchIPTV) {
                                startIPTV()
                            }
                        }
                    }
                },
                onLoginFailure = {
                    isGlowBox = false
                    isServerRunning = true
                    showLoginPopup = true
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

    private fun startIPTV() {
        executor.execute {
            try {
                runOnUiThread {
                    val appPackageName = preferenceManager.myPrefs.iptvAppPackageName
                    if (!appPackageName.isNullOrEmpty()) {
                        Log.d("DIX", appPackageName)
                        val appName = preferenceManager.myPrefs.iptvAppName

                        if (appPackageName == "webtv") {
                            val intent = Intent(this@MainActivity, WebPlayerActivity::class.java)
                            Toast.makeText(this@MainActivity, "Opening WEBTV", Toast.LENGTH_SHORT)
                                .show()
                            Log.d("DIX", "Opening WEBTV")
                            startActivity(intent)
                        } else {
                            val launchIntent =
                                packageManager.getLaunchIntentForPackage(appPackageName)
                            launchIntent?.let {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Opening $appName",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d("DIX", "Opening $appName")
                                startActivity(it)
                            } ?: run {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Cannot find the specified application",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d("DIX", "Cannot find the specified application")
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "IPTV not set.", Toast.LENGTH_SHORT)
                            .show()
                        Log.d("DIX", "IPTV not set")
                    }
                }
            } catch (e: Exception) {
                Log.e("DIX", "Error starting IPTV", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error starting IPTV", Toast.LENGTH_SHORT)
                        .show()
                }
                Log.d("DIX", "Error starting IPTV: ${e.message}")
            }
        }
    }

    private fun getPublicJTVServerURL(context: Context): String {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val savedPortNumber = preferenceManager.myPrefs.jtvGoServerPort

        val isPublic = !preferenceManager.myPrefs.serveLocal
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
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val linkProperties: LinkProperties? =
                    connectivityManager.getLinkProperties(activeNetwork)
                val ipAddresses = linkProperties?.linkAddresses
                    ?.filter { it.address is Inet4Address } // Filter for IPv4 addresses
                    ?.map { it.address.hostAddress }
                val ipAddress = ipAddresses?.firstOrNull() // Get the first IPv4 address

                if (ipAddress != null)
                    return "http://$ipAddress:$savedPortNumber/playlist.m3u"
            }
        }

        // WiFi is not connected or no public IP address found
        return "Not connected to Wi-Fi"
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

    private fun iptvRedirectFunc() {
        // Retrieve the app package name from shared preferences
        val appPackageName = preferenceManager.myPrefs.iptvAppPackageName

        if (!appPackageName.isNullOrEmpty()) {
            Log.d("DIX", appPackageName)
            // Retrieve other details
            val appLaunchActivity = preferenceManager.myPrefs.iptvAppLaunchActivity
            val appName = preferenceManager.myPrefs.iptvAppName

            if (appPackageName == "webtv") {
                val intent = Intent(this@MainActivity, WebPlayerActivity::class.java)
                startActivity(intent)
                Toast.makeText(this@MainActivity, "Starting: $appName", Toast.LENGTH_SHORT).show()
            } else {
                if (!appLaunchActivity.isNullOrEmpty()) {
                    Log.d("DIX", appLaunchActivity)
                    // Create an intent to launch the app
                    val launchIntent = Intent().apply {
                        setClassName(appPackageName, appLaunchActivity)
                    }

                    // Check if the app can be resolved
                    val packageManager = this@MainActivity.packageManager
                    if (launchIntent.resolveActivity(packageManager) != null) {
                        // Start the activity
                        startActivity(launchIntent)
                        Toast.makeText(this@MainActivity, "Starting: $appName", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        // Handle the case where the app can't be resolved
                        Toast.makeText(this@MainActivity, "App not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    // Handle the case where app_launch_activity is not found
                    Toast.makeText(
                        this@MainActivity,
                        "App launch activity not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            // Handle the case where app_packagename is null
            Toast.makeText(this@MainActivity, "IPTV app not selected", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Handle the result of the permission request
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission Denied!", Toast.LENGTH_SHORT)
                .show()
        } else {
            preferenceManager.myPrefs.autoStartOnBootForeground = true
            preferenceManager.savePreferences()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionPopup = true
            } else {
                preferenceManager.myPrefs.autoStartOnBootForeground = true
                preferenceManager.savePreferences()
            }
        }
    }
}