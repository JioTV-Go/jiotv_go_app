package com.skylake.skytv.jgorunner.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay

@Composable
fun RedirectPopup(
    appIPTV: String?,
    appIPTVpkg: String?,
    isVisible: Boolean,
    countdownTime: Int,
    onUserCancel: () -> Unit,
    onTimeOut: () -> Unit,
    context: Context
) {
    if (appIPTV == null || appIPTVpkg == null) {
        return
    }

    var currentTime by remember { mutableIntStateOf(countdownTime) }

    // Retrieve the app icon from the package name
    val appIcon = remember {
        try {
            val pm = context.packageManager
            pm.getApplicationIcon(appIPTVpkg)
        } catch (e: Exception) {
            null
        }
    }

    if (isVisible) {
        Log.d("RedirectPopup", "Popup is visible.")

        // Countdown logic
        LaunchedEffect(Unit) {
            currentTime = countdownTime
            while (currentTime > 0) {
                delay(1000)
                currentTime -= 1
            }
            onTimeOut()
        }

        // Log current time
        Log.d("RedirectPopup", "Current countdown: $currentTime")

        AlertDialog(
            onDismissRequest = { onUserCancel()  },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    appIcon?.let {
                        Image(
                            bitmap = it.toBitmap().asImageBitmap(),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = if (appIPTV == "No IPTV")
                            "No IPTV selected."
                        else
                            "Redirecting to $appIPTV",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "$currentTime seconds remaining",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(if (currentTime % 2 == 0) 1f else 0.7f)
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onUserCancel() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Dismiss")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        )
    } else {
        Log.d("RedirectPopup", "Popup is NOT visible.")
    }
}