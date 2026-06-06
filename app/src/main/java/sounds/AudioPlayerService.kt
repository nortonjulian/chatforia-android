package com.chatforia.android.sounds

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AudioPlayerService(
    private val context: Context
) {
    private var player: MediaPlayer? = null

    fun playMessageTone(
        filename: String,
        volume: Int
    ) {
        playSound(filename, volume)
    }

    fun playRingtone(
        filename: String,
        volume: Int
    ) {
        playSound(filename, volume)
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun playSound(
        filename: String,
        volume: Int
    ) {
        stop()

        if (filename.equals("Vibrate.mp3", ignoreCase = true)) {
            vibrate()
            return
        }

        val rawName =
            filename
                .substringBeforeLast(".")
                .lowercase()
                .replace(" ", "_")

        val resId =
            context.resources.getIdentifier(
                rawName,
                "raw",
                context.packageName
            )

        if (resId == 0) {
            return
        }

        val safeVolume =
            volume.coerceIn(0, 100) / 100f

        player =
            MediaPlayer.create(context, resId)?.apply {
                setVolume(safeVolume, safeVolume)
                setOnCompletionListener {
                    stop()
                }
                start()
            }
    }

    private fun vibrate() {
        val vibrator =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                            as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

        vibrator.vibrate(
            VibrationEffect.createOneShot(
                120,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }
}