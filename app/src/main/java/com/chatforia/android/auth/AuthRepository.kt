package com.chatforia.android.auth

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthRepository(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun login(
        identifier: String,
        password: String
    ): UserDto {
        val bodyJson = json.encodeToString(
            LoginRequest(
                identifier = identifier.trim(),
                password = password
            )
        )

        val response: LoginResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "auth/login",
                        method = HttpMethod.POST,
                        bodyJson = bodyJson,
                        requiresAuth = false
                    )
                )
            }

        tokenStorage.save(response.token)

        return response.user
    }

    suspend fun loginWithGoogle(
        idToken: String
    ): UserDto {
        val bodyJson =
            json.encodeToString(
                GoogleLoginRequest(
                    idToken = idToken
                )
            )

        val response: LoginResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "auth/oauth/google/ios",
                        method = HttpMethod.POST,
                        bodyJson = bodyJson,
                        requiresAuth = false
                    )
                )
            }

        tokenStorage.save(response.token)

        return response.user
    }

    suspend fun fetchMe(): UserDto {
        val response: MeResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "auth/me",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )
            }

        return response.user
    }

    suspend fun bootstrap(): UserDto? {
        val token = tokenStorage.read()

        if (token.isNullOrBlank()) {
            return null
        }

        return try {
            fetchMe()
        } catch (error: Exception) {
            tokenStorage.clear()
            null
        }
    }

    fun logout() {
        tokenStorage.clear()
    }
}