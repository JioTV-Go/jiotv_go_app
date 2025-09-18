package com.skylake.skytv.jgorunner.ui.tvhome

import androidx.annotation.Keep

@Keep
data class ChannelResponse(
    val code: Int,
    val message: String,
    val result: List<Channel>
)

@Keep
data class Channel(
    val channel_id: String,
    val channel_name: String,
    val channel_url: String,
    val logoUrl: String,
    val channelCategoryId: Int,
    val channelLanguageId: Int,
    val isHD: Boolean
)

@Keep
data class ChannelResponse2(val data: List<Channel>)

// Data classes for EPG
@Keep
data class EpgResponse(val epg: List<EpgProgram>)

@Keep
data class EpgProgram(
    val srno: Long,
    val showId: String,
    val showtime: String,
    val showname: String,
    val description: String,
    val duration: Int,
    val endtime: String,
    val channel_name: String,
    val episodeThumbnail: String,
    val episodePoster: String,
    val startEpoch: Long,
    val endEpoch: Long
)

// Data class for parsed channels
@Keep
data class M3UChannelExp(
    val name: String,
    val url: String,
    val logo: String?,
    val category: String?
)