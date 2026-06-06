package com.chatforia.android.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun IncomingCallSheet(
    payload: IncomingCallPayload,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isVideo =
        payload.mode?.uppercase() == "VIDEO" ||
                !payload.roomName.isNullOrBlank()

    val callerName =
        payload.callerName
            ?: payload.fromNumber
            ?: "Incoming call"

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {},
        title = {
            Text(
                text = if (isVideo) "Incoming video call" else "Incoming call",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = ChatforiaColors.accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.titleLarge,
                    color = ChatforiaColors.primaryText
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onDecline,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "Decline call"
                            )
                        }

                        Text(
                            text = "Decline",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onAccept,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ChatforiaColors.accent
                            )
                        ) {
                            Icon(
                                if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Accept call"
                            )
                        }

                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        modifier = Modifier.padding(12.dp)
    )
}