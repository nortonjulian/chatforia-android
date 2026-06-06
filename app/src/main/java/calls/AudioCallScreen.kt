package com.chatforia.android.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun AudioCallScreen(
    session: CallSession,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = session.displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Audio call",
            style = MaterialTheme.typography.bodyLarge,
            color = ChatforiaColors.secondaryText
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            FilledIconButton(
                onClick = onToggleMute,
                shape = CircleShape
            ) {
                Icon(
                    if (session.muted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute"
                )
            }

            FilledIconButton(
                onClick = onToggleSpeaker,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Speaker"
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
                    contentDescription = "End call"
                )
            }
        }
    }
}