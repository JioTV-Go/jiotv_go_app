package com.skylake.skytv.jgorunner.services.player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.screens.ExoPlayJetScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat

class ExoPlayJet : ComponentActivity() {

    private val TAG: String = "ExoJetPack"

    private var videoUrlState by mutableStateOf("http://localhost:5001/live/143.m3u8")
    private var logoUrlState by mutableStateOf("https://www.sonypicturesnetworks.com/images/logos/SET%20LOGO.png")
    private var channelNameState by mutableStateOf("HANA4k")
    private var signatureFallbackState by mutableStateOf("0x0")
    private var channelListState by mutableStateOf<ArrayList<ChannelInfo>?>(null)
    private var currentChannelIndexState by mutableStateOf(-1)

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }


        val prefManager = SkySharedPref.getInstance(this)

        setContent {
            JGOTheme(themeOverride = prefManager.myPrefs.darkMODE) {
                ExoPlayJetScreen(
                    preferenceManager = prefManager,
                    videoUrl = videoUrlState,
//                    logoUrl = logoUrlState,
//                    channelName = channelNameState,
//                    signatureFallback = signatureFallbackState,
                    channelList = channelListState,
                    currentChannelIndex = currentChannelIndexState
                )
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val defaultVideoUrl = "http://localhost:5001/live/143.m3u8"
        val defaultLogoUrl = "https://www.sonypicturesnetworks.com/images/logos/SET%20LOGO.png"
        val defaultChannelName = "HANA4k"

        val channelList = intent.getParcelableArrayListExtra<ChannelInfo>("channel_list_data")

//        val channelList: ArrayList<ChannelInfo>? = IntentCompat.getParcelableArrayListExtra(intent, "channel_list_data", ChannelInfo::class.java)
        val currentChannelIndex = intent.getIntExtra("current_channel_index", -1)

        val signature = intent.getStringExtra("zone")

        signatureFallbackState = if (signature.isNullOrEmpty()) "0x0" else signature
        channelListState = channelList
        currentChannelIndexState = currentChannelIndex

        if (!channelList.isNullOrEmpty() && currentChannelIndex in channelList.indices) {
            val currentChannel: ChannelInfo = channelList[currentChannelIndex]
            videoUrlState = currentChannel.videoUrl ?: defaultVideoUrl
            logoUrlState = currentChannel.logoUrl ?: defaultLogoUrl
            channelNameState = currentChannel.channelName ?: defaultChannelName

            Log.d(TAG, "Loaded channel from list: $channelNameState at index $currentChannelIndex")
        } else {
            videoUrlState = intent.getStringExtra("video_url") ?: defaultVideoUrl
            logoUrlState = intent.getStringExtra("logo_url") ?: defaultLogoUrl
            channelNameState = intent.getStringExtra("ch_name") ?: defaultChannelName
            Log.d(TAG, "Loaded channel from direct intent extras: $channelNameState")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Mark player as inactive for Zone TV autoplay
        val prefManager = SkySharedPref.getInstance(this)
        prefManager.myPrefs.tvPlayerActive = false
        prefManager.savePreferences()
    }
}
