package com.chatforia.android.voicemail

import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chatforia.android.R
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.network.Environment
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri

private enum class VoicemailAudioRouteKind {
    Earpiece,
    Speaker,
    Bluetooth,
    Headphones
}

private data class VoicemailAudioRouteOption(
    val kind: VoicemailAudioRouteKind,
    val device: AudioDeviceInfo
)

@Composable
fun VoicemailPlayerScreen(
    voicemail: VoicemailDto
) {
    val context = LocalContext.current

    val signInAgainMessage =
        stringResource(R.string.android_voicemail_player_sign_in_again)

    val unableToPlayMessage =
        stringResource(R.string.android_voicemail_player_unable_to_play)

    val unableToLoadMessage =
        stringResource(R.string.android_voicemail_player_unable_to_load)

    val audioOutputUnavailableMessage =
        stringResource(R.string.android_voicemail_audio_output_unavailable)

    val audioManager = remember {
        context.getSystemService(AudioManager::class.java)
    }

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

    var positionMs by remember(voicemail.id) { mutableStateOf(0) }
    var durationMs by remember(voicemail.id) {
        mutableStateOf((voicemail.durationSec ?: 0).coerceAtLeast(0) * 1000)
    }

    var isSpeakerEnabled by remember(voicemail.id) {
        mutableStateOf(false)
    }

    var playbackError by remember(voicemail.id) {
        mutableStateOf<String?>(null)
    }

    var routeOptions by remember(voicemail.id) {
        mutableStateOf<List<VoicemailAudioRouteOption>>(emptyList())
    }

    val selectedRoute =
        selectedVoicemailRoute(
            routes = routeOptions,
            isSpeakerEnabled = isSpeakerEnabled
        )

    val playButtonShape = RoundedCornerShape(28.dp)

    LaunchedEffect(player, isPlaying) {
        while (isPlaying) {
            positionMs = runCatching { player.currentPosition }.getOrDefault(0).coerceAtLeast(0)
            val actualDuration = runCatching { player.duration }.getOrDefault(0)
            if (actualDuration > 0) durationMs = actualDuration
            kotlinx.coroutines.delay(250)
        }
    }

    DisposableEffect(audioManager, player) {
        val handler = Handler(Looper.getMainLooper())

        fun refreshRoutes() {
            val refreshedRoutes =
                availableVoicemailRoutes(audioManager)

            routeOptions = refreshedRoutes

            val nextRoute =
                selectedVoicemailRoute(
                    routes = refreshedRoutes,
                    isSpeakerEnabled = isSpeakerEnabled
                )

            if (nextRoute != null) {
                runCatching {
                    setPreferredDeviceCompat(player, nextRoute.device)
                }
            }
        }

        val callback =
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(
                    addedDevices: Array<out AudioDeviceInfo>
                ) {
                    refreshRoutes()
                }

                override fun onAudioDevicesRemoved(
                    removedDevices: Array<out AudioDeviceInfo>
                ) {
                    refreshRoutes()
                }
            }

        refreshRoutes()

        audioManager.registerAudioDeviceCallback(
            callback,
            handler
        )

        onDispose {
            runCatching {
                audioManager.unregisterAudioDeviceCallback(callback)
            }

            runCatching {
                setPreferredDeviceCompat(player, null)
            }

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

        Row(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, ChatforiaColors.border, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Keep the same authenticated MediaPlayer callback; only reshape the controls.
            Button(
                modifier = Modifier.size(52.dp)
                    .clip(playButtonShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(ChatforiaColors.buttonStart, ChatforiaColors.buttonEnd)
                        )
                    ),
                contentPadding = PaddingValues(0.dp),
                enabled = !isPreparing,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = ChatforiaColors.buttonForeground,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = ChatforiaColors.buttonForeground.copy(alpha = 0.65f)
                ),
                onClick = {
                    playbackError = null
    
                    when {
                        isPlaying -> {
                            player.pause()
                            isPlaying = false
                        }
    
                        isPrepared -> {
                            selectedRoute?.let {
                                setPreferredDeviceCompat(player, it.device)
                            }
    
                            player.start()
                            isPlaying = true
                        }
    
                        token.isNullOrBlank() -> {
                            playbackError = signInAgainMessage
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
    
                                selectedRoute?.let {
                                    setPreferredDeviceCompat(player, it.device)
                                }
    
                                player.setDataSource(
                                    context,
                                    proxyUrl.toUri(),
                                    mapOf(
                                        "Authorization" to "Bearer $token"
                                    )
                                )
    
                                player.setOnPreparedListener {
                                    isPreparing = false
                                    isPrepared = true
                                    isPlaying = true
    
                                    selectedVoicemailRoute(
                                        routes = routeOptions,
                                        isSpeakerEnabled = isSpeakerEnabled
                                    )?.let { route ->
                                        setPreferredDeviceCompat(it, route.device)
                                    }
    
                                    it.start()
                                }
    
                                player.setOnCompletionListener {
                                    isPlaying = false
                                    positionMs = 0
    
                                    runCatching {
                                        it.seekTo(0)
                                    }
                                }
    
                                player.setOnErrorListener { _, _, _ ->
                                    isPreparing = false
                                    isPrepared = false
                                    isPlaying = false
                                    playbackError = unableToPlayMessage
                                    true
                                }
    
                                player.prepareAsync()
                            }.onFailure {
                                isPreparing = false
                                isPrepared = false
                                isPlaying = false
                                playbackError = unableToLoadMessage
                            }
                        }
                    }
                }
            ) {
                if (isPreparing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.android_voicemail_player_pause
                            else R.string.android_voicemail_inbox_play_voicemail
                        ),
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { voicemailPlaybackFraction(positionMs, durationMs) },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = ChatforiaColors.accent,
                    trackColor = ChatforiaColors.border
                )
                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatVoicemailPlaybackTime(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatforiaColors.secondaryText
                    )
                    Text(
                        formatVoicemailPlaybackTime(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatforiaColors.secondaryText
                    )
                }
            }

            OutlinedButton(
                modifier = Modifier.size(48.dp),
                enabled =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                            routeOptions.isNotEmpty(),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor =
                        if (isSpeakerEnabled) {
                            ChatforiaColors.buttonStart
                        } else {
                            Color.Transparent
                        },
                    contentColor =
                        if (isSpeakerEnabled) {
                            ChatforiaColors.buttonForeground
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                ),
                onClick = {
                    val nextSpeakerEnabled =
                        !isSpeakerEnabled
    
                    val nextRoute =
                        selectedVoicemailRoute(
                            routes = routeOptions,
                            isSpeakerEnabled = nextSpeakerEnabled
                        )
    
                    if (nextRoute == null) {
                        playbackError = audioOutputUnavailableMessage
                        return@OutlinedButton
                    }
    
                    val applied =
                        setPreferredDeviceCompat(
                            player = player,
                            device = nextRoute.device
                        )
    
                    if (applied) {
                        isSpeakerEnabled =
                            nextSpeakerEnabled
                        playbackError = null
                    } else {
                        playbackError = audioOutputUnavailableMessage
                    }
                }
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription =
                        stringResource(
                            if (isSpeakerEnabled) {
                                R.string.android_voicemail_audio_output_earpiece
                            } else {
                                R.string.android_voicemail_audio_output_speaker
                            }
                        ),
                    modifier = Modifier.size(21.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(
                R.string.android_voicemail_transcript
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        val transcriptText =
            when (voicemail.transcriptStatus) {
                VoicemailTranscriptStatus.COMPLETE -> {
                    voicemail.transcript
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: stringResource(
                            R.string.android_voicemail_no_transcript
                        )
                }

                VoicemailTranscriptStatus.PENDING -> {
                    stringResource(
                        R.string.android_voicemail_transcript_pending
                    )
                }

                VoicemailTranscriptStatus.FAILED -> {
                    stringResource(
                        R.string.android_voicemail_transcript_unavailable
                    )
                }
            }

        Text(
            text = transcriptText,
            style = MaterialTheme.typography.bodyMedium,
            color = ChatforiaColors.primaryText,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = ChatforiaColors.border,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        )
    }
}

private fun selectedVoicemailRoute(
    routes: List<VoicemailAudioRouteOption>,
    isSpeakerEnabled: Boolean
): VoicemailAudioRouteOption? {
    if (isSpeakerEnabled) {
        return routes.firstOrNull {
            it.kind == VoicemailAudioRouteKind.Speaker
        } ?: preferredPrivateRoute(routes)
    }

    return preferredPrivateRoute(routes)
}

private fun preferredPrivateRoute(
    routes: List<VoicemailAudioRouteOption>
): VoicemailAudioRouteOption? {
    return routes.firstOrNull {
        it.kind == VoicemailAudioRouteKind.Bluetooth
    } ?: routes.firstOrNull {
        it.kind == VoicemailAudioRouteKind.Headphones
    } ?: routes.firstOrNull {
        it.kind == VoicemailAudioRouteKind.Earpiece
    } ?: routes.firstOrNull {
        it.kind == VoicemailAudioRouteKind.Speaker
    }
}

private fun setPreferredDeviceCompat(
    player: MediaPlayer,
    device: AudioDeviceInfo?
): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        player.setPreferredDevice(device)
    } else {
        false
    }
}

