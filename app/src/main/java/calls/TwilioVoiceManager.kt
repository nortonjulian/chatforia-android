package com.chatforia.android.calls

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.twilio.voice.Call
import com.twilio.voice.CallException
import com.twilio.voice.ConnectOptions
import com.twilio.voice.Voice

class TwilioVoiceManager(
    private val context: Context
) : CallAudioClient {

    private var activeCall: Call? = null
    private var isMuted: Boolean = false

    override fun startCall(
        accessToken: String,
        to: String,
        listener: CallAudioClient.Listener
    ) {
        val connectOptions =
            ConnectOptions.Builder(accessToken)
                .params(mapOf("To" to to))
                .build()

        activeCall =
            Voice.connect(
                context,
                connectOptions,
                object : Call.Listener {
                    override fun onConnectFailure(
                        call: Call,
                        error: CallException
                    ) {
                        Log.e(
                            "ChatforiaTwilioVoice",
                            "Twilio outgoing connect failure: ${error.message}",
                            error
                        )

                        activeCall = null
                        listener.onFailed(error.message ?: "Call failed.")
                    }

                    override fun onRinging(call: Call) {
                        listener.onRinging()
                    }

                    override fun onConnected(call: Call) {
                        activeCall = call
                        listener.onConnected()
                    }

                    override fun onReconnecting(
                        call: Call,
                        error: CallException
                    ) {}

                    override fun onReconnected(call: Call) {}

                    override fun onDisconnected(
                        call: Call,
                        error: CallException?
                    ) {
                        activeCall = null
                        isMuted = false

                        if (error != null) {
                            Log.w(
                                "ChatforiaTwilioVoice",
                                "Twilio call disconnected with error/warning: ${error.message}",
                                error
                            )
                        }

                        listener.onDisconnected()
                    }
                }
            )
    }

    override fun acceptCall(
        listener: CallAudioClient.Listener
    ): Boolean {
        if (activeCall != null) {
            return true
        }

        val invite =
            TwilioIncomingCallStore.take()
                ?: return false

        return try {
            activeCall =
                invite.accept(
                    context,
                    object : Call.Listener {
                        override fun onConnectFailure(
                            call: Call,
                            error: CallException
                        ) {
                            Log.e(
                                "ChatforiaTwilioVoice",
                                "Twilio incoming connect failure: ${error.message}",
                                error
                            )

                            activeCall = null
                            listener.onFailed(error.message ?: "Incoming call failed.")
                        }

                        override fun onRinging(call: Call) {
                            listener.onRinging()
                        }

                        override fun onConnected(call: Call) {
                            activeCall = call
                            listener.onConnected()
                        }

                        override fun onReconnecting(
                            call: Call,
                            error: CallException
                        ) {}

                        override fun onReconnected(call: Call) {}

                        override fun onDisconnected(
                            call: Call,
                            error: CallException?
                        ) {
                            activeCall = null
                            isMuted = false

                            if (error != null) {
                                Log.w(
                                    "ChatforiaTwilioVoice",
                                    "Twilio call disconnected with error/warning: ${error.message}",
                                    error
                                )
                            }

                            listener.onDisconnected()
                        }
                    }
                )

            true
        } catch (e: Exception) {
            Log.e("ChatforiaTwilioVoice", "Failed to accept Twilio call invite", e)
            activeCall = null
            listener.onFailed(e.message ?: "Could not accept incoming call.")
            false
        }
    }

    override fun rejectIncomingCall(): Boolean {
        val invite =
            TwilioIncomingCallStore.take()
                ?: return false

        return try {
            invite.reject(context)
            true
        } catch (e: Exception) {
            Log.e("ChatforiaTwilioVoice", "Failed to reject Twilio call invite", e)
            false
        } finally {
            TwilioIncomingCallStore.clear()
        }
    }

    override fun endCall() {
        try {
            activeCall?.disconnect()
        } catch (e: Exception) {
            Log.e("ChatforiaTwilioVoice", "Failed to disconnect active call", e)
        }

        try {
            TwilioIncomingCallStore.clear()
        } catch (e: Exception) {
            Log.e("ChatforiaTwilioVoice", "Failed to clear pending invite", e)
        }

        activeCall = null
        isMuted = false
    }

    override fun setMuted(isMuted: Boolean) {
        this.isMuted = isMuted
        activeCall?.mute(isMuted)
    }

    override fun setSpeaker(enabled: Boolean) {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = enabled
    }
}