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
    private var activeCall: Call? = null
    private var isMuted: Boolean = false

    fun prepare(token: String) {
        // Twilio Voice Android does not need a separate prepare step
        // for basic outgoing calls. Token is used when connecting.
    }

    fun startCall(
        accessToken: String,
        to: String
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
                    }

                    override fun onRinging(call: Call) {}

                    override fun onConnected(call: Call) {
                        activeCall = call
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
                    }
                }
            )
    }

    fun acceptCall() {
        // TODO: Incoming Twilio Voice invites require FCM + Voice.handleMessage.
        // Socket incoming calls can still show UI, but true Twilio invite accept
        // comes after push integration.
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