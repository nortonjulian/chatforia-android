package com.chatforia.android.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AudioPlayerService(
    private val context: Context
) {
    private var player: MediaPlayer? = null

    companion object {
        private const val PREFS_NAME = "chatforia_sound_settings"
        private const val MESSAGE_TONE_KEY = "chatforia.messageTone"
        private const val RINGTONE_KEY = "chatforia.ringtone"
        private const val SOUND_VOLUME_KEY = "chatforia.soundVolume"

        fun save(
            context: Context,
            messageTone: String,
            ringtone: String,
            soundVolume: Int
        ) {
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(MESSAGE_TONE_KEY, messageTone)
                .putString(RINGTONE_KEY, ringtone)
                .putInt(SOUND_VOLUME_KEY, soundVolume)
                .apply()
        }

        fun savedMessageTone(context: Context): String {
            return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(MESSAGE_TONE_KEY, "Default.mp3")
                ?: "Default.mp3"
        }

        fun savedRingtone(context: Context): String {
            return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(RINGTONE_KEY, "Classic.mp3")
                ?: "Classic.mp3"
        }

        fun savedSoundVolume(context: Context): Int {
            return context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(SOUND_VOLUME_KEY, 70)
        }
    }

    fun playMessageTone(filename: String, volume: Int) {
        playSound(filename, volume)
    }

    fun playRingtone(filename: String, volume: Int) {
        playSound(filename, volume)
    }

    fun playSavedMessageTone() {
        playMessageTone(
            filename = savedMessageTone(context),
            volume = savedSoundVolume(context)
        )
    }

    fun playSavedRingtone() {
        playRingtone(
            filename = savedRingtone(context),
            volume = savedSoundVolume(context)
        )
    }

    fun stop() {
        player?.setOnCompletionListener(null)
        player?.stop()
        player?.release()
        player = null
    }

    private fun playSound(filename: String, volume: Int) {
        stop()

        val rawName = rawResourceName(filename)

        if (rawName == "vibrate") {
            vibrate()
            return
        }

        val resId =
            context.resources.getIdentifier(
                rawName,
                "raw",
                context.packageName
            )

        if (resId == 0) {
            println("❌ Sound resource not found: $filename -> $rawName")
            return
        }

        val uri =
            Uri.parse("android.resource://${context.packageName}/$resId")

        val safeVolume =
            volume.coerceIn(0, 100) / 100f

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            setDataSource(context, uri)
            setVolume(safeVolume, safeVolume)

            setOnCompletionListener {
                stop()
            }

            prepare()
            start()
        }
    }

    private fun rawResourceName(filename: String): String {
        val base =
            filename
                .substringBeforeLast(".")
                .lowercase()
                .replace(" ", "_")
                .replace("-", "_")

        return when (base) {
            "default" -> "default_tone"
            else -> base
        }
    }

    private fun vibrate() {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                            as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    500,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}