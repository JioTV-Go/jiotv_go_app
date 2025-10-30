package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.data.SkySharedPref


@Composable
fun OperationModeSetup(preferenceManager: SkySharedPref, isDark: Boolean) {
    val context = LocalContext.current
    val inactiveText = if (isDark) Color(0xFFAAAAEE) else Color(0xFF5C5CA8)
    val activeText = if (isDark) Color.White else Color(0xFF3F3DD9)
    var selectedIndex by remember {
        mutableIntStateOf(preferenceManager.myPrefs.operationMODE)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.Center) {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = selectedIndex == 0,
                    onClick = {
                        selectedIndex = 0
                        preferenceManager.myPrefs.operationMODE = 0

                        preferenceManager.myPrefs.autoStartServer = true
                        preferenceManager.myPrefs.loginChk = true
                        preferenceManager.myPrefs.jtvGoServerPort = 5350
                        preferenceManager.myPrefs.iptvAppPackageName = "tvzone"

                        preferenceManager.savePreferences()

                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = {
                        Text(
                            "Simple",
                            color = if (selectedIndex == 0) activeText else inactiveText
                        )
                    }
                )
                SegmentedButton(
                    selected = selectedIndex == 1,
                    onClick = {
                        selectedIndex = 1
                        preferenceManager.myPrefs.operationMODE = 1

                        preferenceManager.myPrefs.autoStartServer = true
                        preferenceManager.myPrefs.loginChk = true
                        preferenceManager.myPrefs.iptvAppPackageName = ""
                        preferenceManager.myPrefs.startTvAutomatically = false

                        preferenceManager.savePreferences()

                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = {
                        Text(
                            "Expert",
                            color = if (selectedIndex == 1) activeText else inactiveText
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        ModeDescription(selectedIndex, isDark)
    }
}

@Composable
fun ModeDescription(index: Int, isDark: Boolean) {
    val color = if (isDark) Color(0xFFB5B8FF) else Color(0xFF4F46E5)
    val description = when (index) {
        0 -> "Simple Mode: Focused interface for easy use."
        1 -> "Expert Mode: Full control and customization."
        else -> "Please select a mode."
    }
    Text(
        text = description,
        color = color,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}
