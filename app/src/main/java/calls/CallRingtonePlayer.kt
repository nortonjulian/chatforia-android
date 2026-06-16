package com.chatforia.android.calls

import android.content.Context
import com.chatforia.android.sounds.AudioPlayerService

interface CallRingtonePlayer {
    fun playSavedRingtone()

    fun stop()
}

class AudioCallRingtonePlayer(
    context: Context
) : CallRingtonePlayer {
    private val audioPlayerService =
        AudioPlayerService(context)

    override fun playSavedRingtone() {
        audioPlayerService.playSavedRingtone()
    }

    override fun stop() {
        audioPlayerService.stop()
    }
}