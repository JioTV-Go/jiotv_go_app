package com.skylake.skytv.jgorunner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.skylake.skytv.jgorunner.R

val confirmButtonFocusRequesterX = FocusRequester()

@Composable
fun LoginPopup(
    isVisible: Boolean,
    title: String,
    text: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)?,
    onSettingsClick: (Any?) -> Unit
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            confirmButtonFocusRequesterX.requestFocus()
        }
    }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = { if (onDismiss != null) onDismiss() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon: Painter = painterResource(id = R.drawable.logo)
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 9.dp)
                    )
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onConfirm() },
                        modifier = Modifier.focusRequester(confirmButtonFocusRequesterX)
                    ) {
                        Text(confirmButtonText)
                    }
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .size(24.dp)
                            .padding(start = 8.dp)
                            .align(Alignment.CenterVertically)
                            .clickable { onSettingsClick("Login") }
                    )
                }
            },
            dismissButton = {
                if (onDismiss != null)
                    FilledTonalButton(onClick = { onDismiss() }) {
                        Text(dismissButtonText)
                    }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}
