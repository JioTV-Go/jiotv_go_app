package com.skylake.skytv.jgorunner.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.screens.FirstModeSelectorScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme

class InitModeSelectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefManager = SkySharedPref.getInstance(this)

        setContent {
            JGOTheme(themeOverride = prefManager.myPrefs.darkMODE) {
                FirstModeSelectorScreen(
                    preferenceManager = prefManager
                )
            }
        }
    }
}
