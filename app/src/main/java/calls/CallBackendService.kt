package com.chatforia.android.calls

interface CallBackendService {
    fun createAppCall(
        calleeId: Int,
        video: Boolean
    ): Int

    fun startExternalCall(
        phoneNumber: String
    ): Int

    fun endCall(
        callId: Int,
        reason: String? = null,
        durationSec: Int? = null
    )

    fun fetchVoiceToken(): VoiceTokenResponse
}