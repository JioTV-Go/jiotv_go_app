package com.skylake.skytv.jgorunner.services.player

import android.content.Intent
import androidx.annotation.Keep
import androidx.core.content.IntentCompat
import com.skylake.skytv.jgorunner.activities.ChannelInfo

@Keep
data class IntentParseResult(
    val videoUrl: String?,
    val logoUrl: String?,
    val channelName: String?,
    val channelList: ArrayList<ChannelInfo>?,
    val currentChannelIndex: Int,
    val signature: String?
)

object PlayerIntentHandler {
    private const val DEFAULT_VIDEO = "http://localhost:5001/live/143.m3u8"
    private const val DEFAULT_LOGO = "https://www.sonypicturesnetworks.com/images/logos/SET%20LOGO.png"
    private const val DEFAULT_NAME = "HANA4k"

    fun parse(intent: Intent?): IntentParseResult {
        if (intent == null) {
            return IntentParseResult(DEFAULT_VIDEO, DEFAULT_LOGO, DEFAULT_NAME, null, -1, "0x0")
        }

        val channelList: ArrayList<ChannelInfo>? =
            IntentCompat.getParcelableArrayListExtra(intent, "channel_list_data", ChannelInfo::class.java)

        val currentChannelIndex = intent.getIntExtra("current_channel_index", -1)
        val signature = intent.getStringExtra("zone")

        if (!channelList.isNullOrEmpty() && currentChannelIndex in channelList.indices) {
            val cur = channelList[currentChannelIndex]
            return IntentParseResult(
                videoUrl = cur.videoUrl ?: DEFAULT_VIDEO,
                logoUrl = cur.logoUrl ?: DEFAULT_LOGO,
                channelName = cur.channelName ?: DEFAULT_NAME,
                channelList = channelList,
                currentChannelIndex = currentChannelIndex,
                signature = signature
            )
        }

        val videoUrl = intent.getStringExtra("video_url") ?: DEFAULT_VIDEO
        val logoUrl = intent.getStringExtra("logo_url") ?: DEFAULT_LOGO
        val chName = intent.getStringExtra("ch_name") ?: DEFAULT_NAME

        return IntentParseResult(
            videoUrl = videoUrl,
            logoUrl = logoUrl,
            channelName = chName,
            channelList = channelList,
            currentChannelIndex = currentChannelIndex,
            signature = signature
        )
    }
}
