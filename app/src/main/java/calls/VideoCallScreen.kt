package com.chatforia.android.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun VideoCallScreen(
    session: CallSession,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Video room: ${session.roomName ?: "Connecting..."}",
                color = ChatforiaColors.primaryText
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(width = 110.dp, height = 160.dp),
            color = ChatforiaColors.cardBackground,
            shape = MaterialTheme.shapes.large
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (session.cameraEnabled) "Local video" else "Camera off",
                    color = ChatforiaColors.secondaryText
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FilledIconButton(
                onClick = onToggleMute,
                shape = CircleShape
            ) {
                Icon(
                    if (session.muted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = stringResource(R.string.android_audio_call_mute)
                )
            }

            FilledIconButton(
                onClick = onToggleCamera,
                shape = CircleShape
            ) {
                Icon(
                    if (session.cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = stringResource(R.string.android_video_call_camera)
                )
            }

            FilledIconButton(
                onClick = onFlipCamera,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.android_video_call_flip_camera)
                )
            }

            FilledIconButton(
                onClick = onEndCall,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = stringResource(R.string.android_audio_call_end_call)
                )
            }
        }
    }
}