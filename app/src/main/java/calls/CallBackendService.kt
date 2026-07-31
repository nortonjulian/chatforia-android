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

    /**
     * Marks an explicitly accepted call active.
     *
     * The default implementation preserves existing test fakes.
     */
    fun markCallActive(callId: Int) {}

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

    fun fetchVoiceToken(): VoiceTokenResponse
}