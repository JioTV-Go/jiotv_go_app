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
import com.skylake.skytv.jgorunner.ui.screens.LandingPageScreen
import com.skylake.skytv.jgorunner.ui.screens.LandingPageScreen2

class LandingPage : ComponentActivity() {

    private val TAG: String = "XLandingPage"
    var currentScreen by mutableStateOf("Home")
    private val onExit = { finish() }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
//                LandingPageScreen(
//                    context = this@LandingPage,
//                    onNavigate = { title -> currentScreen = title },
//                    onExit
//                )

                LandingPageScreen2(
                )
            }
        }
    }

}