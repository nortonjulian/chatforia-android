package com.chatforia.android.calls

interface VideoCallBackend {
    fun startVideo(
        calleeId: Int,
        chatRoomId: Int? = null
    ): VideoStartResponse

    fun fetchVideoToken(
        identity: String,
        roomName: String
    ): VideoTokenResponse
}