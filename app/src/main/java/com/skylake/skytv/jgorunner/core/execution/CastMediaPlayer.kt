package com.skylake.skytv.jgorunner.core.execution

import android.content.Context
import android.util.Log
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.skylake.skytv.jgorunner.data.SkySharedPref

fun castMediaPlayer(context: Context, videoUrl: String) {
    val TAG = "MediaPlayer-DIX"
    val castSession: CastSession? = CastContext.getSharedInstance(context).sessionManager.currentCastSession
    val remoteMediaClient: RemoteMediaClient? = castSession?.remoteMediaClient
    val prefManager = SkySharedPref.getInstance(context)

    val currentChannelName1 = prefManager.myPrefs.castChannelName+""

    Log.d(TAG, currentChannelName1)

    if (remoteMediaClient != null) {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, currentChannelName1)
        }

        val mediaInfo = MediaInfo.Builder(videoUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(metadata)
            .build()


        val loadRequestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()

        remoteMediaClient.load(loadRequestData)
    }
}

