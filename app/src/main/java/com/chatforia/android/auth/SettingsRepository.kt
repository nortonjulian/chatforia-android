package com.chatforia.android.auth

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Serializable
    private data class UsernameUpdateRequest(
        val username: String
    )

    suspend fun updateUsername(
        username: String
    ): UserDto {
        val bodyJson =
            json.encodeToString(
                UsernameUpdateRequest(
                    username = username
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "users/me",
                    method = HttpMethod.PATCH,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun updateSettings(
        request: SettingsUpdateRequest
    ): UserDto {
        val bodyJson =
            json.encodeToString(request)

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "users/me",
                    method = HttpMethod.PATCH,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun deleteAccount() {
        withContext(Dispatchers.IO) {
            apiClient.send<Unit>(
                ApiRequest(
                    path = "users/me",
                    method = HttpMethod.DELETE,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun removeAvatar(): AvatarResponse {
        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "users/me/avatar",
                    method = HttpMethod.DELETE,
                    requiresAuth = true
                )
            )
        }
    }
}