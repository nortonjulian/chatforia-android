package com.chatforia.android.auth

interface AuthSessionRepository {
    suspend fun login(
        identifier: String,
        password: String
    ): UserDto

    suspend fun loginWithGoogle(
        idToken: String
    ): UserDto

    suspend fun fetchMe(): UserDto

    suspend fun rotateEncryptionKey(
        publicKey: String
    )

    suspend fun bootstrap(): UserDto?

    fun saveExternalToken(
        token: String
    )

    fun logout()
}