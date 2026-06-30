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
import com.chatforia.android.calls.TwilioIncomingCallStore
import com.twilio.voice.CallException
import com.twilio.voice.CallInvite
import com.twilio.voice.CancelledCallInvite
import com.twilio.voice.MessageListener
import com.twilio.voice.Voice
import com.chatforia.android.calls.TwilioVoiceCallEvents
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

        val pushData = message.data.toMutableMap()

        message.notification?.title?.let {
            pushData["title"] = it
        }

        message.notification?.body?.let {
            pushData["body"] = it
        }

        val handledByTwilio =
            Voice.handleMessage(
                applicationContext,
                HashMap(message.data),
                object : MessageListener {
                    override fun onCallInvite(callInvite: CallInvite) {
                        Log.d(
                            "ChatforiaTwilioVoice",
                            "Received Twilio call invite: ${callInvite.callSid}"
                        )

                        TwilioIncomingCallStore.save(callInvite)

                        val custom =
                            callInvite.customParameters ?: emptyMap()

                        val data =
                            mapOf(
                                "type" to "call_incoming",
                                "callId" to (custom["callId"] ?: ""),
                                "callerId" to (custom["callerId"] ?: ""),
                                "callerName" to (
                                        custom["callerName"]
                                            ?: callInvite.from
                                            ?: "Incoming call"
                                        ),
                                "fromNumber" to (
                                        custom["fromNumber"]
                                            ?: callInvite.from
                                            ?: "Unknown caller"
                                        ),
                                "mode" to "AUDIO",
                                "roomName" to ""
                            )

                        NotificationCoordinator(this@ChatforiaFirebaseMessagingService)
                            .showIncomingCallNotification(data)
                    }

                    override fun onCancelledCallInvite(
                        cancelledCallInvite: CancelledCallInvite,
                        callException: CallException?
                    ) {
                        Log.d(
                            "ChatforiaTwilioVoice",
                            "Twilio call invite cancelled"
                        )

                        TwilioIncomingCallStore.clear()

                        NotificationCoordinator(this@ChatforiaFirebaseMessagingService)
                            .cancelIncomingCallNotification()

                        TwilioVoiceCallEvents.notifyRemoteEnded()
                    }
                }
            )

        if (handledByTwilio) {
            return
        }

        when (pushData["type"]) {
            "message_new" -> {
                NotificationCoordinator(this)
                    .showMessageNotification(pushData)
            }

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