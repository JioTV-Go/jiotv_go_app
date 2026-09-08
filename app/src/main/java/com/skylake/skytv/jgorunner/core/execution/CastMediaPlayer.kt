package com.skylake.skytv.jgorunner.core.execution

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import org.json.JSONObject

private const val TAG = "MediaPlayer-JGX"


fun castMediaPlayer(context: Context, videoUrl: String, title: String?, logoUrl: String?) {
    val remoteMediaClient = getRemoteMediaClient(context) ?: return

    
    val channelName = title?.takeIf { it.isNotEmpty() } ?: "Streaming"

    val finalVideoUrl = if (videoUrl.endsWith(".m3u8")) videoUrl.replace(".m3u8", ".mpd") else videoUrl
    val licUrl = finalVideoUrl.replace("/live/mpd/", "/live/key/")

    Log.d(TAG, "Casting DASH Stream: $channelName")
    Log.d(TAG, "Video URL: $finalVideoUrl")
    Log.d(TAG, "License URL: $licUrl")

    val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
        putString(MediaMetadata.KEY_TITLE, channelName)
        if (!logoUrl.isNullOrEmpty()) {
            addImage(WebImage(logoUrl.toUri()))
        }
    }

    val customData = JSONObject().apply {
        put("licenseUrl", licUrl)
        put("drmSystem", "com.widevine.alpha")
    }

    val mediaInfo = MediaInfo.Builder(finalVideoUrl)
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

private fun getRemoteMediaClient(context: Context): RemoteMediaClient? {
    val castSession = CastContext.getSharedInstance(context).sessionManager.currentCastSession
    if (castSession == null || !castSession.isConnected) {
        Log.w(TAG, "Cast session is null or disconnected.")
    }
    return castSession?.remoteMediaClient
}