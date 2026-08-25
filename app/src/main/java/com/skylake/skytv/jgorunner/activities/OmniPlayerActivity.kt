package com.skylake.skytv.jgorunner.activities

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.PipController
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus
import com.skylake.skytv.jgorunner.ui.screens.OmniPlayerScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme
import com.skylake.skytv.jgorunner.utils.DeviceUtils
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import com.skylake.skytv.jgorunner.utils.LogCollector

class OmniPlayerActivity : ComponentActivity() {
    private var initialIndex by mutableIntStateOf(0)
    private var channelList: List<OmniChannel> = emptyList()
    private val prefManager by lazy { SkySharedPref.getInstance(this) }
    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }

    private val mirrorListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(d: Int) = applyOrientation()
        override fun onDisplayRemoved(d: Int) = applyOrientation()
        override fun onDisplayChanged(d: Int) = applyOrientation()
    }

    private fun isMirroring() = displayManager.displays.any { it.displayId != Display.DEFAULT_DISPLAY }

    private fun applyOrientation() {
        if (DeviceUtils.isTvDevice(this)) return
        requestedOrientation = if (isMirroring()) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    private val pipController by lazy { PipController(this) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogCollector.init(applicationContext)
        channelList = com.skylake.skytv.jgorunner.data.OmniDataManager.currentChannelList
        initialIndex = intent.getIntExtra("channel_index", 0).coerceIn(0, maxOf(0, channelList.size - 1))
        LogCollector.log("OmniPlayerActivity: onCreate with ${channelList.size} channels, target index: $initialIndex")
        applyImmersive()
        setContent {
            JGOTheme(themeOverride = prefManager.myPrefs.darkMODE) {
                OmniPlayerScreen(preferenceManager = prefManager, channelList = channelList, initialIndex = initialIndex)
            }
        }

        PlayerCommandBus.setOnStateChanged {
            if (PlayerCommandBus.isInPipMode && prefManager.myPrefs.enablePip) {
                runOnUiThread { pipController.updatePipActionsIfAllowed() }
            }
        }

        PlayerCommandBus.setPipRequestHandlers(
            openApp = {
                runOnUiThread {
                    if (isInPictureInPictureMode) {
                        try {
                            moveTaskToBack(false)
                        } catch (_: Exception) {}
                    }
                }
            },
            closePip = {
                runOnUiThread {
                    PlayerCommandBus.requestStopPlayback()
                    try {
                        finishAndRemoveTask()
                    } catch (_: Exception) {
                        finish()
                    }
                }
            }
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) {
            LogCollector.log("OmniPlayerActivity: onUserLeaveHint -> enterPipIfAllowed")
            pipController.enterPipIfAllowed()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerCommandBus.isInPipMode = isInPictureInPictureMode
        PlayerCommandBus.isEnteringPip = false
        PlayerCommandBus.notifyPipModeChanged(isInPictureInPictureMode)
        LogCollector.log("OmniPlayerActivity: onPictureInPictureModeChanged -> isInPip: $isInPictureInPictureMode")

        if (isInPictureInPictureMode) {
            if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) pipController.updatePipActionsIfAllowed()
        } else {
            if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) {
                window.decorView.postDelayed({
                    if (!this@OmniPlayerActivity.hasWindowFocus()) {
                        PlayerCommandBus.requestStopPlayback()
                        try {
                            finishAndRemoveTask()
                        } catch (_: Exception) {
                            finish()
                        }
                    }
                }, 120)
            }
        }
    }

    override fun onBackPressed() {
        if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) {
            LogCollector.log("OmniPlayerActivity: onBackPressed -> enterPipIfAllowed")
            pipController.enterPipIfAllowed()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        displayManager.registerDisplayListener(mirrorListener, null)
        applyOrientation()
        if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) {
            pipController.updatePipActionsIfAllowed()
        }
    }

    override fun onStop() {
        super.onStop()
        try { displayManager.unregisterDisplayListener(mirrorListener) } catch (_: Exception) {}
    }

    private fun applyImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
