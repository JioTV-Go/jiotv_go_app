package com.skylake.skytv.jgorunner.ui.tvhome

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class OmniChannel(
    @SerializedName("name") val name: String? = null,
    @SerializedName("group") val group: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("m3u8_url") val m3u8Url: String? = null,
    @SerializedName("mpd_url") val mpdUrl: String? = null
)
