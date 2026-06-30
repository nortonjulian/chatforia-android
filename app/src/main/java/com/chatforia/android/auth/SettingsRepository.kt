package com.chatforia.android.auth

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(
    private val apiClient: ApiTransport,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserSettingsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Serializable
    private data class UsernameUpdateRequest(
        val username: String
    )

    private suspend fun sendRawRequest(
        request: ApiRequest
    ): String {
        return withContext(ioDispatcher) {
            apiClient.sendRaw(request)
        }
    }

    private suspend inline fun <reified T> sendJson(
        request: ApiRequest
    ): T {
        val responseText = sendRawRequest(request)

        return json.decodeFromString(
            if (responseText.isBlank()) "{}" else responseText
        )
    }

    override suspend fun updateUsername(
        username: String
    ): UserDto {
        val bodyJson =
            json.encodeToString(
                UsernameUpdateRequest(
                    username = username
                )
            )

        return sendJson(
            ApiRequest(
                path = "users/me",
                method = HttpMethod.PATCH,
                bodyJson = bodyJson,
                requiresAuth = true
            )
        )
    }

    override suspend fun updateSettings(
        request: SettingsUpdateRequest
    ): UserDto {
        val bodyJson =
            json.encodeToString(request)

        sendRawRequest(
            ApiRequest(
                path = "users/me",
                method = HttpMethod.PATCH,
                bodyJson = bodyJson,
                requiresAuth = true
            )
        )

        val response: MeResponse =
            sendJson(
                ApiRequest(
                    path = "auth/me",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )

        return response.user
    }

    override suspend fun updateAccessibility(
        request: AccessibilitySettingsUpdateRequest
    ): UserDto {
        sendRawRequest(
            ApiRequest(
                path = "users/me/a11y",
                method = HttpMethod.PATCH,
                bodyJson = json.encodeToString(request),
                requiresAuth = true
            )
        )

        val response: MeResponse =
            sendJson(
                ApiRequest(
                    path = "auth/me",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )

        return response.user
    }

    override suspend fun deleteAccount() {
        sendRawRequest(
            ApiRequest(
                path = "users/me",
                method = HttpMethod.DELETE,
                requiresAuth = true
            )
        )
    }

    override suspend fun removeAvatar(): AvatarResponse {
        return sendJson(
            ApiRequest(
                path = "users/me/avatar",
                method = HttpMethod.DELETE,
                requiresAuth = true
            )
        )
    }
}