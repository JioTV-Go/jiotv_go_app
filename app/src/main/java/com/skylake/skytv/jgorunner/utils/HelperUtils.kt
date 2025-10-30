package com.skylake.skytv.jgorunner.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun RememberBackPressManager(
    timeoutMs: Long = 2000L,
    onExit: () -> Unit,
    showHint: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var lastPress by remember { mutableLongStateOf(0L) }
    var active by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        val now = System.currentTimeMillis()
        if (active && now - lastPress < timeoutMs) {
            onExit()
        } else {
            active = true
            lastPress = now
            scope.launch { showHint() }
            scope.launch {
                delay(timeoutMs)
                if (System.currentTimeMillis() - lastPress >= timeoutMs) {
                    active = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { active = false }
}

@Composable
fun HandleTvBackKey(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                    keyEvent.type == KeyEventType.KeyUp
                ) {
                    onBack()
                    true
                } else {
                    false
                }
            }
    )
}