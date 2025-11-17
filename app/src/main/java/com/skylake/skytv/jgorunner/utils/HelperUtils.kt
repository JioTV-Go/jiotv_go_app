package com.skylake.skytv.jgorunner.utils

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
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
import androidx.compose.ui.unit.dp
import com.skylake.skytv.jgorunner.data.SkySharedPref
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
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK &&
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

object DeviceUtils {
    fun isTvDevice(context: Context): Boolean {
        val pm: PackageManager = context.packageManager
        return try {
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        } catch (_: Exception) {
            false
        }
    }

    fun pendingIntentFlags(baseFlags: Int = 0): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            baseFlags or PendingIntent.FLAG_IMMUTABLE
        } else baseFlags
    }
}


fun withQuality(context: Context, chURL: String, logIT: Boolean = false): String {
    val skyPREF = SkySharedPref.getInstance(context).myPrefs
    var videoUrl = chURL

    logIT.takeIf { it }?.let { Log.d("wQTY", "input = $videoUrl") }
    when (skyPREF.filterQX?.lowercase()) {
        "low" -> videoUrl = videoUrl.replace("/live/", "/live/low/")
        "high" -> videoUrl = videoUrl.replace("/live/", "/live/high/")
        "medium" -> videoUrl = videoUrl.replace("/live/", "/live/medium/")
    }
    logIT.takeIf { it }?.let { Log.d("wQTY", "output = $videoUrl") }

    return videoUrl
}
