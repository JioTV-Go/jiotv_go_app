package com.skylake.skytv.jgorunner.services

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

object CastManager {

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    private val _isConnecting = mutableStateOf(false)
    val isConnecting: State<Boolean> = _isConnecting

    private var listener: SessionManagerListener<CastSession>? = null

    fun init(context: Context) {
        val castContext = CastContext.getSharedInstance(context)


        if (listener != null) return // prevent duplicate

        listener = object : SessionManagerListener<CastSession> {

            override fun onSessionStarting(session: CastSession) {
                _isConnecting.value = true
                _isConnected.value = false
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                _isConnecting.value = false
                _isConnected.value = true
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                _isConnecting.value = false
                _isConnected.value = false
            }

            override fun onSessionResuming(session: CastSession, sessionId: String) {
                _isConnecting.value = true
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                _isConnecting.value = false
                _isConnected.value = true
            }

            override fun onSessionEnded(session: CastSession, error: Int) {
                _isConnecting.value = false
                _isConnected.value = false
            }

            override fun onSessionEnding(session: CastSession) {
                _isConnecting.value = false
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                _isConnecting.value = false
                _isConnected.value = false
            }

            override fun onSessionSuspended(session: CastSession, reason: Int) {
                _isConnected.value = false
            }
        }

        castContext.sessionManager.addSessionManagerListener(
            listener!!,
            CastSession::class.java
        )

        // initial state
        _isConnected.value =
            castContext.sessionManager.currentCastSession?.isConnected == true
    }

    private val _isProcessing = mutableStateOf(false)
    val isProcessing: State<Boolean> = _isProcessing

    fun setProcessing(value: Boolean) {
        _isProcessing.value = value
    }
}