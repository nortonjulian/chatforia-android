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
                        path = "auth/oauth/google/android",
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

    suspend fun rotateEncryptionKey(publicKey: String) {
        val bodyJson = json.encodeToString(
            RotateEncryptionKeyRequest(
                publicKey = publicKey,
                invalidateExistingBackup = true
            )
        )

        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
                ApiRequest(
                    path = "auth/keys/rotate",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
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

    suspend fun resendVerificationEmail(email: String) {
        val bodyJson =
            json.encodeToString(
                ResendVerificationRequest(
                    email = email.trim()
                )
            )

        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
                ApiRequest(
                    path = "auth/resend-email",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = false
                )
            )
        }
    }

    suspend fun forgotPassword(identifier: String) {
        val bodyJson =
            json.encodeToString(
                ForgotPasswordRequest(
                    identifier = identifier.trim()
                )
            )

        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
                ApiRequest(
                    path = "auth/forgot-password",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = false
                )
            )
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        phone: String? = null,
        smsConsent: Boolean? = null
    ): RegistrationResponse {
        val trimmedPhone =
            phone
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val bodyJson =
            json.encodeToString(
                RegistrationRequest(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    phone = trimmedPhone,
                    smsConsent =
                        if (trimmedPhone == null) null else smsConsent
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "auth/register",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = false
                )
            )
        }
    }

    fun saveExternalToken(token: String) {
        tokenStorage.save(token)
    }

    fun logout() {
        tokenStorage.clear()
    }
}

@kotlinx.serialization.Serializable
data class RotateEncryptionKeyRequest(
    val publicKey: String,
    val invalidateExistingBackup: Boolean = true
)

@kotlinx.serialization.Serializable
data class ForgotPasswordRequest(
    val identifier: String
)

@kotlinx.serialization.Serializable
data class ResendVerificationRequest(
    val email: String
)
