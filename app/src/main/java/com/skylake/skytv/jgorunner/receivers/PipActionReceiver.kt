package com.skylake.skytv.jgorunner.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.skylake.skytv.jgorunner.services.player.PlayerCommandBus
import com.skylake.skytv.jgorunner.data.SkySharedPref

class PipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val pref = SkySharedPref.getInstance(context)
        if (!pref.myPrefs.enablePip) return
        when (intent?.action) {
            ACTION_PREV -> PlayerCommandBus.playPrev()
            ACTION_TOGGLE -> PlayerCommandBus.togglePlayPause()
            ACTION_NEXT -> PlayerCommandBus.playNext()
            ACTION_OPEN -> PlayerCommandBus.requestOpenApp()
            ACTION_CLOSE -> {
                PlayerCommandBus.requestStopPlayback()
                PlayerCommandBus.requestClosePip()
            }
        }
    }

    companion object {
        const val ACTION_PREV = "com.skylake.skytv.jgorunner.PIP_PREV"
        const val ACTION_TOGGLE = "com.skylake.skytv.jgorunner.PIP_TOGGLE"
        const val ACTION_NEXT = "com.skylake.skytv.jgorunner.PIP_NEXT"
        const val ACTION_OPEN = "com.skylake.skytv.jgorunner.PIP_OPEN"
        const val ACTION_CLOSE = "com.skylake.skytv.jgorunner.PIP_CLOSE"
    }
}
