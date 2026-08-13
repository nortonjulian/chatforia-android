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
        backendCallId: Int,
        listener: Listener
    )

    fun hasPendingIncomingCall(): Boolean {
        return false
    }

    fun acceptCall(listener: Listener): Boolean

    fun rejectIncomingCall(): Boolean

    fun disconnectActiveCall()

    fun endCall()

    fun setMuted(isMuted: Boolean)

    fun setSpeaker(enabled: Boolean)

    fun sendDigits(digits: String)
}