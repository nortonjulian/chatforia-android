package com.chatforia.android.auth

interface AuthSessionRepository {
    suspend fun login(
        identifier: String,
        password: String
    ): UserDto

    suspend fun loginWithGoogle(
        idToken: String
    ): UserDto

    suspend fun completeMfa(mfaToken: String, code: String): UserDto {
        throw UnsupportedOperationException("Two-factor login is unavailable")
    }

    suspend fun fetchMe(): UserDto

    suspend fun rotateEncryptionKey(
        publicKey: String
    )

    suspend fun bootstrap(): UserDto?

    fun saveExternalToken(
        token: String
    )

    fun currentToken(): String? = null

    fun logout()
}
