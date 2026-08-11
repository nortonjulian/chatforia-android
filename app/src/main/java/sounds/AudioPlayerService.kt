package com.chatforia.android.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AudioPlayerService(
    private val context: Context
) {
    private var player: MediaPlayer? = null

    companion object {
        private const val PREFS_NAME = "chatforia_sound_settings"
        private const val MESSAGE_TONE_KEY = "chatforia.messageTone"
        private const val RINGTONE_KEY = "chatforia.ringtone"
        private const val SOUND_VOLUME_KEY = "chatforia.soundVolume"

        @Volatile
        private var sharedMessageTonePlayer: AudioPlayerService? = null

        @Volatile
        private var sharedIncomingRingtonePlayer: AudioPlayerService? = null

        @Synchronized
        fun playSavedMessageToneShared(context: Context) {
            sharedMessageTonePlayer?.stop()

            val nextPlayer =
                AudioPlayerService(context.applicationContext)

            sharedMessageTonePlayer = nextPlayer
            nextPlayer.playSavedMessageTone()
        }

        @Synchronized
        fun playSavedRingtoneShared(context: Context) {
            if (sharedIncomingRingtonePlayer != null) {
                return
            }

            val nextPlayer =
                AudioPlayerService(context.applicationContext)

            sharedIncomingRingtonePlayer = nextPlayer

            nextPlayer.playSound(
                filename = savedRingtone(context),
                volume = savedSoundVolume(context),
                looping = true,
                usage = AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            )
        }

        @Synchronized
        fun stopSavedRingtoneShared() {
            val currentPlayer = sharedIncomingRingtonePlayer
            sharedIncomingRingtonePlayer = null
            currentPlayer?.stop()
        }

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

        fun saveMessageTone(
            context: Context,
            messageTone: String
        ) {
            context
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                    MESSAGE_TONE_KEY,
                    messageTone
                )
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
        playSound(
            filename = filename,
            volume = volume,
            looping = false,
            usage = AudioAttributes.USAGE_NOTIFICATION
        )
    }

    fun playRingtone(filename: String, volume: Int) {
        playSound(
            filename = filename,
            volume = volume,
            looping = false,
            usage = AudioAttributes.USAGE_NOTIFICATION_RINGTONE
        )
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

    fun playOutgoingRingback() {
        playSound(
            filename = "ringback.wav",
            volume = savedSoundVolume(context),
            looping = true,
            usage = AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING
        )
    }

    fun stop() {
        val currentPlayer = player
        player = null

        currentPlayer?.setOnCompletionListener(null)

        runCatching {
            currentPlayer?.stop()
        }

        currentPlayer?.release()
    }

    private fun playSound(
        filename: String,
        volume: Int,
        looping: Boolean = false,
        usage: Int = AudioAttributes.USAGE_NOTIFICATION
    ) {
        stop()

        val rawName = rawResourceName(filename)

        if (rawName == "vibrate") {
            Log.d(
                "ChatforiaTone",
                "Playing vibration message alert"
            )
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
            Log.e(
                "ChatforiaTone",
                "Sound resource missing: $filename -> $rawName"
            )
            return
        }

        val uri =
            Uri.parse(
                "android.resource://${context.packageName}/$resId"
            )

        val safeVolume =
            volume.coerceIn(0, 100) / 100f

        Log.d(
            "ChatforiaTone",
            "Preparing sound filename=$filename, resource=$rawName, " +
                    "volume=$volume, usage=$usage"
        )

        val nextPlayer = MediaPlayer()

        try {
            nextPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()
            )

            nextPlayer.setDataSource(context, uri)
            nextPlayer.setVolume(safeVolume, safeVolume)
            nextPlayer.isLooping = looping

            if (!looping) {
                nextPlayer.setOnCompletionListener {
                    this@AudioPlayerService.stop()
                }
            }

            nextPlayer.prepare()

            player = nextPlayer
            nextPlayer.start()

            Log.d(
                "ChatforiaTone",
                "Sound playback started: $filename"
            )
        } catch (e: Exception) {
            if (player === nextPlayer) {
                player = null
            }

            runCatching {
                nextPlayer.release()
            }

            Log.e(
                "ChatforiaTone",
                "Sound playback failed: $filename",
                e
            )
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

        val pattern =
            longArrayOf(
                0L,
                500L,
                200L,
                500L
            )

        val attributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    pattern,
                    -1
                ),
                attributes
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                pattern,
                -1,
                attributes
            )
        }
    }
}