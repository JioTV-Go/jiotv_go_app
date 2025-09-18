package com.skylake.skytv.jgorunner.ui.components

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.Helper


@Composable
fun JTVModeSelectorPopup(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit,
    preferenceManager: SkySharedPref,
    context: Context
) {


    if (isVisible) {
        var selectedIndex by remember { mutableIntStateOf(3) }


        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(text = "Select Operation Mode")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "JTV-Go Operation Mode",
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { selectedIndex = 0
                                preferenceManager.myPrefs.operationMODE = selectedIndex
                                applySettings(preferenceManager)
                                Helper.setEasyMode(context)
                            },
                            selected = 0 == selectedIndex,
                            label = { Text("Simple") },
                            modifier = Modifier.width(IntrinsicSize.Min)
                        )

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { selectedIndex = 1
                                preferenceManager.myPrefs.operationMODE = selectedIndex
                                applySettings(preferenceManager)
                                Helper.setExpertMode(context)},
                            selected = 1 == selectedIndex,
                            label = { Text("Expert") },
                            modifier = Modifier.width(IntrinsicSize.Min)
                        )
                    }

                    // Display info based on selected option
                    when (selectedIndex) {
                        0 -> {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("• Simple and direct operation")
                                Text("• Best for beginners")
                                Text("• Default parameters")
                            }
                        }
                        1 -> {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("• Full control over all settings")
                                Text("• Suited for advanced users")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModeSelected(selectedIndex)
                        onDismiss()

                        Handler(Looper.getMainLooper()).postDelayed({
                            if (preferenceManager.myPrefs.operationMODE == 0) {
                                val intent = Intent(context, context::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        }, 500)

                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}

fun applySettings(preferenceManager: SkySharedPref) {
    preferenceManager.savePreferences()
}

