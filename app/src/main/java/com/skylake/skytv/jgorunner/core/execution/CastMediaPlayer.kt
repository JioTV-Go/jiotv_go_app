package com.skylake.skytv.jgorunner.core.execution

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.skylake.skytv.jgorunner.data.SkySharedPref
import org.json.JSONObject

fun castMediaPlayer(context: Context, videoUrl: String) {
    val tag = "MediaPlayer-JGX"
    val castSession: CastSession? =
        CastContext.getSharedInstance(context).sessionManager.currentCastSession
    val remoteMediaClient: RemoteMediaClient? = castSession?.remoteMediaClient
    val prefManager = SkySharedPref.getInstance(context)

    val currentChannelName =
        prefManager.myPrefs.castChannelName?.takeIf { it.isNotEmpty() } ?: "Streaming"

    val channelLogo =
        prefManager.myPrefs.castChannelLogo

    Log.d(tag, currentChannelName)

    val licUrl = videoUrl.replace("/live/mpd/", "/live/key/")

    if (remoteMediaClient != null) {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, currentChannelName)

            if (!channelLogo.isNullOrEmpty()) {
                addImage(WebImage(channelLogo.toUri()))
            }
        }

        val customData = JSONObject().apply {
            put("licenseUrl", licUrl)
            put("drmSystem", "com.widevine.alpha")
        }

        val mediaInfo = MediaInfo.Builder(videoUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType("application/dash+xml")
            .setMetadata(metadata)
            .setCustomData(customData)
            .build()


        val loadRequestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()

        remoteMediaClient.load(loadRequestData)
    }
}






//package com.skylake.skytv.jgorunner.core.execution
//
//import android.content.Context
//import android.net.Uri
//import android.util.Log
//import com.google.android.gms.cast.MediaInfo
//import com.google.android.gms.cast.MediaLoadRequestData
//import com.google.android.gms.cast.MediaMetadata
//import com.google.android.gms.cast.framework.CastContext
//import com.google.android.gms.cast.framework.CastSession
//import com.google.android.gms.cast.framework.media.RemoteMediaClient
//import com.google.android.gms.common.images.WebImage
//import com.skylake.skytv.jgorunner.data.SkySharedPref
//import androidx.core.net.toUri
//
//fun castMediaPlayer(context: Context, videoUrl: String) {
//    val tag = "MediaPlayer-JGX"
//    val castSession: CastSession? =
//        CastContext.getSharedInstance(context).sessionManager.currentCastSession
//    val remoteMediaClient: RemoteMediaClient? = castSession?.remoteMediaClient
//    val prefManager = SkySharedPref.getInstance(context)
//
//    val currentChannelName =
//        prefManager.myPrefs.castChannelName?.takeIf { it.isNotEmpty() } ?: "Streaming"
//
//    val channelLogo =
//        prefManager.myPrefs.castChannelLogo
//
//    Log.d(tag, currentChannelName)
//
//
//    val licUrl = videoUrl.replace("/live/mpd/", "/live/key/")
//
//    if (remoteMediaClient != null) {
//        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
//            putString(MediaMetadata.KEY_TITLE, currentChannelName)
//
//            if (!channelLogo.isNullOrEmpty()) {
//                addImage(WebImage(channelLogo.toUri()))
//            }
//        }
//
//        val mediaInfo = MediaInfo.Builder(videoUrl)
//            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
//            .setMetadata(metadata)
//            .build()
//
//
//        val loadRequestData = MediaLoadRequestData.Builder()
//            .setMediaInfo(mediaInfo)
//            .setAutoplay(true)
//            .build()
//
//        remoteMediaClient.load(loadRequestData)
//    }
//}
//
