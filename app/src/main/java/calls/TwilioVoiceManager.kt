package com.chatforia.android.calls

import android.content.Context
import android.media.AudioManager
import com.twilio.voice.Call
import com.twilio.voice.CallException
import com.twilio.voice.ConnectOptions
import com.twilio.voice.Voice

class TwilioVoiceManager(
    private val context: Context
) {
    interface Listener {
        fun onRinging() {}
        fun onConnected() {}
        fun onFailed(message: String) {}
        fun onDisconnected() {}
    }

    private var activeCall: Call? = null
    private var isMuted: Boolean = false

    fun startCall(
        accessToken: String,
        to: String,
        listener: Listener
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
                            listener.onFailed(error.message ?: "Call disconnected.")
                        } else {
                            listener.onDisconnected()
                        }
                    }
                }
            )
    }

    fun acceptCall(): Boolean {
        return activeCall != null
    }

    fun endCall() {
        activeCall?.disconnect()
        activeCall = null
        isMuted = false
    }

    fun setMuted(isMuted: Boolean) {
        this.isMuted = isMuted
        activeCall?.mute(isMuted)
    }

    fun setSpeaker(enabled: Boolean) {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = enabled
    }
}