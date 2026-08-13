package com.skylake.skytv.jgorunner.ui.tvhome

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

import java.io.Serializable

@Keep
data class OmniSubtitle(
    @SerializedName("url") val url: String,
    @SerializedName("language") val language: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("mime") val mimeType: String? = null
) : Serializable

@Keep
data class OmniStreamSource(
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String,
    @SerializedName("license_url") val licenseUrl: String? = null,
    @SerializedName("headers") val headers: Map<String, String>? = null
) : Serializable

@Keep
data class OmniChannel(
    @SerializedName("name") val name: String? = null,
    @SerializedName("group") val group: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("m3u8_url") val m3u8Url: String? = null,
    @SerializedName("mpd_url") val mpdUrl: String? = null,
    @SerializedName("license_url") val licenseUrl: String? = null,
    @SerializedName("headers") val headers: Map<String, String>? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("user_agent") val userAgent: String? = null,
    @SerializedName("requiresSubscription") val requiresSubscription: Boolean = false,
    @SerializedName("alt_urls") val altUrls: List<String>? = null,
    @SerializedName("alt_sources") val altSources: List<OmniStreamSource>? = null,
    @SerializedName("subtitles") val subtitles: List<OmniSubtitle>? = null,
    @SerializedName("auto_advance") val autoAdvance: Boolean = false,
    @SerializedName("expires_in") val expiresIn: String? = null
) : Serializable
