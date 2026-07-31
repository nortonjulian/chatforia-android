package com.chatforia.android.notifications

import android.util.Log
import com.chatforia.android.ChatforiaAppState
import com.chatforia.android.sounds.AudioPlayerService
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
import com.chatforia.android.calls.CallService
import com.chatforia.android.calls.TwilioVoicePushRegistrar
import com.chatforia.android.calls.CallLifecyclePushEvents
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

        Log.d("ChatforiaFCM", "Received refreshed FCM token")

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

                Log.d(
                    "ChatforiaFCM",
                    "Refreshed FCM token registered with backend for device $deviceId"
                )

                val twilioRegistered =
                    TwilioVoicePushRegistrar(
                        callService = CallService(apiClient)
                    ).register(token)

                if (twilioRegistered) {
                    Log.d(
                        "ChatforiaTwilioVoice",
                        "Refreshed FCM token registered with Twilio Voice"
                    )
                } else {
                    Log.w(
                        "ChatforiaTwilioVoice",
                        "Could not register refreshed FCM token with Twilio Voice"
                    )
                }

            } catch (e: Exception) {
                Log.e("ChatforiaFCM", "Failed to register refreshed FCM token", e)
            }
        }
    }

    private fun isIncomingCallStillLive(
        pushData: Map<String, String>
    ): Boolean {
        val callId =
            pushData["callId"]
                ?.toIntOrNull()
                ?: return true

        return try {
            val tokenStorage =
                TokenStorage(applicationContext)

            val authToken = tokenStorage.read()

            if (authToken.isNullOrBlank()) {
                return true
            }

            val response =
                CallService(
                    ApiClient(tokenStorage)
                ).fetchCallStatus(callId)

            when (
                response.call.status?.uppercase()
            ) {
                "INITIATED",
                "RINGING" -> true

                "ACTIVE",
                "ENDED",
                "DECLINED",
                "MISSED",
                "FAILED" -> false

                else -> true
            }
        } catch (error: Exception) {
            // A temporary lookup failure should not block a valid call.
            Log.w(
                "ChatforiaFCM",
                "Unable to validate incoming call $callId; " +
                    "using push freshness fallback.",
                error
            )

            true
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(
            "ChatforiaFCM",
            "Received push type=${message.data["type"] ?: "unknown"}"
        )

        val pushData = message.data.toMutableMap()

        message.notification?.title?.let {
            pushData["title"] = it
        }

        message.notification?.body?.let {
            pushData["body"] = it
        }

        val pushType = pushData["type"]

        val isRecognizedChatforiaPush =
            pushType == "message_new" ||
                pushType == "call_incoming" ||
                pushType == "call_missed" ||
                pushType == "call_ended"

        if (isRecognizedChatforiaPush) {
            Log.d(
                "ChatforiaFCM",
                "Routing recognized Chatforia push directly: type=$pushType"
            )
        }

        val handledByTwilio =
            if (isRecognizedChatforiaPush) {
                false
            } else {
                Voice.handleMessage(
                applicationContext,
                HashMap(message.data),
                object : MessageListener {
                    override fun onCallInvite(callInvite: CallInvite) {
                        if (
                            IncomingCallFreshness.isExpired(
                                message.sentTime
                            )
                        ) {
                            Log.w(
                                "ChatforiaTwilioVoice",
                                "Discarding expired Twilio call invite: ${callInvite.callSid}"
                            )

                            try {
                                callInvite.reject(applicationContext)
                            } catch (e: Exception) {
                                Log.w(
                                    "ChatforiaTwilioVoice",
                                    "Expired Twilio invite could not be rejected",
                                    e
                                )
                            }

                            TwilioIncomingCallStore.clear()

                            IncomingCallDisplayStore.clear(
                                applicationContext
                            )

                            NotificationCoordinator(
                                this@ChatforiaFirebaseMessagingService
                            ).cancelIncomingCallNotification()

                            return
                        }

                        Log.d(
                            "ChatforiaTwilioVoice",
                            "Received Twilio call invite: ${callInvite.callSid}"
                        )

                        TwilioIncomingCallStore.save(callInvite)

                        val custom =
                            callInvite.customParameters ?: emptyMap()

                        val cached =
                            IncomingCallDisplayStore.recent(
                                applicationContext
                            )

                        fun friendlyValue(
                            value: String?
                        ): String? {
                            return value
                                ?.takeIf { it.isNotBlank() }
                                ?.takeUnless {
                                    it.startsWith(
                                        "client:user_",
                                        ignoreCase = true
                                    )
                                }
                        }

                        val callerName =
                            friendlyValue(cached?.get("callerName"))
                                ?: friendlyValue(custom["callerName"])
                                ?: friendlyValue(callInvite.from)
                                ?: "Chatforia user"

                        val data =
                            mapOf(
                                "type" to "call_incoming",
                                "callId" to (
                                    custom["callId"]
                                        ?.takeIf { it.isNotBlank() }
                                        ?: cached?.get("callId")
                                        ?: ""
                                    ),
                                "callerId" to (
                                    custom["callerId"]
                                        ?.takeIf { it.isNotBlank() }
                                        ?: cached?.get("callerId")
                                        ?: ""
                                    ),
                                "callerName" to callerName,
                                "fromNumber" to (
                                    friendlyValue(custom["fromNumber"])
                                        ?: friendlyValue(
                                            cached?.get("fromNumber")
                                        )
                                        ?: callerName
                                    ),
                                "mode" to (
                                    cached?.get("mode")
                                        ?: "AUDIO"
                                    ),
                                "roomName" to (
                                    custom["roomName"]
                                        ?.takeIf { it.isNotBlank() }
                                        ?: cached?.get("roomName")
                                        ?: ""
                                    )
                            )

                        NotificationCoordinator(this@ChatforiaFirebaseMessagingService)
                            .showIncomingCallNotification(data)
                    }

                    override fun onCancelledCallInvite(
                        cancelledCallInvite: CancelledCallInvite,
                        callException: CallException?
                    ) {
                        val cancelledCallSid =
                            cancelledCallInvite.callSid

                        val pendingCallSid =
                            TwilioIncomingCallStore.peek()?.callSid

                        if (
                            pendingCallSid.isNullOrBlank() ||
                            cancelledCallSid.isNullOrBlank() ||
                            pendingCallSid != cancelledCallSid
                        ) {
                            Log.d(
                                "ChatforiaTwilioVoice",
                                "Ignoring cancellation for non-current invite: " +
                                    "cancelledCallSid=$cancelledCallSid " +
                                    "pendingCallSid=$pendingCallSid"
                            )

                            return
                        }

                        Log.d(
                            "ChatforiaTwilioVoice",
                            "Twilio call invite cancelled: $cancelledCallSid"
                        )

                        TwilioIncomingCallStore.clear()
                        IncomingCallDisplayStore.clear(
                            applicationContext
                        )

                        NotificationCoordinator(this@ChatforiaFirebaseMessagingService)
                            .cancelIncomingCallNotification()

                        TwilioVoiceCallEvents.notifyRemoteEnded()
                    }
                }
                )
            }

        if (handledByTwilio) {
            Log.d(
                "ChatforiaTwilioVoice",
                "Twilio handled incoming FCM payload"
            )
            return
        }

        if (pushData["type"].isNullOrBlank()) {
            Log.e(
                "ChatforiaTwilioVoice",
                "Twilio rejected incoming FCM payload; keys=" +
                    message.data.keys.sorted().joinToString(",")
            )
        }

        when (pushData["type"]) {
            "message_new" -> {
                if (ChatforiaAppState.isInForeground) {
                    Log.d(
                        "ChatforiaFCM",
                        "App is foreground; playing selected Chatforia message tone"
                    )

                    AudioPlayerService
                        .playSavedMessageToneShared(
                            applicationContext
                        )
                } else {
                    NotificationCoordinator(this)
                        .showMessageNotification(pushData)
                }
            }

            "call_incoming" -> {
                if (
                    IncomingCallFreshness.isExpired(
                        message.sentTime
                    )
                ) {
                    Log.w(
                        "ChatforiaFCM",
                        "Discarding expired Chatforia incoming-call push"
                    )

                    IncomingCallDisplayStore.clear(
                        applicationContext
                    )

                    NotificationCoordinator(this)
                        .cancelIncomingCallNotification()

                    return
                }

                if (!isIncomingCallStillLive(pushData)) {
                    Log.w(
                        "ChatforiaFCM",
                        "Discarding terminal or already-active " +
                            "incoming call ${pushData["callId"]}"
                    )

                    IncomingCallDisplayStore.clear(
                        applicationContext
                    )

                    NotificationCoordinator(this)
                        .cancelIncomingCallNotification()

                    CallLifecyclePushEvents
                        .notifyHistoryRefresh()

                    return
                }

                IncomingCallDisplayStore.save(
                    applicationContext,
                    pushData
                )

                NotificationCoordinator(this)
                    .showIncomingCallNotification(pushData)
            }

            "call_ended" -> {
                val endedCallId =
                    pushData["callId"]
                        ?.toIntOrNull()

                val cachedCallId =
                    IncomingCallDisplayStore
                        .recent(applicationContext)
                        ?.get("callId")
                        ?.toIntOrNull()

                if (
                    endedCallId == null ||
                    cachedCallId == null ||
                    endedCallId == cachedCallId
                ) {
                    TwilioIncomingCallStore.clear()

                    IncomingCallDisplayStore.clear(
                        applicationContext
                    )

                    NotificationCoordinator(this)
                        .cancelIncomingCallNotification()
                }

                CallLifecyclePushEvents.notifyTerminal(
                    callId = endedCallId,
                    status = pushData["status"],
                    reason = pushData["reason"],
                    mode = pushData["mode"]
                )

                CallLifecyclePushEvents
                    .notifyHistoryRefresh()
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