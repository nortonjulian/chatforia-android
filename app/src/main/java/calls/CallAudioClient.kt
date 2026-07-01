package com.chatforia.android.calls

interface CallAudioClient {
    interface Listener {
        fun onConnected()
        fun onFailed(message: String)
        fun onDisconnected()
        fun onRinging() {}
    }

    fun startCall(
        accessToken: String,
        to: String,
        listener: Listener
    )

    fun acceptCall(listener: Listener): Boolean

    fun rejectIncomingCall(): Boolean

    fun endCall()

    fun setMuted(isMuted: Boolean)

    fun setSpeaker(enabled: Boolean)

    fun sendDigits(digits: String)
}