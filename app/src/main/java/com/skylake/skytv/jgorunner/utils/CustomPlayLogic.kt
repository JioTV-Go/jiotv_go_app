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

@UnstableApi
fun setupCustomPlaybackLogic(
    exoPlayer: ExoPlayer,
    videoUrl: String,
    onReplay: (() -> Unit)? = null
) {
    var hasSeeked = false

    exoPlayer.addListener(object : Player.Listener {

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY && !hasSeeked && videoUrl.containsAnyId()) {
                exoPlayer.seekTo(14_500)
                hasSeeked = true
            }

            if (state == Player.STATE_ENDED) {
                hasSeeked = false
                onReplay?.invoke() ?: run {
                    exoPlayer.seekTo(14_500)
                    exoPlayer.playWhenReady = true
                }
            }
        }
    })
}
