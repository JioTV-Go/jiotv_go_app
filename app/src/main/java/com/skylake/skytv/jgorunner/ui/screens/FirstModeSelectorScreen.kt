package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.Helper
import com.skylake.skytv.jgorunner.activities.MainActivity
import android.content.Intent

@SuppressLint("AutoboxingStateCreation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstModeSelectorScreen(
    preferenceManager: SkySharedPref,
    onModeSelected: (Int) -> Unit = {}
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Operation Mode",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                SegmentedButton(
                    selected = selectedIndex == 0,
                    onClick = {
                        selectedIndex = 0
                        preferenceManager.myPrefs.operationMODE = 0
                        preferenceManager.savePreferences()
                        Helper.setEasyMode(context)
                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = { Text("Simple") }
                )
                SegmentedButton(
                    selected = selectedIndex == 1,
                    onClick = {
                        selectedIndex = 1
                        preferenceManager.myPrefs.operationMODE = 1
                        preferenceManager.savePreferences()
                        Helper.setExpertMode(context)
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = { Text("Expert") }
                )
            }

            ModeDescription(selectedIndex)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onModeSelected(selectedIndex)
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    android.os.Process.killProcess(android.os.Process.myPid())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm")
            }
        }
    }
}

@Composable
fun ModeDescription(selectedIndex: Int) {
    when (selectedIndex) {
        0 -> {
            Column(horizontalAlignment = Alignment.Start) {
                Text("• Simple and direct operation")
                Text("• Ideal for new users")
                Text("• Auto-applies recommended settings")
                Text("• Uses the new TV UI by default")
            }
        }
        1 -> {
            Column(horizontalAlignment = Alignment.Start) {
                Text("• Advanced configs and custom options")
                Text("• Pick your IPTV player")
                Text("• Full access to expert settings")
            }
        }
        -1 -> {
            Column(horizontalAlignment = Alignment.Start) {
                Text("Select operation mode to get started!")
            }
        }
    }
}

