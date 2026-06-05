package com.chatforia.android.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.messages.MessageDto
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun MessageBubble(
    message: MessageDto,
    isMine: Boolean
) {
    val isDeleted = message.deletedForAll == true

    val displayText =
        when {
            isDeleted -> "This message was deleted"

            !message.decryptedContent.isNullOrBlank() ->
                message.decryptedContent

            !message.translatedForMe.isNullOrBlank() ->
                message.translatedForMe

            !message.rawContent.isNullOrBlank() ->
                message.rawContent

            !message.content.isNullOrBlank() ->
                message.content

            message.attachments.isNotEmpty() ->
                ""

            message.attachmentsInline.isNotEmpty() ->
                ""

            !message.contentCiphertext.isNullOrBlank() ->
                "Unable to decrypt this older message."

            else -> ""
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color =
                if (isMine) {
                    ChatforiaColors.accent
                } else {
                    ChatforiaColors.cardBackground
                },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = displayText,
                    color =
                        if (isMine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            ChatforiaColors.primaryText
                        }
                )

                if (message.editedAt != null && !isDeleted) {
                    Text(
                        text = "Edited",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isMine) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            } else {
                                ChatforiaColors.secondaryText
                            }
                    )
                }

                if (message.optimistic) {
                    Text(
                        text = "Sending…",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isMine) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            } else {
                                ChatforiaColors.secondaryText
                            }
                    )
                }

                if (message.failed) {
                    Text(
                        text = "Failed to send",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (message.expiresAt != null && !isDeleted) {
                    Text(
                        text = "Disappearing message",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isMine) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            } else {
                                ChatforiaColors.secondaryText
                            }
                    )
                }
            }
        }
    }
}