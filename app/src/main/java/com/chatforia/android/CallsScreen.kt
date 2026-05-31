package com.chatforia.android

import com.chatforia.android.ui.components.ChatforiaAction
import com.chatforia.android.ui.components.ChatforiaActionPill
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.foundation.background

enum class CallsSegment {
    Recents,
    Voicemail
}

data class CallPreview(
    val name: String,
    val direction: String,
    val status: String,
    val duration: String?,
    val timestamp: String,
    val isMissed: Boolean = false,
    val isOutgoing: Boolean = false,
    val canVideo: Boolean = true,
    val canPhone: Boolean = true
)

@Composable
fun CallsScreen() {
    var selectedSegment by remember { mutableStateOf(CallsSegment.Recents) }

    val calls = listOf(
        CallPreview(
            name = "Ria",
            direction = "Incoming",
            status = "Completed",
            duration = "4:23",
            timestamp = "May 30, 2:30 PM"
        ),
        CallPreview(
            name = "bob",
            direction = "Outgoing",
            status = "Completed",
            duration = "1:02",
            timestamp = "May 29, 9:14 PM",
            isOutgoing = true
        ),
        CallPreview(
            name = "Random Chat",
            direction = "Incoming",
            status = "Missed",
            duration = null,
            timestamp = "May 28, 7:45 PM",
            isMissed = true
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calls",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            ChatforiaActionPill(
                actions = listOf(
                    ChatforiaAction(
                        icon = Icons.Default.Dialpad,
                        contentDescription = "Open dial pad"
                    )
                )
            )
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedSegment == CallsSegment.Recents,
                onClick = { selectedSegment = CallsSegment.Recents },

                shape = SegmentedButtonDefaults.itemShape(
                    index = 0,
                    count = 2
                ),

                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ChatforiaColors.highlightedSurface,
                    activeContentColor = ChatforiaColors.primaryText,

                    inactiveContainerColor = ChatforiaColors.cardBackground,
                    inactiveContentColor = ChatforiaColors.secondaryText
                ),

                label = { Text("Recents") }
            )

            SegmentedButton(
                selected = selectedSegment == CallsSegment.Voicemail,

                onClick = {
                    selectedSegment = CallsSegment.Voicemail
                },

                shape = SegmentedButtonDefaults.itemShape(
                    index = 1,
                    count = 2
                ),

                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ChatforiaColors.highlightedSurface,
                    activeContentColor = ChatforiaColors.primaryText,

                    inactiveContainerColor = ChatforiaColors.cardBackground,
                    inactiveContentColor = ChatforiaColors.secondaryText
                ),

                label = {
                    Text("Voicemail")
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedSegment) {
            CallsSegment.Recents -> {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = ChatforiaColors.cardBackground,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        calls.forEachIndexed { index, call ->
                            CallPreviewRow(call)

                            if (index != calls.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            CallsSegment.Voicemail -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No voicemails yet",
                        color = ChatforiaColors.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun CallPreviewRow(call: CallPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        val icon = when {
            call.isMissed -> Icons.Default.PhoneMissed
            call.isOutgoing -> Icons.Default.PhoneForwarded
            else -> Icons.Default.PhoneCallback
        }

        val statusColor =
            if (call.isMissed)
                MaterialTheme.colorScheme.error
            else
                ChatforiaColors.secondaryText

        Surface(
            modifier = Modifier.size(42.dp),
            shape = MaterialTheme.shapes.large,
            color = ChatforiaColors.highlightedSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = call.status,
                    tint = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                call.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (call.isMissed) FontWeight.Bold else FontWeight.SemiBold,
                color =
                    if (call.isMissed)
                        MaterialTheme.colorScheme.error
                    else
                        ChatforiaColors.primaryText
            )

            Row {
                Text(
                    "${call.direction} • ${call.status}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )

                if (call.duration != null) {
                    Text(
                        " • ${call.duration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }
            }

            Text(
                call.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = ChatforiaColors.secondaryText
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (call.canVideo) {
                FilledIconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ChatforiaColors.highlightedSurface,
                        contentColor = ChatforiaColors.accent
                    )
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video call")
                }
            }

            if (call.canPhone) {
                FilledIconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ChatforiaColors.highlightedSurface,
                        contentColor = ChatforiaColors.accent
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call")
                }
            }
        }
    }
}