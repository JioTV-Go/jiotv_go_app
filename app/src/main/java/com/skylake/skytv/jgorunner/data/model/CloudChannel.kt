package com.skylake.skytv.jgorunner.data.model

import androidx.annotation.Keep

@Keep
data class CloudChannel(
    val type: String? = null,
    val id: String? = null,
    val name: String,
    val group: String? = null,
    val language: String? = null,
    val logo: String? = null,
    val user_agent: String? = null,
    val mpd_url: String? = null,
    val m3u8_url: String? = null,
    val license_url: String? = null,
    val headers: Map<String, String>? = null,
    val expires_in: String? = null
)
