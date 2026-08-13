package com.skylake.skytv.jgorunner.utils

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

private val specialIds = listOf(
    "154", "155", "162", "204", "289", "291", "471", "474", "476", "483", "514", "524", "525",
    "697", "872", "873", "874", "891", "892", "1146", "1393", "1396", "1772", "1773", "1774", "1775", "3351"
)

fun String.containsAnyId(): Boolean =
    specialIds.any { this.contains(it, ignoreCase = true) }

// Tracks the single active listener per player so we can remove it before
// adding a new one. Without this, every channel switch accumulates a new
// listener and they all fire seekTo() simultaneously, causing repeated
// STATE_BUFFERING hits and a persistent loading spinner.
private val activeListeners = mutableMapOf<ExoPlayer, Player.Listener>()

/**
 * Call this when the ExoPlayer is about to be released (in onDispose).
 * Removes the listener and drops the map entry so the player can be GC'd.
 */
@UnstableApi
fun cleanupPlaybackLogic(exoPlayer: ExoPlayer) {
    activeListeners.remove(exoPlayer)?.let { exoPlayer.removeListener(it) }
}

@UnstableApi
fun setupCustomPlaybackLogic(
    exoPlayer: ExoPlayer,
    videoUrl: String,
    onReplay: (() -> Unit)? = null
) {
    // Remove any listener registered for this player by a previous channel
    activeListeners.remove(exoPlayer)?.let { exoPlayer.removeListener(it) }

    val listener = object : Player.Listener {

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                onReplay?.invoke() ?: run {
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.playWhenReady = true
                }
            }
        }
    }

    activeListeners[exoPlayer] = listener
    exoPlayer.addListener(listener)
}
