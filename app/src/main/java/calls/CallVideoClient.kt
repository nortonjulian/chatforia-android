package com.chatforia.android.calls

interface CallVideoClient {
    interface Listener {
        fun onConnected() {}
        fun onFailed(message: String) {}
        fun onDisconnected() {}
    }

    fun connect(
        accessToken: String,
        roomName: String,
        listener: Listener
    )

    fun disconnect()

    fun setMuted(
        isMuted: Boolean
    )

    fun setCameraEnabled(
        enabled: Boolean
    )

    fun flipCamera()
}