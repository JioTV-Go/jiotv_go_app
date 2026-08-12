package com.skylake.skytv.jgorunner.ui.tvhome

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ChannelResponse(
    @SerializedName("code")
    val code: Int = -1,
    @SerializedName("message")
    val message: String = "",
    @SerializedName("result")
    val result: List<Channel> = emptyList()
)

@Keep
data class Channel(
    @SerializedName("channel_id")
    val channel_id: String = "",
    @SerializedName("channel_name")
    val channel_name: String = "",
    @SerializedName("channel_url")
    val channel_url: String = "",
    @SerializedName("key_url")
    val key_url: String = "",
    @SerializedName("logoUrl")
    val logoUrl: String = "",
    @SerializedName("channelCategoryId")
    val channelCategoryId: Int = 0,
    @SerializedName("channelLanguageId")
    val channelLanguageId: Int = 0,
    @SerializedName("isHD")
    val isHD: Boolean = false
)

@Keep
data class EpgResponse(
    @SerializedName("epg")
    val epg: List<EpgProgram> = emptyList()
)

@Keep
data class EpgProgram(
    @SerializedName("srno")
    val srno: Long = 0L,
    @SerializedName("showId")
    val showId: String = "",
    @SerializedName("showtime")
    val showtime: String = "",
    @SerializedName("showname")
    val showname: String = "",
    @SerializedName("description")
    val description: String = "",
    @SerializedName("duration")
    val duration: Int = 0,
    @SerializedName("endtime")
    val endtime: String = "",
    @SerializedName("channel_name")
    val channel_name: String = "",
    @SerializedName("episodeThumbnail")
    val episodeThumbnail: String = "",
    @SerializedName("episodePoster")
    val episodePoster: String = "",
    @SerializedName("startEpoch")
    val startEpoch: Long = 0L,
    @SerializedName("endEpoch")
    val endEpoch: Long = 0L
)

@Keep
data class M3UChannelExp(
    @SerializedName("name")
    val name: String = "",
    @SerializedName("url")
    val url: String = "",
    @SerializedName("logo")
    val logo: String? = null,
    @SerializedName("category")
    val category: String? = null
)