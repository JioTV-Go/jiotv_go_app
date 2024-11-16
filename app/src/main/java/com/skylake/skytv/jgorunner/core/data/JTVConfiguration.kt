package com.skylake.skytv.jgorunner.core.data

import com.google.gson.annotations.SerializedName

data class JTVConfiguration(
    @SerializedName("epg")
    var epg: Boolean = false,

    @SerializedName("debug")
    var debug: Boolean = false,

    @SerializedName("disable_ts_handler")
    var disableTsHandler: Boolean = false,

    @SerializedName("disable_logout")
    var disableLogout: Boolean = false,

    @SerializedName("drm")
    var drm: Boolean = false,

    @SerializedName("title")
    var title: String = "JTV-GO",

    @SerializedName("disable_url_encryption")
    var disableUrlEncryption: Boolean = false,

    @SerializedName("path_prefix")
    var pathPrefix: String = "",

    @SerializedName("proxy")
    var proxy: String = ""
)