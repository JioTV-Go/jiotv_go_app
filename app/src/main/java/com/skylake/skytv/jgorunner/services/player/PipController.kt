package com.skylake.skytv.jgorunner.services.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.util.Rational
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.receivers.PipActionReceiver
import com.skylake.skytv.jgorunner.utils.DeviceUtils

class PipController(private val activity: Activity) {

    private val tag = "PipController"
    private var pipActionSeq: Int = 0

    private fun piBroadcast(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(activity, PipActionReceiver::class.java).setAction(action)
        val req = requestCode + (pipActionSeq * 10)
        val flags = DeviceUtils.pendingIntentFlags(0)
        return PendingIntent.getBroadcast(activity, req, intent, flags)
    }

    private fun buildRemoteActions(): List<RemoteAction> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

        pipActionSeq = (pipActionSeq + 1) % 1000

        val iconPrev = Icon.createWithResource(activity, R.drawable.ic_skip_previous_24)
        val iconToggle = if (PlayerCommandBus.isPlaying())
            Icon.createWithResource(activity, R.drawable.ic_pause_24)
        else Icon.createWithResource(activity, R.drawable.ic_play_arrow_24)
        val iconNext = Icon.createWithResource(activity, R.drawable.ic_skip_next_24)
        val iconOpen = Icon.createWithResource(activity, R.drawable.ic_open_in_app_24)
        val iconClose = Icon.createWithResource(activity, R.drawable.ic_close_24)

        val actionPrev = RemoteAction(iconPrev, "Previous", "Previous", piBroadcast(PipActionReceiver.ACTION_PREV, 101))
        val actionToggle = RemoteAction(iconToggle, "⏯", "Play/Pause", piBroadcast(PipActionReceiver.ACTION_TOGGLE, 102))
        val actionNext = RemoteAction(iconNext, "Next", "Next", piBroadcast(PipActionReceiver.ACTION_NEXT, 103))

        val openIntent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val openPi = PendingIntent.getActivity(activity, 104, openIntent, DeviceUtils.pendingIntentFlags(0))
        val actionOpen = RemoteAction(iconOpen, "Open", "Open App", openPi)

        val actionClose = RemoteAction(iconClose, "Close", "Close PiP", piBroadcast(PipActionReceiver.ACTION_CLOSE, 105))

        return listOf(actionPrev, actionToggle, actionNext, actionOpen, actionClose)
    }

    fun enterPipIfAllowed() {
        val prefManager = com.skylake.skytv.jgorunner.data.SkySharedPref.getInstance(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            prefManager.myPrefs.enablePip &&
            !DeviceUtils.isTvDevice(activity)
        ) {
            try {
                PlayerCommandBus.isEnteringPip = true
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setActions(buildRemoteActions())
                    .build()
                activity.enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Log.w(tag, "Failed to enter PiP: ${e.message}")
            }
        }
    }

    fun updatePipActionsIfAllowed() {
        val prefManager = com.skylake.skytv.jgorunner.data.SkySharedPref.getInstance(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            prefManager.myPrefs.enablePip &&
            !DeviceUtils.isTvDevice(activity)
        ) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setActions(buildRemoteActions())
                    .build()
                activity.setPictureInPictureParams(params)
            } catch (e: Exception) {
                Log.w(tag, "Failed to update PiP actions: ${e.message}")
            }
        }
    }
}
