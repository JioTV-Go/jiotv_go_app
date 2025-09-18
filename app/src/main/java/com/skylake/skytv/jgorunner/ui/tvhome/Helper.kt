package com.skylake.skytv.jgorunner.ui.tvhome

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.skylake.skytv.jgorunner.activities.ChannelInfo
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.player.ExoPlayJet


object Helper {
    private const val TAG = "[JGO]"
    private const val EASY_MODE = 0
    private const val EXPERT_MODE = 1

    fun setEasyMode(context: Context) {
        val preferenceManager = SkySharedPref(context)
        Toast.makeText(context, "Setting operation mode to SIMPLE", Toast.LENGTH_SHORT).show()
//        Toast.makeText(context, "Applying default settings", Toast.LENGTH_SHORT).show()
        Toast.makeText(context, "Restart JTV-Go App", Toast.LENGTH_LONG).show()
        Log.d(TAG, "Setting operation mode to SIMPLE")

        preferenceManager.myPrefs.autoStartServer = true
        preferenceManager.myPrefs.loginChk = true

        preferenceManager.myPrefs.jtvGoServerPort = 5350
        preferenceManager.myPrefs.iptvAppPackageName = "tvzone"
//        preferenceManager.myPrefs.startTvAutomatically = true


        preferenceManager.myPrefs.operationMODE = EASY_MODE


        preferenceManager.savePreferences()

        Log.d(TAG, "${preferenceManager.myPrefs.operationMODE},${preferenceManager.myPrefs.jtvGoServerPort},${preferenceManager.myPrefs.autoStartServer},")
    }

    fun setExpertMode(context: Context) {
        val preferenceManager = SkySharedPref(context)
        Toast.makeText(context, "Setting operation mode to EXPERT", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Setting operation mode to EXPERT")

        preferenceManager.myPrefs.iptvAppPackageName = ""

        preferenceManager.myPrefs.operationMODE = EXPERT_MODE
        preferenceManager.savePreferences()
    }

}

fun extractChannelIdFromPlayUrl(playUrl: String): String? {
    Log.d("NANOdix0", ">> $playUrl")
    return if ("live" in playUrl) {
        val regex = """.*/(\d+)(?:\.m3u8?|)?$""".toRegex()
        regex.find(playUrl)?.groups?.get(1)?.value
    } else {
        extraLoader(playUrl)
    }
}

val channelDict = mapOf(
    "MC05LTl6NTM4MzQ4Ng==" to "445",
    "MC05LXplZW1hcmF0aGk=" to "1360",
    "Ly1fdzNKYnEzUW9XLW1GQ00yWUl6eEE=" to "1146",
    "MC05LXplZXl1dmE=" to "414",
    "MC05LTl6NTM4MzQ4OQ==" to "153",
    "MC05LXplZXRhbGtpZXM=" to "1358",
    "MC05LTM5NA==" to "2758",
    "MC05LXplZXR2aGQ=" to "167",
    "SGdhQi11NnJTcEd4M21vNFh1M3NMdw==" to "291",
    "L1VJNFFGSl91Ums2YUx4SWNBRHFhX0E=" to "154",
    "L0hfWnZYV3FIUkdLcEhjZERFNVJjREE=" to "1393",
    "L3JQekYyOHFPUmJLWmtoY2lfMDRmZFE=" to "474",
    "MC05LXplZWNpbmVtYQ==" to "484",
    "MC05LXplZWNpbmVtYWhk" to "165",
    "MC05LXBpY3R1cmVz" to "1839",
    "MC05LXR2cGljdHVyZXNoZA==" to "185",
    "L29KLVRHZ1ZGU2dTTUJVb1RrYXV2RlE=" to "289",
    "L1F5cXo0MGJTUXJpcVN1QUM3UjhfRnc=" to "476",
    "LzRKY3UxOTVRVHBDTkJYR25wdzJJNmc=" to "483",
    "MC05LXplZWNsYXNzaWM=" to "487",
    "MC05LXplZWFjdGlvbg==" to "488",
    "MC05LTE3Ng==" to "1691",
    "MC05LXplZWFubW9sY2luZW1h" to "415",
    "MC05LTl6NTU0MzUxNA==" to "1349",
    "MC05LWNoYW5uZWxfMjEwNTMzNTA0Ng==" to "1322",
    )

fun decodeBase64(input: String): String {
    val decodedBytes = Base64.decode(input, Base64.DEFAULT)
    return String(decodedBytes, Charsets.UTF_8)
}

fun extraLoader(playUrl: String): String {
    channelDict.forEach { (encodedKey, code) ->
        val key = decodeBase64(encodedKey)
        if (playUrl.contains(key, ignoreCase = true)) {
            return code
        }
    }
    return ""
}

fun changeIconToSecond(context: Context) {
    val packageManager = context.packageManager
    packageManager.setComponentEnabledSetting(
        ComponentName(context, "com.skylake.skytv.jgorunner.Default"),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, // X
        PackageManager.DONT_KILL_APP
    )

    packageManager.setComponentEnabledSetting(
        ComponentName(context, "com.skylake.skytv.jgorunner.TVBanner"),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, // ON
        PackageManager.DONT_KILL_APP
    )
}

fun changeIconTOFirst(context: Context) {
    val packageManager = context.packageManager
    packageManager.setComponentEnabledSetting(
        ComponentName(context, "com.skylake.skytv.jgorunner.Default"),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, // ON
        PackageManager.DONT_KILL_APP
    )

    packageManager.setComponentEnabledSetting(
        ComponentName(context, "com.skylake.skytv.jgorunner.TVBanner"),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, // X
        PackageManager.DONT_KILL_APP
    )
}


fun switchIcon(enableAlias: String, disableAlias: String, context: Context) {
    val pm = context.packageManager

    val enableComponent = ComponentName(context, "com.skylake.skytv.jgorunner.$enableAlias")
    val disableComponent = ComponentName(context, "com.skylake.skytv.jgorunner.$disableAlias")

    pm.setComponentEnabledSetting(
        enableComponent,
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )

    pm.setComponentEnabledSetting(
        disableComponent,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )

}

fun startChannel(context: Context, firstChannel: Channel, localPORT: Int, channels: List<Channel>) {
    val intent = Intent(context, ExoPlayJet::class.java).apply {
        putExtra("zone", "TV")
        putParcelableArrayListExtra("channel_list_data", ArrayList(
            channels.map { ch ->
                ChannelInfo(
                    ch.channel_url ?: "",
                    "http://localhost:$localPORT/jtvimage/${ch.logoUrl ?: ""}",
                    ch.channel_name ?: ""
                )
            }
        ))
        putExtra("current_channel_index", 0)
        putExtra("video_url", firstChannel.channel_url ?: "")
        putExtra("logo_url", "http://localhost:$localPORT/jtvimage/${firstChannel.logoUrl ?: ""}")
        putExtra("ch_name", firstChannel.channel_name ?: "")
    }
    startActivity(context, intent, null)
}
