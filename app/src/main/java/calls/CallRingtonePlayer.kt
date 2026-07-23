package com.chatforia.android.calls

import android.content.Context
import com.chatforia.android.sounds.AudioPlayerService

interface CallRingtonePlayer {
    fun playSavedRingtone()

    fun playOutgoingRingback()

    fun stop()
}

class AudioCallRingtonePlayer(
    context: Context
) : CallRingtonePlayer {

    private val appContext =
        context.applicationContext

    private val outgoingAudioPlayerService =
        AudioPlayerService(appContext)

    override fun playSavedRingtone() {
        AudioPlayerService.playSavedRingtoneShared(appContext)
    }

    override fun playOutgoingRingback() {
        outgoingAudioPlayerService.playOutgoingRingback()
    }

    override fun stop() {
        AudioPlayerService.stopSavedRingtoneShared()
        outgoingAudioPlayerService.stop()
    }
}
