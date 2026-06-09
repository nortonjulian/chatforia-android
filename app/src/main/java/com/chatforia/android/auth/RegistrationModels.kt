package com.chatforia.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val smsConsent: Boolean? = null
)

@Serializable
data class RegistrationResponse(
    val message: String? = null,
    val token: String? = null,
    val user: UserDto? = null,
    val privateKey: String? = null,

    val id: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val publicKey: String? = null,
    val plan: String? = null,
    val role: String? = null,
    val preferredLanguage: String? = null,
    val uiLanguage: String? = null,
    val theme: String? = null,
    val avatarUrl: String? = null
) {
    val resolvedUser: UserDto?
        get() {
            user?.let { return it }

            val resolvedId = id ?: return null
            val resolvedUsername = username ?: return null

            return UserDto(
                id = resolvedId,
                email = email,
                username = resolvedUsername,
                publicKey = publicKey,
                plan = plan,
                role = role,
                preferredLanguage = preferredLanguage,
                uiLanguage = uiLanguage ?: preferredLanguage,
                theme = theme,
                avatarUrl = avatarUrl
            )
        }
}