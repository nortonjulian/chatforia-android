package com.chatforia.android.voicemail

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class DeleteSelectedVoicemailsRequest(
    val ids: List<String>
)

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

    fun deleteSelected(ids: Collection<String>) {
        val normalizedIds =
            ids
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

        if (normalizedIds.isEmpty()) return

        apiClient.sendRaw(
            ApiRequest(
                path = "voicemail/delete-selected",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    DeleteSelectedVoicemailsRequest(
                        ids = normalizedIds
                    )
                ),
                requiresAuth = true
            )
        )
    }

    fun clearVoicemails() {
        apiClient.sendRaw(
            ApiRequest(
                path = "voicemail",
                method = HttpMethod.DELETE,
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