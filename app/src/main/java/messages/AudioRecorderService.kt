package com.chatforia.android.messages

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.util.UUID

class AudioRecorderService(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt: Long = 0L

    fun start() {
        val output = File(context.cacheDir, "voice-${UUID.randomUUID()}.m4a")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(12000)
            setAudioChannels(1)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }

        file = output
        startedAt = System.currentTimeMillis()
    }

    fun stop(): VoiceNoteDraft? {
        val currentFile = file ?: return null

        runCatching {
            recorder?.stop()
        }

        recorder?.release()
        recorder = null

        val duration = ((System.currentTimeMillis() - startedAt) / 1000.0)
            .coerceAtLeast(1.0)

        file = null
        startedAt = 0L

        return VoiceNoteDraft(
            file = currentFile,
            durationSec = duration
        )
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        file?.delete()
        file = null
        startedAt = 0L
    }
}