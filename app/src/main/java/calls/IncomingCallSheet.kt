package com.chatforia.android.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatforia.android.R

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

    val background =
        Brush.verticalGradient(
            colors =
                listOf(
                    Color(0xFF2C2418),
                    Color(0xFF171717),
                    Color(0xFF090909)
                )
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background)
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    if (isVideo) {
                        "Chatforia Video Call"
                    } else {
                        "Chatforia Call"
                    },
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFC247),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(18.dp))

            Text(
                text = callerName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallAction(
                label = stringResource(R.string.android_incoming_call_decline),
                containerColor = Color(0xFFE53935),
                icon = {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription =
                            stringResource(
                                R.string.android_incoming_call_decline_call
                            ),
                        tint = Color.White
                    )
                },
                onClick = onDecline
            )

            CallAction(
                label = stringResource(R.string.android_incoming_call_accept),
                containerColor = Color(0xFF43A047),
                icon = {
                    Icon(
                        imageVector =
                            if (isVideo) {
                                Icons.Default.Videocam
                            } else {
                                Icons.Default.Call
                            },
                        contentDescription =
                            stringResource(
                                R.string.android_incoming_call_accept_call
                            ),
                        tint = Color.White
                    )
                },
                onClick = onAccept
            )
        }
    }
}

@Composable
private fun CallAction(
    label: String,
    containerColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = containerColor
                )
        ) {
            icon()
        }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}
