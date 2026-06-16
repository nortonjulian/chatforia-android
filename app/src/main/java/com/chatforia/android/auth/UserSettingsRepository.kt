package com.chatforia.android.auth

interface UserSettingsRepository {
    suspend fun updateUsername(
        username: String
    ): UserDto

    suspend fun updateSettings(
        request: SettingsUpdateRequest
    ): UserDto

    suspend fun updateAccessibility(
        request: AccessibilitySettingsUpdateRequest
    ): UserDto

    suspend fun deleteAccount()

    suspend fun removeAvatar(): AvatarResponse
}