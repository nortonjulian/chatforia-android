package com.chatforia.android.calls

import android.util.Log
import com.twilio.voice.RegistrationException
import com.twilio.voice.RegistrationListener
import com.twilio.voice.Voice
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class TwilioVoicePushRegistrar(
    private val callService: CallBackendService
) {
    suspend fun register(fcmToken: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            try {
                val voiceToken = callService.fetchVoiceToken().token

                Voice.register(
                    voiceToken,
                    Voice.RegistrationChannel.FCM,
                    fcmToken,
                    object : RegistrationListener {
                        override fun onRegistered(
                            accessToken: String,
                            fcmToken: String
                        ) {
                            Log.d(
                                "ChatforiaTwilioVoice",
                                "Twilio Voice FCM registration succeeded: ${fcmToken.take(24)}..."
                            )

                            if (continuation.isActive) {
                                continuation.resume(true)
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
            } catch (e: Exception) {
                Log.e(
                    "ChatforiaTwilioVoice",
                    "Failed before Twilio Voice FCM registration",
                    e
                )

                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }
}