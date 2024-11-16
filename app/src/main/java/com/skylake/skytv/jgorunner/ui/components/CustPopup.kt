package com.skylake.skytv.jgorunner.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

@Composable
fun CustPopup(
    isVisible: Boolean,
    title: String,
    text: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                Button(onClick = { onConfirm() }) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
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