package com.skylake.skytv.jgorunner.activities

import android.content.Context
import android.util.Log
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient

fun castVideo(context: Context, videoUrl: String) {
    val castSession: CastSession? = CastContext.getSharedInstance(context).sessionManager.currentCastSession
    val remoteMediaClient: RemoteMediaClient? = castSession?.remoteMediaClient

    if (remoteMediaClient != null) {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, "Live Stream")
            putString(MediaMetadata.KEY_SUBTITLE, "Live Stream Subtitle")
        }


//         Build media info with DASH content type and media tracks
//        val mediaInfo = MediaInfo.Builder(videoUrl)
//            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
//            .setContentType("application/vnd.apple.mpegurl") // Content type for DASH
//            .setMetadata(metadata)
//            .build()

//        val mediaInfo = MediaInfo.Builder(videoUrl)
//            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
//            .setContentType("video/mp4") // Correct MIME type for MP4
//            .setMetadata(metadata)
//            .build() "video/mp4;codecs=hev1.2.4.L153.B0"

        val mediaInfo = MediaInfo.Builder(videoUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_INVALID)
//            .setContentType("application/vnd.apple.mpegurl") // MIME type for MPEG-TS
            .setMetadata(metadata)
            .build()


//        val mediaInfo = MediaInfo.Builder(videoUrl)
//            .setStreamType(MediaInfo.STREAM_TYPE_INVALID)
//            .setMetadata(metadata)
//            .build()



        // Build and load the media content
        val loadRequestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()


        Log.d("DIX-src", "HERE")

        remoteMediaClient.load(loadRequestData)
    }
}

