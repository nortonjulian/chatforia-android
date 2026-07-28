package com.chatforia.android.notifications

import com.chatforia.android.crypto.LinkedDeviceDto

sealed interface PushRegistrationResult {

    data object Success : PushRegistrationResult

    data class ReplacementRequired(
        val existingDevices: List<LinkedDeviceDto>,
        val message: String
    ) : PushRegistrationResult

    data class Failed(
        val message: String
    ) : PushRegistrationResult
}

interface PushTokenRegisterer {

    suspend fun registerCurrentFcmToken(
        replaceDeviceId: String? = null
    ): PushRegistrationResult

    suspend fun unregisterCurrentDevice(
        authToken: String
    ) {
        // Optional for test doubles.
    }
}
