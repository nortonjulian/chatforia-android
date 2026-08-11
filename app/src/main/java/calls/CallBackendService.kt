package com.chatforia.android.calls

enum class CallAnswerClaimResult {
    CLAIMED,
    ANSWERED_ELSEWHERE,
}

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
        durationSec: Int? = null,
        deviceId: String? = null
    )

    /**
     * Marks an explicitly accepted call active.
     *
     * The default implementation preserves existing test fakes.
     */
    fun markCallActive(
        callId: Int,
        deviceId: String? = null
    ): CallAnswerClaimResult =
        CallAnswerClaimResult.CLAIMED

/**
     * Returns the authoritative backend lifecycle state.
     *
     * ACTIVE is the default so existing test fakes preserve their
     * previous immediate-connected behavior unless they override it.
     */
    fun fetchCallStatus(
        callId: Int
    ): CallStatusLookupResponse =
        CallStatusLookupResponse(
            call =
                CallStatusLookupDto(
                    id = callId,
                    status = "ACTIVE"
                )
        )

    fun fetchVoiceToken(
        deviceId: String
    ): VoiceTokenResponse

    /**
     * Confirms that this device successfully registered its
     * device-specific identity with Twilio Voice.
     *
     * Default no-op preserves existing test fakes while production
     * CallService persists the authoritative registration server-side.
     */
    fun confirmVoiceRegistration(
        deviceId: String
    ) = Unit
}