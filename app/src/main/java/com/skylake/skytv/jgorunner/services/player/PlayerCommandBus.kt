package com.skylake.skytv.jgorunner.services.player

/**
 * A tiny bus to bridge PiP action intents to the running player in Compose.
 * The Compose screen should call [setHandlers] when the player and channel list are ready.
 */
object PlayerCommandBus {
    @Volatile
    private var playPauseHandler: (() -> Unit)? = null

    @Volatile
    private var nextHandler: (() -> Unit)? = null

    @Volatile
    private var prevHandler: (() -> Unit)? = null

    @Volatile
    private var isPlayingProvider: (() -> Boolean)? = null

    @Volatile
    private var onStateChangedHandler: (() -> Unit)? = null

    @Volatile
    private var openAppHandler: (() -> Unit)? = null

    @Volatile
    private var closePipHandler: (() -> Unit)? = null

    @Volatile
    private var stopPlaybackHandler: (() -> Unit)? = null

    /** True when the Activity is in Picture-in-Picture mode. */
    @Volatile
    var isInPipMode: Boolean = false

    /** True during the transition when we request to enter PiP (to avoid pausing on onPause). */
    @Volatile
    var isEnteringPip: Boolean = false

    @Volatile
    private var onPipModeChangedHandler: ((Boolean) -> Unit)? = null

    @Volatile
    private var onSwitchRequestHandler: ((url: String?, index: Int?) -> Unit)? = null

    fun setHandlers(
        playPause: () -> Unit,
        next: () -> Unit,
        prev: () -> Unit,
        isPlaying: () -> Boolean,
    ) {
        playPauseHandler = playPause
        nextHandler = next
        prevHandler = prev
        isPlayingProvider = isPlaying
    }

    fun clearHandlers() {
        playPauseHandler = null
        nextHandler = null
        prevHandler = null
        isPlayingProvider = null
        onStateChangedHandler = null
        openAppHandler = null
        closePipHandler = null
        onSwitchRequestHandler = null
        stopPlaybackHandler = null
    }

    fun togglePlayPause() {
        playPauseHandler?.invoke()
    }

    fun playNext() {
        nextHandler?.invoke()
    }

    fun playPrev() {
        prevHandler?.invoke()
    }

    fun isPlaying(): Boolean = isPlayingProvider?.invoke() ?: false

    fun setOnStateChanged(handler: (() -> Unit)?) {
        onStateChangedHandler = handler
    }

    fun notifyStateChanged() {
        onStateChangedHandler?.invoke()
    }

    fun setPipRequestHandlers(openApp: () -> Unit, closePip: () -> Unit) {
        openAppHandler = openApp
        closePipHandler = closePip
    }

    fun requestOpenApp() {
        openAppHandler?.invoke()
    }

    fun requestClosePip() {
        closePipHandler?.invoke()
    }

    fun setOnStopPlayback(handler: (() -> Unit)?) {
        stopPlaybackHandler = handler
    }

    fun requestStopPlayback() {
        stopPlaybackHandler?.invoke()
    }

    fun setOnPipModeChanged(handler: ((Boolean) -> Unit)?) {
        onPipModeChangedHandler = handler
    }

    fun notifyPipModeChanged(isInPip: Boolean) {
        onPipModeChangedHandler?.invoke(isInPip)
    }

    fun setOnSwitchRequest(handler: ((url: String?, index: Int?) -> Unit)?) {
        onSwitchRequestHandler = handler
    }

    fun requestSwitch(url: String? = null, index: Int? = null) {
        onSwitchRequestHandler?.invoke(url, index)
    }
}
