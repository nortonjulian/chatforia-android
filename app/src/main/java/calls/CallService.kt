package com.chatforia.android.calls

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CallService(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun createAppCall(
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
                            offer = if (video) null else CallOffer(
                                type = "offer",
                                sdp = "android-placeholder"
                            )
                        )
                    ),
                    requiresAuth = true
                )
            )

        return response.resolvedCallId
    }

    fun startExternalCall(phoneNumber: String): Int {
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

    fun endCall(
        callId: Int,
        reason: String? = null,
        durationSec: Int? = null
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

    fun fetchVoiceToken(): VoiceTokenResponse {
        return apiClient.send(
            ApiRequest(
                path = "voice/client/token",
                method = HttpMethod.POST,
                requiresAuth = true
            )
        )
    }
}

