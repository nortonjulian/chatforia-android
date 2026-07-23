package com.chatforia.android.voicemail

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.network.Environment

@Composable
fun VoicemailPlayerScreen(
    voicemail: VoicemailDto
) {
    val context = LocalContext.current

    val token = remember {
        TokenStorage(context.applicationContext).read()
    }

    val proxyUrl = remember(voicemail.id) {
        "${Environment.API_BASE_URL}/voicemail/" +
            "${Uri.encode(voicemail.id)}/audio"
    }

    val player = remember(voicemail.id) {
        MediaPlayer()
    }

    var isPrepared by remember(voicemail.id) {
        mutableStateOf(false)
    }

    var isPreparing by remember(voicemail.id) {
        mutableStateOf(false)
    }

    var isPlaying by remember(voicemail.id) {
        mutableStateOf(false)
    }

    var playbackError by remember(voicemail.id) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(player) {
        onDispose {
            runCatching {
                player.stop()
            }

            player.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Selected voicemail",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            enabled = !isPreparing,
            onClick = {
                playbackError = null

                when {
                    isPlaying -> {
                        player.pause()
                        isPlaying = false
                    }

                    isPrepared -> {
                        player.start()
                        isPlaying = true
                    }

                    token.isNullOrBlank() -> {
                        playbackError =
                            "Please sign in again to play this voicemail."
                    }

                    else -> {
                        isPreparing = true

                        runCatching {
                            player.reset()

                            player.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SPEECH
                                    )
                                    .setUsage(
                                        AudioAttributes.USAGE_MEDIA
                                    )
                                    .build()
                            )

                            player.setDataSource(
                                context,
                                Uri.parse(proxyUrl),
                                mapOf(
                                    "Authorization" to "Bearer $token"
                                )
                            )

                            player.setOnPreparedListener {
                                isPreparing = false
                                isPrepared = true
                                isPlaying = true
                                it.start()
                            }

                            player.setOnCompletionListener {
                                isPlaying = false

                                runCatching {
                                    it.seekTo(0)
                                }
                            }

                            player.setOnErrorListener { _, _, _ ->
                                isPreparing = false
                                isPrepared = false
                                isPlaying = false
                                playbackError =
                                    "Unable to play this voicemail."
                                true
                            }

                            player.prepareAsync()
                        }.onFailure {
                            isPreparing = false
                            isPrepared = false
                            isPlaying = false
                            playbackError =
                                "Unable to load this voicemail."
                        }
                    }
                }
            }
        ) {
            if (isPreparing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Loading…")
            } else {
                Text(
                    if (isPlaying) {
                        "Pause"
                    } else {
                        "Play voicemail"
                    }
                )
            }
        }

        playbackError?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!voicemail.transcript.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(voicemail.transcript)
        }
    }
}
