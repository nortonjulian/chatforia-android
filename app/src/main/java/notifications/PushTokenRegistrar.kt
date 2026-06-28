package com.chatforia.android.notifications

import android.util.Log
import com.chatforia.android.crypto.DeviceIdentityStore
import com.chatforia.android.crypto.DeviceRegisterRequest
import com.chatforia.android.crypto.LinkedDevicesDataSource
import com.chatforia.android.calls.TwilioVoicePushRegistrar
class PushTokenRegistrar(
    private val deviceIdentityStorage: DeviceIdentityStore,
    private val linkedDevicesRepository: LinkedDevicesDataSource,
    private val fcmTokenProvider: FcmTokenProvider = FirebaseFcmTokenProvider(),
    private val twilioVoicePushRegistrar: TwilioVoicePushRegistrar? = null
) : PushTokenRegisterer {

    override suspend fun registerCurrentFcmToken() {
        Log.d("ChatforiaFCM", "Starting FCM registration")

        val deviceId =
            try {
                deviceIdentityStorage.getOrCreateDeviceId()
            } catch (e: Exception) {
                Log.e("ChatforiaFCM", "Could not get device ID", e)
                return
            }

        Log.d("ChatforiaFCM", "Device ID: $deviceId")

        val publicKey =
            try {
                deviceIdentityStorage.getOrCreateKeyPair().first
            } catch (e: Exception) {
                Log.e("ChatforiaFCM", "Could not get device public key", e)
                return
            }

        Log.d("ChatforiaFCM", "Device public key ready")

        try {
            linkedDevicesRepository.registerCurrentDevice(
                DeviceRegisterRequest(
                    deviceId = deviceId,
                    name = "Android Device",
                    platform = "Android",
                    publicKey = publicKey
                )
            )

            Log.d("ChatforiaFCM", "Device registered with backend")
        } catch (e: Exception) {
            Log.e(
                "ChatforiaFCM",
                "Device registration failed, continuing with push registration anyway",
                e
            )
        }

        val token =
            try {
                fcmTokenProvider.currentToken()
            } catch (e: Exception) {
                Log.e("ChatforiaFCM", "Could not get FCM token", e)
                return
            }

        Log.d("ChatforiaFCM", "FCM token acquired: ${token.take(24)}...")

        try {
            linkedDevicesRepository.registerPushToken(
                deviceId = deviceId,
                pushToken = token
            )

            Log.d("ChatforiaFCM", "Registered FCM token with backend for device $deviceId")
        } catch (e: Exception) {
            Log.e(
                "ChatforiaFCM",
                "Backend FCM token registration failed, continuing with Twilio registration anyway",
                e
            )
        }

        val twilioRegistered =
            try {
                twilioVoicePushRegistrar?.register(token) ?: false
            } catch (e: Exception) {
                Log.e("ChatforiaTwilioVoice", "Twilio Voice push registration crashed", e)
                false
            }

        Log.d(
            "ChatforiaTwilioVoice",
            "Twilio Voice push registration result: $twilioRegistered"
        )
    }
}