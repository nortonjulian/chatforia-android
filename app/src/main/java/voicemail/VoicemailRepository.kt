package com.chatforia.android.voicemail

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VoicemailRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun fetchVoicemails(): List<VoicemailDto> {
        val response: VoicemailListResponse =
            apiClient.send(
                ApiRequest(
                    path = "voicemail",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )

        return response.resolvedItems
    }

    fun markRead(id: String, isRead: Boolean) {
        apiClient.sendRaw(
            ApiRequest(
                path = "voicemail/$id/read",
                method = HttpMethod.PATCH,
                bodyJson = json.encodeToString(
                    VoicemailReadRequest(isRead = isRead)
                ),
                requiresAuth = true
            )
        )
    }

    fun deleteVoicemail(id: String) {
        apiClient.sendRaw(
            ApiRequest(
                path = "voicemail/$id",
                method = HttpMethod.DELETE,
                requiresAuth = true
            )
        )
    }
}