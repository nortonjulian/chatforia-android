package com.chatforia.android.calls

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VideoCallRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun startVideo(
        calleeId: Int,
        chatRoomId: Int? = null
    ): VideoStartResponse {
        return apiClient.send(
            ApiRequest(
                path = "video/start",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    VideoStartRequest(
                        calleeId = calleeId,
                        chatRoomId = chatRoomId
                    )
                ),
                requiresAuth = true
            )
        )
    }

    fun fetchVideoToken(
        identity: String,
        roomName: String
    ): VideoTokenResponse {
        return apiClient.send(
            ApiRequest(
                path = "video/token",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    VideoTokenRequest(
                        identity = identity,
                        room = roomName
                    )
                ),
                requiresAuth = true
            )
        )
    }
}