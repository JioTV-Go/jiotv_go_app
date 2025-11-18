package com.skylake.skytv.jgorunner.ui.tvhome

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus

/**
 * Wrapper layout exposing zone UI for switching between JioTV and M3U favourites.
 * Keeps states inside each underlying layout; switching does not reset their internal state
 * because they each manage their own remember() scopes.
 */
@Composable
fun MultiZoneFavouriteLayout(
    context: Context,
    startWithM3U: Boolean
) {
    // Start zone aligns with playlist selection so favourites open on the active source
    var activeZone by remember(startWithM3U) {
        mutableStateOf(if (startWithM3U) "M3U" else "JIO")
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zone selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("JIO", "M3U").forEach { zone ->
                val selected = zone == activeZone
                OutlinedButton(
                    onClick = {
                        if (zone != activeZone) {
                            try {
                                PlayerCommandBus.requestStopPlayback()
                                PlayerCommandBus.requestClosePip()
                            } catch (_: Exception) {
                                // best-effort stop; ignore errors
                            }
                        }
                        activeZone = zone
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color(0xFF2962FF).copy(alpha = 0.18f) else Color.Transparent,
                        contentColor = if (selected) Color(0xFF2962FF) else Color.Unspecified
                    )
                ) { Text(if (zone == "JIO") "JioTV" else "M3U", fontSize = 14.sp) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Surface(modifier = Modifier.weight(1f)) {
            when (activeZone) {
                "M3U" -> M3UFavouriteLayout(context)
                else -> FavouriteLayout(context)
            }
        }
    }
}
