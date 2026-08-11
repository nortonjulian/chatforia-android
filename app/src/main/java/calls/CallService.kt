package com.chatforia.android.calls

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CallService(
    private val apiClient: ApiClient
) : CallBackendService {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    override fun createAppCall(
        calleeId: Int,
        video: Boolean
    ): Int {
        val response: CreateCallResponse =
            apiClient.send(
                ApiRequest(
                    path = "calls/invite",
                    method = HttpMethod.POST,
                    bodyJson = json.encodeToString(
                        CreateCallRequest(
                            calleeId = calleeId,
                            mode = if (video) "VIDEO" else "AUDIO",
                            offer = null
                        )
                    ),
                    requiresAuth = true
                )
            )

        return response.resolvedCallId
    }

    override fun startExternalCall(phoneNumber: String): Int {
        val response: CreateCallResponse =
            apiClient.send(
                ApiRequest(
                    path = "calls/start-external",
                    method = HttpMethod.POST,
                    bodyJson = json.encodeToString(
                        StartExternalCallRequest(phoneNumber = phoneNumber)
                    ),
                    requiresAuth = true
                )
            )

        return response.resolvedCallId
    }

    fun addParticipant(
        callId: Int,
        userId: Int
    ) {
        apiClient.sendRaw(
            ApiRequest(
                path = "calls/$callId/add-participant",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    AddParticipantRequest(
                        userId = userId,
                        offer = CallOffer(
                            type = "offer",
                            sdp = "android-placeholder"
                        )
                    )
                ),
                requiresAuth = true
            )
        )
    }

    override fun endCall(
        callId: Int,
        reason: String?,
        durationSec: Int?,
        deviceId: String?
    ) {
        apiClient.sendRaw(
            ApiRequest(
                path = "calls/end",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    EndCallRequest(
                        callId = callId,
                        reason = reason,
                        durationSec = durationSec,
                        deviceId = deviceId
                    )
                ),
                requiresAuth = true
            )
        )
    }

    override fun markCallActive(
        callId: Int,
        deviceId: String?
    ): CallAnswerClaimResult {
        return try {
            val bodyJson =
                json.encodeToString(
                    CallStatusUpdateRequest(
                        status = "ACTIVE",
                        deviceId = deviceId
                    )
                )

            apiClient.sendRaw(
                ApiRequest(
                    path = "calls/$callId/status",
                    method = HttpMethod.PATCH,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )

        CallAnswerClaimResult.CLAIMED
    } catch (
        error:
            com.chatforia.android.network.ApiException
    ) {
        if (
            error.statusCode == 409 &&
            error.responseBody.contains(
                "CALL_ANSWERED_ELSEWHERE"
            )
        ) {
            CallAnswerClaimResult
                .ANSWERED_ELSEWHERE
        } else {
            throw error
        }
    }
}

override fun fetchCallStatus(
        callId: Int
    ): CallStatusLookupResponse {
        return apiClient.send(
            ApiRequest(
                path = "calls/$callId/status",
                method = HttpMethod.GET,
                requiresAuth = true
            )
        )
    }

    override fun fetchVoiceToken(
        deviceId: String
    ): VoiceTokenResponse {
        return apiClient.send(
            ApiRequest(
                path = "voice/client/token",
                method = HttpMethod.POST,
                bodyJson =
                    json.encodeToString(
                        VoiceTokenRequest(
                            platform = "android",
                            deviceId = deviceId
                        )
                    ),
                requiresAuth = true
            )
        )
    }

    override fun confirmVoiceRegistration(
        deviceId: String
    ) {
        apiClient.sendRaw(
            ApiRequest(
                path = "voice/client/registration",
                method = HttpMethod.POST,
                bodyJson =
                    json.encodeToString(
                        VoiceRegistrationConfirmationRequest(
                            deviceId = deviceId
                        )
                    ),
                requiresAuth = true
            )
        )
    }

    @kotlinx.serialization.Serializable
    data class VoiceTokenRequest(
        val platform: String,
        val deviceId: String
    )

    @kotlinx.serialization.Serializable
    data class VoiceRegistrationConfirmationRequest(
        val deviceId: String
    )

    @kotlinx.serialization.Serializable
    data class AddParticipantRequest(
        val userId: Int,
        val offer: CallOffer
    )


    @kotlinx.serialization.Serializable
    data class CallOffer(
        val type: String,
        val sdp: String
    )


}

@kotlinx.serialization.Serializable
data class CallStatusLookupResponse(
    val call: CallStatusLookupDto
)

@kotlinx.serialization.Serializable
data class CallStatusLookupDto(
    val id: Int,
    val mode: String? = null,
    val status: String? = null,
    val endReason: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null
)

