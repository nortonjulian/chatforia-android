package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
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
import com.chatforia.android.calls.CallDto
import com.chatforia.android.calls.CallsSegment
import com.chatforia.android.calls.CallsViewModel
import com.chatforia.android.ui.theme.ChatforiaColors
import com.chatforia.android.voicemail.VoicemailInboxScreen
import com.chatforia.android.voicemail.VoicemailViewModel

@Composable
fun CallsScreen(
    callsViewModel: CallsViewModel,
    voicemailViewModel: VoicemailViewModel,
    onStartAudioCall: (CallDto) -> Unit = {},
    onStartVideoCall: (CallDto) -> Unit = {}
) {
    var selectedSegment by remember {
        mutableStateOf(CallsSegment.Recents)
    }

    val callsState by callsViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        callsViewModel.loadCalls()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Calls",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText,
            modifier = Modifier.padding(top = 20.dp, bottom = 18.dp)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedSegment == CallsSegment.Recents,
                onClick = { selectedSegment = CallsSegment.Recents },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                label = { Text("Recents") }
            )

            SegmentedButton(
                selected = selectedSegment == CallsSegment.Voicemail,
                onClick = { selectedSegment = CallsSegment.Voicemail },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                label = { Text("Voicemail") }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedSegment) {
            CallsSegment.Recents -> {
                if (callsState.isLoading) {
                    CircularProgressIndicator()
                }

                if (callsState.error != null) {
                    Text(
                        callsState.error ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = ChatforiaColors.cardBackground,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (callsState.calls.isEmpty() && !callsState.isLoading) {
                        Text(
                            "No calls yet",
                            modifier = Modifier.padding(20.dp),
                            color = ChatforiaColors.secondaryText
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            items(callsState.calls) { call ->
                                CallHistoryRow(
                                    call = call,
                                    onStartAudioCall = {
                                        onStartAudioCall(call)
                                    },
                                    onStartVideoCall = {
                                        onStartVideoCall(call)
                                    }
                                )

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            CallsSegment.Voicemail -> {
                VoicemailInboxScreen(
                    viewModel = voicemailViewModel
                )
            }
        }
    }
}

@Composable
private fun CallHistoryRow(
    call: CallDto,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit
) {
    val status = call.status ?: ""
    val isMissed = status.uppercase() == "MISSED"
    val isOutgoing = call.direction?.uppercase() == "OUTGOING"

    val icon =
        when {
            isMissed -> Icons.Default.PhoneMissed
            isOutgoing -> Icons.Default.PhoneForwarded
            else -> Icons.Default.PhoneCallback
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = MaterialTheme.shapes.large,
            color = ChatforiaColors.highlightedSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = status,
                    tint =
                        if (isMissed)
                            MaterialTheme.colorScheme.error
                        else
                            ChatforiaColors.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                call.displayName
                    ?: call.phoneNumber
                    ?: "Unknown",
                fontWeight =
                    if (isMissed)
                        FontWeight.Bold
                    else
                        FontWeight.SemiBold,
                color =
                    if (isMissed)
                        MaterialTheme.colorScheme.error
                    else
                        ChatforiaColors.primaryText
            )

            Text(
                listOfNotNull(
                    call.direction,
                    call.status,
                    call.durationSec?.let { "${it}s" }
                ).joinToString(" • "),
                color = ChatforiaColors.secondaryText
            )

            Text(
                call.createdAt ?: call.startedAt ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = ChatforiaColors.secondaryText
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledIconButton(
                onClick = onStartVideoCall,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ChatforiaColors.highlightedSurface,
                    contentColor = ChatforiaColors.accent
                )
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "Video call")
            }

            FilledIconButton(
                onClick = onStartAudioCall,
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