private fun availableVoicemailRoutes(
    audioManager: AudioManager
): List<VoicemailAudioRouteOption> {
    return audioManager
        .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .mapNotNull { device ->
            voicemailRouteKind(device)?.let { kind ->
                VoicemailAudioRouteOption(
                    kind = kind,
                    device = device
                )
            }
        }
        .distinctBy { it.kind }
        .sortedBy {
            when (it.kind) {
                VoicemailAudioRouteKind.Bluetooth -> 0
                VoicemailAudioRouteKind.Headphones -> 1
                VoicemailAudioRouteKind.Earpiece -> 2
                VoicemailAudioRouteKind.Speaker -> 3
            }
        }
}

private fun voicemailRouteKind(
    device: AudioDeviceInfo
): VoicemailAudioRouteKind? {
    return when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ->
            VoicemailAudioRouteKind.Earpiece

        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ->
            VoicemailAudioRouteKind.Speaker

        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_HEARING_AID ->
            VoicemailAudioRouteKind.Bluetooth

        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET ->
            VoicemailAudioRouteKind.Headphones

        else -> {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                )
            ) {
                VoicemailAudioRouteKind.Bluetooth
            } else {
                null
            }
        }
    }
}

internal fun voicemailPlaybackFraction(positionMs: Int, durationMs: Int): Float =
    if (durationMs <= 0) 0f
    else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

internal fun formatVoicemailPlaybackTime(timeMs: Int): String {
    val totalSeconds = timeMs.coerceAtLeast(0) / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
