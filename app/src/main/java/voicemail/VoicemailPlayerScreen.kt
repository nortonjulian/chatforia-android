package com.chatforia.android.voicemail

import android.media.AudioAttributes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.R
import com.chatforia.android.network.Environment
import com.chatforia.android.ui.theme.ChatforiaColors

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

    val playButtonShape = RoundedCornerShape(28.dp)

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
            text = stringResource(R.string.android_calls_voicemail),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            modifier = Modifier
                .clip(playButtonShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ChatforiaColors.buttonStart,
                            ChatforiaColors.buttonEnd
                        )
                    )
                ),
            enabled = !isPreparing,
            shape = playButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = ChatforiaColors.buttonForeground,
                disabledContainerColor = Color.Transparent,
                disabledContentColor =
                    ChatforiaColors.buttonForeground.copy(alpha = 0.65f)
            ),
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
                            context.getString(
                                R.string.android_voicemail_player_sign_in_again
                            )
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
                                    context.getString(
                                        R.string.android_voicemail_player_unable_to_play
                                    )
                                true
                            }

                            player.prepareAsync()
                        }.onFailure {
                            isPreparing = false
                            isPrepared = false
                            isPlaying = false
                            playbackError =
                                context.getString(
                                    R.string.android_voicemail_player_unable_to_load
                                )
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

                Text(
                    stringResource(
                        R.string.android_voicemail_player_loading
                    )
                )
            } else {
                Text(
                    if (isPlaying) {
                        stringResource(R.string.android_voicemail_player_pause)
                    } else {
                        stringResource(
                            R.string.android_voicemail_inbox_play_voicemail
                        )
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
