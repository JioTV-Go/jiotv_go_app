package com.skylake.skytv.jgorunner.ui.components

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.Helper

@Composable
fun ModernOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = outlineColor,
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null // Handled by the parent Card's clickable modifier
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun JTVModeSelectorPopup(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit,
    preferenceManager: SkySharedPref,
    context: Context
) {
    if (isVisible) {
        var selectedIndex by remember { mutableIntStateOf(preferenceManager.myPrefs.operationMODE) }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(
                    text = "Select Operation Mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModernOptionCard(
                        title = "Simple",
                        description = "Direct operation, best for beginners. Uses default parameters.",
                        isSelected = selectedIndex == 0,
                        onClick = {
                            selectedIndex = 0
                            preferenceManager.myPrefs.operationMODE = selectedIndex
                            preferenceManager.myPrefs.operationUI = 0
                            applySettings(preferenceManager)
                            Helper.setEasyMode(context)
                        }
                    )

                    ModernOptionCard(
                        title = "Expert",
                        description = "Full control over all settings. Suited for advanced users.",
                        isSelected = selectedIndex == 1,
                        onClick = {
                            selectedIndex = 1
                            preferenceManager.myPrefs.operationMODE = selectedIndex
                            applySettings(preferenceManager)
                            Helper.setExpertMode(context)
                        }
                    )
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
                                Process.killProcess(Process.myPid())
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

@Composable
fun JTVUISelectorPopup(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit,
    preferenceManager: SkySharedPref,
    context: Context
) {
    if (isVisible) {
        var selectedIndex by remember { mutableIntStateOf(preferenceManager.myPrefs.operationUI) }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(
                    text = "Select UI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModernOptionCard(
                        title = "Omni UI",
                        description = "Modern interface with easy navigation. Recommended for most users.",
                        isSelected = selectedIndex == 0,
                        onClick = {
                            selectedIndex = 0
                            preferenceManager.myPrefs.operationUI = selectedIndex
                            applySettings(preferenceManager)
                            Helper.setUI0Mode(context)
                        }
                    )

                    ModernOptionCard(
                        title = "Legacy TV UI",
                        description = "Classic JTV-Go interface. Will be removed in a future release.",
                        isSelected = selectedIndex == 1,
                        onClick = {
                            selectedIndex = 1
                            preferenceManager.myPrefs.operationUI = selectedIndex
                            applySettings(preferenceManager)
                            Helper.setUI1Mode(context)
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModeSelected(selectedIndex)
                        onDismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(context, context::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            Process.killProcess(Process.myPid())
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