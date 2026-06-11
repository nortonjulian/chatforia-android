package com.chatforia.android.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ChatforiaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("ChatforiaFCM", "New FCM token: $token")

        // Next step: send this token to backend after login.
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