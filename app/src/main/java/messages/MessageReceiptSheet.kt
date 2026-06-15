package com.chatforia.android.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageReceiptSheet(
    message: MessageDto,
    currentUserId: Int?,
    isGroupRoom: Boolean,
    onDismiss: () -> Unit
) {
    val readers = message.readBy
        .filter { it.id != message.sender.id }
        .filter { it.id != currentUserId }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.android_chat_message_row_message_info),
                style = MaterialTheme.typography.titleLarge
            )

            if (readers.isEmpty()) {
                Text(
                    text = stringResource(R.string.android_message_receipt_no_one_has_read_this_yet),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = if (isGroupRoom) {
                        "Seen by ${readers.size}"
                    } else {
                        "Seen"
                    },
                    style = MaterialTheme.typography.labelLarge
                )

                readers.forEach { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials(user),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Column {
                            Text(
                                text = displayName(user),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.android_message_receipt_message_info),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.android_dial_pad_done))
            }
        }
    }
}

private fun displayName(user: SenderDto): String {
    return user.username?.trim()?.takeIf { it.isNotBlank() }
        ?: "User ${user.id}"
}

private fun initials(user: SenderDto): String {
    val name = displayName(user)

    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { name.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }
}