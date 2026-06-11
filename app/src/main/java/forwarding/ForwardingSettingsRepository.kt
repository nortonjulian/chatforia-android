package com.chatforia.android.forwarding

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.encodeToString

class ForwardingSettingsRepository(
    private val apiClient: ApiClient
) {
    suspend fun fetchSettings(): ForwardingSettingsDto {
        return apiClient.send(
            ApiRequest(
                path = "/settings/forwarding",
                method = HttpMethod.GET,
                requiresAuth = true
            )
        )
    }

    suspend fun saveSettings(
        request: ForwardingSettingsDto
    ): ForwardingSettingsDto {
        val bodyJson = apiClient.json.encodeToString(request)

        return apiClient.send(
            ApiRequest(
                path = "/settings/forwarding",
                method = HttpMethod.PATCH,
                bodyJson = bodyJson,
                requiresAuth = true
            )
        )
    }
}