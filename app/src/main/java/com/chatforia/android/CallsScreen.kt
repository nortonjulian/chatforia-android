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
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.IconButton
import com.chatforia.android.calls.DialPadSheet
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import kotlin.math.roundToInt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    callsViewModel: CallsViewModel,
    voicemailViewModel: VoicemailViewModel,
    onStartAudioCall: (CallDto) -> Unit = {},
    onStartVideoCall: (CallDto) -> Unit = {},
    onDialNumber: (String) -> Unit = {}
) {
    var selectedSegment by remember {
        mutableStateOf(CallsSegment.Recents)
    }

    var showDialer by remember {
        mutableStateOf(false)
    }

    val callsState by callsViewModel.state.collectAsState()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(Unit) {
        callsViewModel.loadCalls()
    }

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

            IconButton(
                onClick = {
                    showDialer = true
                }
            ) {
                Icon(
                    Icons.Default.Dialpad,
                    contentDescription = "Open dial pad",
                    tint = ChatforiaColors.accent
                )
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedSegment == CallsSegment.Recents,
                onClick = { selectedSegment = CallsSegment.Recents },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ChatforiaColors.highlightedSurface,
                    activeContentColor = ChatforiaColors.primaryText,
                    inactiveContainerColor = ChatforiaColors.cardBackground,
                    inactiveContentColor = ChatforiaColors.primaryText
                ),
                label = { Text("Recents") }
            )

            SegmentedButton(
                selected = selectedSegment == CallsSegment.Voicemail,
                onClick = { selectedSegment = CallsSegment.Voicemail },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ChatforiaColors.highlightedSurface,
                    activeContentColor = ChatforiaColors.primaryText,
                    inactiveContainerColor = ChatforiaColors.cardBackground,
                    inactiveContentColor = ChatforiaColors.primaryText
                ),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (callsState.calls.isEmpty() && !callsState.isLoading) {
                        EmptyCallsState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            items(
                                items = callsState.calls,
                                key = { it.id }
                            ) { call ->
                                SwipeRevealCallRow(
                                    call = call,
                                    onStartAudioCall = {
                                        onStartAudioCall(call)
                                    },
                                    onStartVideoCall = {
                                        onStartVideoCall(call)
                                    },
                                    onDelete = {
                                        callsViewModel.deleteCall(call)
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

    if (showDialer) {
        ModalBottomSheet(
            onDismissRequest = {
                showDialer = false
            },
            sheetState = sheetState,
            containerColor = ChatforiaColors.screenBackground
        ) {
            DialPadSheet(
                onDismiss = {
                    showDialer = false
                },
                onCall = { number ->
                    showDialer = false
                    onDialNumber(number)
                }
            )
        }
    }
}

@Composable
private fun SwipeRevealCallRow(
    call: CallDto,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember(call.id) {
        mutableFloatStateOf(0f)
    }

    val maxRevealPx = 92.dp.value * LocalDensity.current.density

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(92.dp)
                .background(Color(0xFFE53935)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete call",
                    tint = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(ChatforiaColors.cardBackground)
                .pointerInput(call.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount)
                                .coerceIn(-maxRevealPx, 0f)
                        },
                        onDragEnd = {
                            offsetX = if (offsetX < -maxRevealPx / 2) {
                                -maxRevealPx
                            } else {
                                0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        }
                    )
                }
        ) {
            CallHistoryRow(
                call = call,
                onStartAudioCall = onStartAudioCall,
                onStartVideoCall = onStartVideoCall
            )
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
            color =
                if (ChatforiaColors.screenBackground.luminance() > 0.5f)
                    Color(0xFFFFF1C9)
                else
                    Color(0xFF123A4A)
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
                    ?: call.externalPhone
                    ?: call.toLabel
                    ?: call.fromLabel
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

            val friendlyStatus =
                when (call.status?.uppercase()) {
                    "INITIATED" -> "Calling…"
                    "RINGING" -> "Ringing"
                    "ACTIVE" -> "Connected"
                    "ENDED" -> "Completed"
                    "MISSED" -> "Missed"
                    "DECLINED" -> "Declined"
                    "FAILED" -> "Failed"
                    else -> call.status
                }

            Text(
                listOfNotNull(
                    call.direction?.replaceFirstChar {
                        it.uppercase()
                    },
                    friendlyStatus,
                    call.durationSec?.let { "${it}s" }
                ).joinToString(" • "),
                color = ChatforiaColors.secondaryText
            )

            Text(
                friendlyCallTime(call.createdAt ?: call.startedAt),
                style = MaterialTheme.typography.bodySmall,
                color = ChatforiaColors.secondaryText
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledIconButton(
                onClick = onStartVideoCall,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor =
                        if (ChatforiaColors.screenBackground.luminance() > 0.5f)
                            Color(0xFFFFF1C9)
                        else
                            Color(0xFF123A4A),
                    contentColor = ChatforiaColors.accent
                )
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "Video call")
            }

            FilledIconButton(
                onClick = onStartAudioCall,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor =
                        if (ChatforiaColors.screenBackground.luminance() > 0.5f)
                            Color(0xFFFFF1C9)
                        else
                            Color(0xFF123A4A),
                    contentColor = ChatforiaColors.accent
                )
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call")
            }
        }
    }
}

@Composable
private fun EmptyCallsState() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Phone,
            contentDescription = null,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No calls yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your recent calls will show up here.",
            style = MaterialTheme.typography.bodyLarge,
            color = ChatforiaColors.secondaryText
        )
    }
}

private fun friendlyCallTime(value: String?): String {
    if (value.isNullOrBlank()) return ""

    return try {
        val instant = java.time.Instant.parse(value)
        val zone = java.time.ZoneId.systemDefault()
        val dateTime = instant.atZone(zone)
        val now = java.time.ZonedDateTime.now(zone)

        val time =
            dateTime.format(
                java.time.format.DateTimeFormatter.ofPattern("h:mm a")
            )

        when {
            dateTime.toLocalDate() == now.toLocalDate() ->
                "Today, $time"

            dateTime.toLocalDate() == now.minusDays(1).toLocalDate() ->
                "Yesterday, $time"

            else ->
                dateTime.format(
                    java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")
                )
        }
    } catch (_: Exception) {
        value
    }
}