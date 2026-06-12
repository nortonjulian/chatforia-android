package com.chatforia.android.notifications

import android.util.Log
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.crypto.DeviceIdentityStorage
import com.chatforia.android.crypto.LinkedDevicesRepository
import com.chatforia.android.network.ApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChatforiaFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("ChatforiaFCM", "New FCM token: ${token.take(24)}...")

        serviceScope.launch {
            try {
                val tokenStorage = TokenStorage(applicationContext)
                val authToken = tokenStorage.read()

                if (authToken.isNullOrBlank()) {
                    Log.d("ChatforiaFCM", "User not logged in; skipping token refresh registration")
                    return@launch
                }

                val apiClient = ApiClient(tokenStorage)

                val deviceIdentityStorage =
                    DeviceIdentityStorage(applicationContext)

                val deviceId =
                    deviceIdentityStorage.getOrCreateDeviceId()

                LinkedDevicesRepository(apiClient)
                    .registerPushToken(
                        deviceId = deviceId,
                        pushToken = token
                    )

                Log.d("ChatforiaFCM", "Refreshed FCM token registered for device $deviceId")

            } catch (e: Exception) {
                Log.e("ChatforiaFCM", "Failed to register refreshed FCM token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("ChatforiaFCM", "FCM data: ${message.data}")

        when (message.data["type"]) {
            "call_incoming" -> {
                NotificationCoordinator(this)
                    .showIncomingCallNotification(message.data)
            }

            "call_missed" -> {
                NotificationCoordinator(this)
                    .showMissedCallNotification(message.data)
            }

            else -> {
                Log.d("ChatforiaFCM", "Unhandled push type: ${message.data["type"]}")
            }
        }
    }
}