package com.chatforia.android.auth

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthRepository(
    private val apiClient: ApiTransport,
    private val tokenStorage: AuthTokenStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthSessionRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

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

    override suspend fun login(
        identifier: String,
        password: String
    ): UserDto {
        val bodyJson =
            json.encodeToString(
                LoginRequest(
                    identifier = identifier.trim(),
                    password = password
                )
            )

        val response: LoginResponse =
            sendJson(
                ApiRequest(
                    path = "auth/login",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = false
                )
            )

        tokenStorage.save(response.token)

        return response.user
    }

    override suspend fun loginWithGoogle(
        idToken: String
    ): UserDto {
        val bodyJson =
            json.encodeToString(
                GoogleLoginRequest(
                    idToken = idToken
                )
            )

        val response: LoginResponse =
            sendJson(
                ApiRequest(
                    path = "auth/oauth/google/android",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = false
                )
            )

        tokenStorage.save(response.token)

        return response.user
    }

    override suspend fun fetchMe(): UserDto {
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

    override suspend fun rotateEncryptionKey(publicKey: String) {
        val bodyJson =
            json.encodeToString(
                RotateEncryptionKeyRequest(
                    publicKey = publicKey,
                    invalidateExistingBackup = true
                )
            )

        sendRawRequest(
            ApiRequest(
                path = "auth/keys/rotate",
                method = HttpMethod.POST,
                bodyJson = bodyJson,
                requiresAuth = true
            )
        )
    }

    override suspend fun bootstrap(): UserDto? {
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

        sendRawRequest(
            ApiRequest(
                path = "auth/resend-email",
                method = HttpMethod.POST,
                bodyJson = bodyJson,
                requiresAuth = false
            )
        )
    }

    suspend fun forgotPassword(identifier: String) {
        val bodyJson =
            json.encodeToString(
                ForgotPasswordRequest(
                    identifier = identifier.trim()
                )
            )

        sendRawRequest(
            ApiRequest(
                path = "auth/forgot-password",
                method = HttpMethod.POST,
                bodyJson = bodyJson,
                requiresAuth = false
            )
        )
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

        return sendJson(
            ApiRequest(
                path = "auth/register",
                method = HttpMethod.POST,
                bodyJson = bodyJson,
                requiresAuth = false
            )
        )
    }

    override fun saveExternalToken(token: String) {
        tokenStorage.save(token)
    }

    override fun logout() {
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