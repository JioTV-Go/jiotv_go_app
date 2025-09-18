package com.skylake.skytv.jgorunner.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.screens.AppListScreen
import com.skylake.skytv.jgorunner.ui.theme.JGOTheme

class AppListActivity : ComponentActivity() {
    private lateinit var preferenceManager: SkySharedPref

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = SkySharedPref.getInstance(this)
        enableEdgeToEdge()

        setContent {
            JGOTheme(themeOverride = preferenceManager.myPrefs.darkMODE) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Select IPTV player 📲") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    AppListScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
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

