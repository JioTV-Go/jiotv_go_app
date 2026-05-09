package com.skylake.skytv.jgorunner.data.model

import androidx.annotation.Keep

@Keep
data class Server(
    val name: String,
    val url: String,
    val logo: String?
)
