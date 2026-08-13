package com.chatforia.android.calls

import android.content.Context
import android.util.Log
import com.twilio.voice.RegistrationException
import com.twilio.voice.RegistrationListener
import com.twilio.voice.Voice
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class TwilioVoicePushRegistrar(
    private val callService: CallBackendService,
    context: Context? = null
) {
    private val registrationStore =
        context?.let {
            TwilioVoiceRegistrationStore(
                it.applicationContext
            )
        }

    suspend fun register(
        fcmToken: String,
        deviceId: String
    ): Boolean {
        val normalizedToken = fcmToken.trim()
        val normalizedDeviceId = deviceId.trim()

        if (
            registrationStore?.matches(
                deviceId = normalizedDeviceId,
                fcmToken = normalizedToken
            ) == true
        ) {
            Log.d(
                "ChatforiaTwilioVoice",
                "Reusing confirmed Twilio Voice registration"
            )

            return true
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val voiceToken =
                    callService
                        .fetchVoiceToken(
                            normalizedDeviceId
                        )
                        .token

                Voice.register(
                    voiceToken,
                    Voice.RegistrationChannel.FCM,
                    normalizedToken,
                    object : RegistrationListener {
                        override fun onRegistered(
                            accessToken: String,
                            fcmToken: String
                        ) {
                            Log.d(
                                "ChatforiaTwilioVoice",
                                "Twilio Voice FCM registration succeeded"
                            )

                            CoroutineScope(
                                Dispatchers.IO
                            ).launch {
                                try {
                                    callService
                                        .confirmVoiceRegistration(
                                            normalizedDeviceId
                                        )

                                    registrationStore?.save(
                                        deviceId =
                                            normalizedDeviceId,
                                        fcmToken =
                                            normalizedToken
                                    )

                                    Log.d(
                                        "ChatforiaTwilioVoice",
                                        "Backend Voice registration confirmation succeeded"
                                    )

                                    if (continuation.isActive) {
                                        continuation.resume(true)
                                    }
                                } catch (error: Exception) {
                                    Log.e(
                                        "ChatforiaTwilioVoice",
                                        "Backend Voice registration confirmation failed",
                                        error
                                    )

                                    if (continuation.isActive) {
                                        continuation.resume(false)
                                    }
                                }
                            }
                        }

                        override fun onError(
                            registrationException: RegistrationException,
                            accessToken: String,
                            fcmToken: String
                        ) {
                            Log.e(
                                "ChatforiaTwilioVoice",
                                "Twilio Voice FCM registration failed: ${registrationException.message}",
                                registrationException
                            )

                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }
                )
            } catch (error: Exception) {
                Log.e(
                    "ChatforiaTwilioVoice",
                    "Failed before Twilio Voice FCM registration",
                    error
                )

                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }
    }

    fun clearRegistration(
        deviceId: String? = null
    ) {
        registrationStore?.clear(deviceId)
    }
}
