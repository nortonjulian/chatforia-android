package com.chatforia.android.calls

import com.twilio.video.LocalVideoTrack
import com.twilio.video.RemoteVideoTrack

interface CallVideoClient {
    interface Listener {
        fun onConnected() {}
        fun onFailed(message: String) {}
        fun onDisconnected() {}

        fun onLocalVideoTrack(track: LocalVideoTrack?) {}
        fun onRemoteVideoTrack(track: RemoteVideoTrack?) {}
    }

    fun connect(
        accessToken: String,
        roomName: String,
        listener: Listener
    )

    fun disconnect()

    fun setMuted(isMuted: Boolean)

    fun setCameraEnabled(enabled: Boolean)

    fun flipCamera()
}