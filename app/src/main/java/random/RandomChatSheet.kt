package com.chatforia.android.random

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

data class RandomChatMessage(
    val id: String,
    val text: String,
    val isMine: Boolean
)

@Composable
fun RandomChatSheet(
    session: RandomSession,
    messages: List<RandomChatMessage>,
    onSend: (String) -> Unit,
    onAddFriend: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Surface(
        color = ChatforiaColors.screenBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 520.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = session.partnerAlias,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = if (session.isFriendUnlocked) {
                    "Friend unlocked"
                } else {
                    "Anonymous random chat"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAddFriend,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.android_random_chat_add_friend))
                }

                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.android_random_chat_skip))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            if (message.isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (message.isMine) {
                                ChatforiaColors.accent
                            } else {
                                ChatforiaColors.cardBackground
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message.text,
                                color = if (message.isMine) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    ChatforiaColors.primaryText
                                },
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.android_contact_detail_message)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    enabled = draft.trim().isNotEmpty(),
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            onSend(text)
                            draft = ""
                        }
                    }
                ) {
                    Text(stringResource(R.string.android_random_chat_send))
                }
            }

            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.android_random_chat_end_chat))
            }
        }
    }
}