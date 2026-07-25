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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chatforia.android.R
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.network.Environment
import com.chatforia.android.ui.theme.ChatforiaColors

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

    var playbackError by remember(voicemail.id) {
        mutableStateOf<String?>(null)
    }

    var routeOptions by remember(voicemail.id) {
        mutableStateOf<List<VoicemailAudioRouteOption>>(emptyList())
    }

    var selectedRouteDeviceId by remember(voicemail.id) {
        mutableStateOf<Int?>(null)
    }

    var routeMenuExpanded by remember(voicemail.id) {
        mutableStateOf(false)
    }

    val selectedRoute =
        routeOptions.firstOrNull {
            it.device.id == selectedRouteDeviceId
        } ?: preferredDefaultRoute(routeOptions)

    val playButtonShape = RoundedCornerShape(28.dp)

    DisposableEffect(audioManager, player) {
        val handler = Handler(Looper.getMainLooper())

        fun refreshRoutes() {
            val refreshedRoutes =
                availableVoicemailRoutes(audioManager)

            routeOptions = refreshedRoutes

            val retainedRoute =
                refreshedRoutes.firstOrNull {
                    it.device.id == selectedRouteDeviceId
                }

            val nextRoute =
                retainedRoute ?: preferredDefaultRoute(refreshedRoutes)

            selectedRouteDeviceId = nextRoute?.device?.id

            if (nextRoute != null) {
                runCatching {
                    player.setPreferredDevice(nextRoute.device)
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
                player.setPreferredDevice(null)
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

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
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
                            selectedRoute?.let {
                                player.setPreferredDevice(it.device)
                            }

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

                                selectedRoute?.let {
                                    player.setPreferredDevice(it.device)
                                }

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

                                    selectedRoute?.let { route ->
                                        it.setPreferredDevice(route.device)
                                    }

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
                            stringResource(
                                R.string.android_voicemail_player_pause
                            )
                        } else {
                            stringResource(
                                R.string.android_voicemail_inbox_play_voicemail
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                val selectedLabel =
                    selectedRoute?.let {
                        voicemailRouteLabel(it.kind)
                    } ?: stringResource(
                        R.string.android_voicemail_audio_output
                    )

                OutlinedButton(
                    enabled = routeOptions.isNotEmpty(),
                    shape = playButtonShape,
                    onClick = {
                        routeMenuExpanded = true
                    }
                ) {
                    Icon(
                        imageVector =
                            selectedRoute?.kind?.let {
                                voicemailRouteIcon(it)
                            } ?: Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = selectedLabel,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = selectedLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                DropdownMenu(
                    expanded = routeMenuExpanded,
                    onDismissRequest = {
                        routeMenuExpanded = false
                    }
                ) {
                    routeOptions.forEach { route ->
                        val label =
                            voicemailRouteLabel(route.kind)

                        DropdownMenuItem(
                            text = {
                                Text(label)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        voicemailRouteIcon(route.kind),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                routeMenuExpanded = false

                                val applied =
                                    player.setPreferredDevice(
                                        route.device
                                    )

                                if (applied) {
                                    selectedRouteDeviceId =
                                        route.device.id
                                    playbackError = null
                                } else {
                                    playbackError =
                                        context.getString(
                                            R.string.android_voicemail_audio_output_unavailable
                                        )
                                }
                            }
                        )
                    }
                }
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

@Composable
private fun voicemailRouteLabel(
    kind: VoicemailAudioRouteKind
): String {
    return when (kind) {
        VoicemailAudioRouteKind.Earpiece ->
            stringResource(
                R.string.android_voicemail_audio_output_earpiece
            )

        VoicemailAudioRouteKind.Speaker ->
            stringResource(
                R.string.android_voicemail_audio_output_speaker
            )

        VoicemailAudioRouteKind.Bluetooth ->
            stringResource(
                R.string.android_voicemail_audio_output_bluetooth
            )

        VoicemailAudioRouteKind.Headphones ->
            stringResource(
                R.string.android_voicemail_audio_output_headphones
            )
    }
}

private fun voicemailRouteIcon(
    kind: VoicemailAudioRouteKind
): ImageVector {
    return when (kind) {
        VoicemailAudioRouteKind.Earpiece ->
            Icons.Filled.PhoneInTalk

        VoicemailAudioRouteKind.Speaker ->
            Icons.AutoMirrored.Filled.VolumeUp

        VoicemailAudioRouteKind.Bluetooth ->
            Icons.Filled.BluetoothAudio

        VoicemailAudioRouteKind.Headphones ->
            Icons.Filled.Headphones
    }
}

private fun preferredDefaultRoute(
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
