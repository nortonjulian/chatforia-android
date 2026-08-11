package com.chatforia.android.notifications

import android.os.Build
import android.util.Log
import com.chatforia.android.calls.TwilioVoicePushRegistrar
import com.chatforia.android.crypto.DeviceIdentityStore
import com.chatforia.android.crypto.DeviceRegisterRequest
import com.chatforia.android.crypto.DeviceReplacementRequiredException
import com.chatforia.android.crypto.DeviceReplacementTargetStaleException
import com.chatforia.android.crypto.LinkedDevicesDataSource

private fun currentAndroidDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.trim()
    val model = Build.MODEL.trim()

    if (model.isBlank()) {
        return "Android Device"
    }

    if (manufacturer.isBlank()) {
        return model
    }

    if (
        model.startsWith(
            manufacturer,
            ignoreCase = true
        )
    ) {
        return model
    }

    val formattedManufacturer =
        manufacturer.replaceFirstChar { character ->
            character.uppercase()
        }

    return "$formattedManufacturer $model"
}

class PushTokenRegistrar(
    private val deviceIdentityStorage: DeviceIdentityStore,
    private val linkedDevicesRepository: LinkedDevicesDataSource,
    private val fcmTokenProvider: FcmTokenProvider =
        FirebaseFcmTokenProvider(),
    private val twilioVoicePushRegistrar:
        TwilioVoicePushRegistrar? = null
) : PushTokenRegisterer {

    override suspend fun registerCurrentFcmToken(
        replaceDeviceId: String?
    ): PushRegistrationResult {
        Log.d(
            "ChatforiaFCM",
            "Starting FCM registration"
        )

        val deviceId =
            try {
                deviceIdentityStorage.getOrCreateDeviceId()
            } catch (error: Exception) {
                Log.e(
                    "ChatforiaFCM",
                    "Could not get device ID",
                    error
                )

                return PushRegistrationResult.Failed(
                    error.message
                        ?: "Could not identify this device."
                )
            }

        Log.d(
            "ChatforiaFCM",
            "Device ID: $deviceId"
        )

        val publicKey =
            try {
                deviceIdentityStorage
                    .getOrCreateKeyPair()
                    .first
            } catch (error: Exception) {
                Log.e(
                    "ChatforiaFCM",
                    "Could not get device public key",
                    error
                )

                return PushRegistrationResult.Failed(
                    error.message
                        ?: "Could not prepare this device's encryption key."
                )
            }

        Log.d(
            "ChatforiaFCM",
            "Device public key ready"
        )

        try {
            linkedDevicesRepository.registerCurrentDevice(
                DeviceRegisterRequest(
                    deviceId = deviceId,
                    name = currentAndroidDeviceName(),
                    platform = "Android",
                    publicKey = publicKey,
                    replaceExistingDevice =
                        replaceDeviceId?.let { true },
                    replaceDeviceId = replaceDeviceId
                )
            )

            Log.d(
                "ChatforiaFCM",
                "Device registered with backend"
            )
        } catch (
            error: DeviceReplacementRequiredException
        ) {
            Log.w(
                "ChatforiaFCM",
                "Device replacement confirmation required"
            )

            return PushRegistrationResult.ReplacementRequired(
                existingDevices = error.existingDevices,
                message = error.message
                    ?: "Confirm which existing device should be replaced."
            )
        } catch (
            error: DeviceReplacementTargetStaleException
        ) {
            Log.w(
                "ChatforiaFCM",
                "Replacement target is stale"
            )

            return PushRegistrationResult.ReplacementRequired(
                existingDevices = error.existingDevices,
                message = error.message
                    ?: "The selected device is no longer active."
            )
        } catch (error: Exception) {
            Log.e(
                "ChatforiaFCM",
                "Device registration failed",
                error
            )

            return PushRegistrationResult.Failed(
                error.message
                    ?: "Could not register this device."
            )
        }

        val token =
            try {
                fcmTokenProvider.currentToken()
            } catch (error: Exception) {
                Log.e(
                    "ChatforiaFCM",
                    "Could not get FCM token",
                    error
                )

                return PushRegistrationResult.Failed(
                    error.message
                        ?: "Could not obtain the notification token."
                )
            }

        Log.d(
            "ChatforiaFCM",
            "FCM token acquired: ${token.take(24)}..."
        )

        try {
            linkedDevicesRepository.registerPushToken(
                deviceId = deviceId,
                pushToken = token
            )

            Log.d(
                "ChatforiaFCM",
                "Registered FCM token with backend for device $deviceId"
            )
        } catch (error: Exception) {
            Log.e(
                "ChatforiaFCM",
                "Backend FCM token registration failed",
                error
            )

            return PushRegistrationResult.Failed(
                error.message
                    ?: "Could not register notifications for this device."
            )
        }

        val twilioRegistered =
            try {
                twilioVoicePushRegistrar
                    ?.register(
                        fcmToken = token,
                        deviceId = deviceId
                    )
                    ?: false
            } catch (error: Exception) {
                Log.e(
                    "ChatforiaTwilioVoice",
                    "Twilio Voice push registration crashed",
                    error
                )

                false
            }

        Log.d(
            "ChatforiaTwilioVoice",
            "Twilio Voice push registration result: $twilioRegistered"
        )

        if (!twilioRegistered) {
            return PushRegistrationResult.Failed(
                "Could not register this device for incoming voice calls."
            )
        }

        return PushRegistrationResult.Success
    }
    override suspend fun unregisterCurrentDevice(
        authToken: String
    ) {
        val deviceId =
            deviceIdentityStorage.getOrCreateDeviceId()

        linkedDevicesRepository.clearPushTokensForCurrentDevice(
            deviceId = deviceId,
            authToken = authToken
        )

        Log.d(
            "ChatforiaFCM",
            "Cleared backend push tokens for logged-out device $deviceId"
        )
    }

}
