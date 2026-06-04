package com.chatforia.android.contacts

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

class InviteRepository(
    private val apiClient: ApiClient
) {
    suspend fun createInvite(
        targetPhone: String? = null,
        targetEmail: String? = null,
        channel: String = "share_link"
    ): CreateInviteResponse {
        val bodyJson =
            apiClient.json.encodeToString(
                CreateInviteRequest(
                    targetPhone = targetPhone?.trim()?.ifBlank { null },
                    targetEmail = targetEmail?.trim()?.lowercase()?.ifBlank { null },
                    channel = channel
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "people-invites",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }
}