package com.chatforia.android.ria

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun RiaChatScreen(
    viewModel: RiaChatViewModel,
    memoryEnabled: Boolean,
    filterProfanity: Boolean,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
    ) {
        RiaHeader(onClose = onClose)

        if (!state.aiDisabledReason.isNullOrBlank()) {
            RiaBanner(
                text = state.aiDisabledReason.orEmpty(),
                isError = false
            )
        } else if (!state.error.isNullOrBlank()) {
            RiaBanner(
                text = state.error.orEmpty(),
                isError = true
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You’re now chatting with Ria",
                        color = ChatforiaColors.secondaryText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .background(
                                ChatforiaColors.cardBackground,
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            items(state.messages) { message ->
                RiaMessageBubble(message)
            }

            if (state.isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = ChatforiaColors.accent
                        )
                    }
                }
            }
        }

        RiaComposer(
            draft = draft,
            isLoading = state.isLoading,
            onDraftChange = { draft = it },
            onSend = {
                val text = draft
                draft = ""
                viewModel.sendMessage(
                    text = text,
                    memoryEnabled = memoryEnabled,
                    filterProfanity = filterProfanity
                )
            }
        )
    }
}

@Composable
private fun RiaHeader(
    onClose: () -> Unit
) {
    Surface(
        color = ChatforiaColors.cardBackground,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = ChatforiaColors.highlightedSurface,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ChatforiaColors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ria",
                        color = ChatforiaColors.primaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "AI",
                        color = ChatforiaColors.accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                ChatforiaColors.highlightedSurface,
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Chat with Ria anytime, separate from random human matching.",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ChatforiaColors.secondaryText
                )
            }
        }
    }
}

@Composable
private fun RiaBanner(
    text: String,
    isError: Boolean
) {
    Text(
        text = text,
        color =
            if (isError)
                MaterialTheme.colorScheme.error
            else
                ChatforiaColors.secondaryText,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .background(
                ChatforiaColors.cardBackground,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}

@Composable
private fun RiaMessageBubble(
    message: RiaChatMessage
) {
    val isAssistant = message.role == "assistant"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isAssistant) Arrangement.Start else Arrangement.End
    ) {
        Text(
            text = message.content,
            color =
                if (isAssistant)
                    ChatforiaColors.primaryText
                else
                    ChatforiaColors.primaryText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (isAssistant)
                        ChatforiaColors.cardBackground
                    else
                        ChatforiaColors.accent,
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun RiaComposer(
    draft: String,
    isLoading: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = ChatforiaColors.cardBackground,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Ask Ria...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                minLines = 1,
                maxLines = 5
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = !isLoading && draft.trim().isNotEmpty(),
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        ChatforiaColors.accent,
                        CircleShape
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ChatforiaColors.primaryText
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = ChatforiaColors.primaryText
                    )
                }
            }
        }
    }
}