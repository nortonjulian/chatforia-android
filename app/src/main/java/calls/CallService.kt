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
        durationSec: Int?
    ) {
        apiClient.sendRaw(
            ApiRequest(
                path = "calls/end",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    EndCallRequest(
                        callId = callId,
                        reason = reason,
                        durationSec = durationSec
                    )
                ),
                requiresAuth = true
            )
        )
    }

    override fun fetchVoiceToken(): VoiceTokenResponse {
        return apiClient.send(
            ApiRequest(
                path = "voice/client/token",
                method = HttpMethod.POST,
                bodyJson = """{"platform":"android"}""",
                requiresAuth = true
            )
        )
    }

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

