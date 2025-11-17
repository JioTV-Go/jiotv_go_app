package com.skylake.skytv.jgorunner.services.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.screens.ExoPlayJetScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme
import com.skylake.skytv.jgorunner.utils.DeviceUtils

@SuppressLint("MutableCollectionMutableState")
class ExoPlayJet : ComponentActivity() {

    private val tag = "ExoJetPack"

    private var videoUrlState by mutableStateOf("http://localhost:5001/live/143.m3u8")
    private var logoUrlState by mutableStateOf("https://www.sonypicturesnetworks.com/images/logos/SET%20LOGO.png")
    private var channelNameState by mutableStateOf("HANA4k")
    private var signatureFallbackState by mutableStateOf("0x0")
    private var channelListState by mutableStateOf<ArrayList<ChannelInfo>?>(null)
    private var currentChannelIndexState by mutableIntStateOf(-1)

    private val prefManager by lazy { SkySharedPref.getInstance(this) }
    private val pipController by lazy { PipController(this) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyIntent(intent)

        applyImmersive(this)

        setContent {
            JGOTheme(themeOverride = prefManager.myPrefs.darkMODE) {
                ExoPlayJetScreen(
                    preferenceManager = prefManager,
                    videoUrl = videoUrlState,
                    channelList = channelListState,
                    currentChannelIndex = currentChannelIndexState
                )
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
                        } catch (_: Exception) {
                        }
                    }
                }
            },
            closePip = {
                runOnUiThread {
                    if (isInPictureInPictureMode) {
                        PlayerCommandBus.requestStopPlayback()
                        try {
                            finishAndRemoveTask()
                        } catch (_: Exception) {
                            finish()
                        }
                    }
                }
            }
        )
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val wasInPip =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false

        val list: ArrayList<ChannelInfo>? =
            androidx.core.content.IntentCompat.getParcelableArrayListExtra(
                intent,
                "channel_list_data",
                ChannelInfo::class.java
            )
        val idx = intent.getIntExtra("current_channel_index", -1)
        val urlFromIntent = intent.getStringExtra("video_url")

        if (wasInPip) {
            applyIntent(intent)
            if (!list.isNullOrEmpty() && idx in list.indices) {
                PlayerCommandBus.requestSwitch(index = idx)
            } else if (!urlFromIntent.isNullOrEmpty()) {
                PlayerCommandBus.requestSwitch(url = urlFromIntent)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) pipController.updatePipActionsIfAllowed()
        } else {
            applyIntent(intent)
        }
    }

    private fun applyIntent(intent: Intent?) {
        val parsed = PlayerIntentHandler.parse(intent)
        signatureFallbackState = parsed.signature ?: "0x0"
        channelListState = parsed.channelList
        currentChannelIndexState = parsed.currentChannelIndex

        if (!parsed.channelList.isNullOrEmpty() && parsed.currentChannelIndex in parsed.channelList.indices) {
            val cur = parsed.channelList[parsed.currentChannelIndex]
            videoUrlState = cur.videoUrl ?: videoUrlState
            logoUrlState = cur.logoUrl ?: logoUrlState
            channelNameState = cur.channelName ?: channelNameState
            Log.d(
                tag,
                "Loaded channel from list: $channelNameState at index $currentChannelIndexState"
            )
        } else {
            videoUrlState = parsed.videoUrl ?: videoUrlState
            logoUrlState = parsed.logoUrl ?: logoUrlState
            channelNameState = parsed.channelName ?: channelNameState
            Log.d(tag, "Loaded channel from direct intent extras: $channelNameState")
        }
    }

    @SuppressLint("WrongConstant")
    fun applyImmersive(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller.hide(android.view.WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) {
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

        if (isInPictureInPictureMode) {
            if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) pipController.updatePipActionsIfAllowed()
        } else {
            if (prefManager.myPrefs.enablePip && !DeviceUtils.isTvDevice(this)) {
                window.decorView.postDelayed({
                    if (!this@ExoPlayJet.hasWindowFocus()) {
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

    override fun onStop() {
        super.onStop()
        if (!PlayerCommandBus.isInPipMode) {
            PlayerCommandBus.requestStopPlayback()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!PlayerCommandBus.isEnteringPip && !PlayerCommandBus.isInPipMode) {
            PlayerCommandBus.requestStopPlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!PlayerCommandBus.isInPipMode || isFinishing) {
            PlayerCommandBus.requestStopPlayback()
        }
    }
}
