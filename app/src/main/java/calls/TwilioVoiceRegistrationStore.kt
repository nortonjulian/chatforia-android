package com.chatforia.android.calls

import android.content.Context

class TwilioVoiceRegistrationStore(
    context: Context
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun matches(
        deviceId: String,
        fcmToken: String
    ): Boolean {
        val normalizedDeviceId = deviceId.trim()
        val normalizedToken = fcmToken.trim()

        if (
            normalizedDeviceId.isBlank() ||
            normalizedToken.isBlank()
        ) {
            return false
        }

        return (
            preferences.getString(KEY_DEVICE_ID, null) ==
                normalizedDeviceId &&
            preferences.getString(KEY_FCM_TOKEN, null) ==
                normalizedToken
        )
    }

    fun save(
        deviceId: String,
        fcmToken: String
    ) {
        val normalizedDeviceId = deviceId.trim()
        val normalizedToken = fcmToken.trim()

        if (
            normalizedDeviceId.isBlank() ||
            normalizedToken.isBlank()
        ) {
            return
        }

        preferences
            .edit()
            .putString(KEY_DEVICE_ID, normalizedDeviceId)
            .putString(KEY_FCM_TOKEN, normalizedToken)
            .apply()
    }

    fun clear(deviceId: String? = null) {
        val normalizedDeviceId =
            deviceId
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        if (
            normalizedDeviceId != null &&
            preferences.getString(KEY_DEVICE_ID, null) !=
                normalizedDeviceId
        ) {
            return
        }

        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME =
            "chatforia_twilio_voice_registration"

        const val KEY_DEVICE_ID = "device_id"
        const val KEY_FCM_TOKEN = "fcm_token"
    }
}
