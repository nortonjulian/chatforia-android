package com.chatforia.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class AvatarResponse(
    val avatarUrl: String? = null
)