package com.chatforia.android.notifications

import android.util.Log
import com.chatforia.android.crypto.DeviceIdentityStore
import com.chatforia.android.crypto.DeviceRegisterRequest
import com.chatforia.android.crypto.LinkedDevicesDataSource

class PushTokenRegistrar(
    private val deviceIdentityStorage: DeviceIdentityStore,
    private val linkedDevicesRepository: LinkedDevicesDataSource,
    private val fcmTokenProvider: FcmTokenProvider = FirebaseFcmTokenProvider()
) : PushTokenRegisterer {

    override suspend fun registerCurrentFcmToken() {
        try {
            Log.d("ChatforiaFCM", "Starting FCM registration")

            val deviceId = deviceIdentityStorage.getOrCreateDeviceId()
            Log.d("ChatforiaFCM", "Device ID: $deviceId")

            val publicKey = deviceIdentityStorage.getOrCreateKeyPair().first
            Log.d("ChatforiaFCM", "Device public key ready")

            linkedDevicesRepository.registerCurrentDevice(
                DeviceRegisterRequest(
                    deviceId = deviceId,
                    name = "Android Device",
                    platform = "Android",
                    publicKey = publicKey
                )
            )
            Log.d("ChatforiaFCM", "Device registered with backend")

            val token = fcmTokenProvider.currentToken()
            Log.d("ChatforiaFCM", "FCM token acquired: ${token.take(24)}...")

            linkedDevicesRepository.registerPushToken(
                deviceId = deviceId,
                pushToken = token
            )
            Log.d("ChatforiaFCM", "Registered FCM token for device $deviceId")
        } catch (e: Exception) {
            Log.e("ChatforiaFCM", "Failed to register FCM token", e)
        }
    }
}