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
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.MoreHoriz
import com.chatforia.android.contacts.ContactDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCallScreen(
    session: CallSession,
    statusText: String = "Calling…",
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSendDigit: (String) -> Unit,
    onEndCall: () -> Unit,
    onAddParticipant: (ContactDto) -> Unit,
    contacts: List<ContactDto> = emptyList(),
) {
    var showKeypad by remember { mutableStateOf(false) }

    var showCallOptions by remember { mutableStateOf(false) }

    var showAddParticipant by remember { mutableStateOf(false) }

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
            text = statusText,
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
                    contentDescription = stringResource(R.string.android_audio_call_mute)
                )
            }

            FilledIconButton(
                onClick = onToggleSpeaker,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = stringResource(R.string.android_audio_call_speaker)
                )
            }

            FilledIconButton(
                onClick = { showCallOptions = true },
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "More"
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

    if (showKeypad) {
        ModalBottomSheet(
            onDismissRequest = { showKeypad = false }
        ) {
            InCallKeypadSheet(
                onDigit = onSendDigit,
                onDone = { showKeypad = false }
            )
        }
    }

    if (showCallOptions) {
        ModalBottomSheet(
            onDismissRequest = { showCallOptions = false }
        ) {
            InCallOptionsSheet(
                onAddParticipant = {
                    showCallOptions = false
                    showAddParticipant = true
                },
                onKeypad = {
                    showCallOptions = false
                    showKeypad = true
                }
            )
        }
    }

    if (showAddParticipant) {
        ModalBottomSheet(
            onDismissRequest = { showAddParticipant = false }
        ) {
            InCallAddParticipantSheet(
                contacts = contacts,
                onPick = { contact ->
                    onAddParticipant(contact)
                    showAddParticipant = false
                }
            )
        }
    }

}

@Composable
private fun InCallOptionsSheet(
    onAddParticipant: () -> Unit,
    onKeypad: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatforiaColors.screenBackground)
            .padding(22.dp)
    ) {
        Text(
            text = "Call Options",
            style = MaterialTheme.typography.titleLarge,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onAddParticipant) {
            Text("Add Participant")
        }

        TextButton(onClick = onKeypad) {
            Text("Keypad")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun InCallAddParticipantSheet(
    contacts: List<ContactDto>,
    onPick: (ContactDto) -> Unit
) {
    val appContacts =
        contacts.filter { contact ->
            contact.user?.id != null || contact.userId != null
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatforiaColors.screenBackground)
            .padding(22.dp)
    ) {
        Text(
            text = "Add Participant",
            style = MaterialTheme.typography.titleLarge,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(16.dp))

        appContacts.forEach { contact ->
            TextButton(
                onClick = { onPick(contact) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        contact.alias
                            ?: contact.user?.username
                            ?: contact.externalName
                            ?: contact.externalPhone
                            ?: "Unknown Contact"
                )
            }
        }

        if (appContacts.isEmpty()) {
            Text(
                text = "No Chatforia contacts available to add.",
                color = ChatforiaColors.secondaryText
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun InCallKeypadSheet(
    onDigit: (String) -> Unit,
    onDone: () -> Unit
) {
    var digits by remember { mutableStateOf("") }

    val rows =
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatforiaColors.screenBackground)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Keypad",
                style = MaterialTheme.typography.titleLarge,
                color = ChatforiaColors.primaryText
            )

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onDone) {
                Text("Done")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = digits.ifEmpty { "Enter digits" },
            style = MaterialTheme.typography.headlineSmall,
            color =
                if (digits.isEmpty()) {
                    ChatforiaColors.secondaryText
                } else {
                    ChatforiaColors.primaryText
                }
        )

        Spacer(modifier = Modifier.height(18.dp))

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { digit ->
                    OutlinedButton(
                        onClick = {
                            digits += digit
                            onDigit(digit)
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = digit,
                                style = MaterialTheme.typography.headlineSmall,
                                color = ChatforiaColors.primaryText
                            )

                            Text(
                                text = lettersForDigit(digit),
                                style = MaterialTheme.typography.labelSmall,
                                color = ChatforiaColors.secondaryText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun lettersForDigit(digit: String): String {
    return when (digit) {
        "2" -> "ABC"
        "3" -> "DEF"
        "4" -> "GHI"
        "5" -> "JKL"
        "6" -> "MNO"
        "7" -> "PQRS"
        "8" -> "TUV"
        "9" -> "WXYZ"
        else -> ""
    }
}