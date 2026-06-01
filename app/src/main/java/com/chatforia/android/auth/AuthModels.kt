package com.chatforia.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val identifier: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserDto
)

@Serializable
data class MeResponse(
    val user: UserDto
)

@Serializable
data class GoogleLoginRequest(
    val idToken: String
)