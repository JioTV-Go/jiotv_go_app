package com.skylake.skytv.jgorunner.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.screens.AppListScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme

class AppListActivity : ComponentActivity() {
    private lateinit var preferenceManager: SkySharedPref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = SkySharedPref.getInstance(this)
        enableEdgeToEdge()

        setContent {
            JGOTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    AppListScreen(
                        modifier = Modifier.padding(innerPadding),
                        onAppSelected = { selectedApp ->
                            preferenceManager.myPrefs.iptvAppName = selectedApp.appName
                            preferenceManager.myPrefs.iptvAppPackageName = selectedApp.packageName
                            preferenceManager.myPrefs.iptvAppLaunchActivity = selectedApp.launchActivity
                            preferenceManager.savePreferences()
                            finish()
                        }
                    )
                }
            }
        }
    }
}
