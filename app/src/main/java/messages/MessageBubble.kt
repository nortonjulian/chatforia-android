package com.chatforia.android.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun MessageBubble(
    message: MessageDto,
    isMine: Boolean
) {
    val isDeleted = message.deletedForAll == true

    val displayText =
        when {
            isDeleted -> "This message was deleted"

            isMine -> {
                listOf(
                    message.rawContent,
                    message.content,
                    message.decryptedContent
                ).firstOrNull { !it.isNullOrBlank() }
                    ?: when {
                        message.attachments.isNotEmpty() -> ""
                        message.attachmentsInline.isNotEmpty() -> ""
                        !message.contentCiphertext.isNullOrBlank() -> "Unable to decrypt this older message."
                        else -> ""
                    }
            }

            else -> {
                when {
                    !message.translatedForMe.isNullOrBlank() -> message.translatedForMe
                    !message.decryptedContent.isNullOrBlank() -> message.decryptedContent
                    !message.rawContent.isNullOrBlank() -> message.rawContent
                    !message.content.isNullOrBlank() -> message.content
                    message.attachments.isNotEmpty() -> ""
                    message.attachmentsInline.isNotEmpty() -> ""
                    !message.contentCiphertext.isNullOrBlank() -> "Unable to decrypt this older message."
                    else -> ""
                }
            }
        }

    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 18.dp
    )

    val outgoingTextColor = ChatforiaColors.outgoingBubbleText
    val outgoingMetaTextColor = ChatforiaColors.outgoingBubbleText.copy(alpha = 0.75f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(
                    if (isMine) {
                        Brush.horizontalGradient(
                            listOf(
                                ChatforiaColors.outgoingBubbleStart,
                                ChatforiaColors.outgoingBubbleEnd
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                ChatforiaColors.cardBackground,
                                ChatforiaColors.cardBackground
                            )
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = displayText,
                    color = if (isMine) outgoingTextColor else ChatforiaColors.primaryText
                )

                if (message.editedAt != null && !isDeleted) {
                    Text(
                        text = stringResource(R.string.android_chat_thread_edited),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) outgoingMetaTextColor else ChatforiaColors.secondaryText
                    )
                }

                if (message.optimistic) {
                    Text(
                        text = stringResource(R.string.android_chat_thread_sending),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) outgoingMetaTextColor else ChatforiaColors.secondaryText
                    )
                }

                if (message.failed) {
                    Text(
                        text = stringResource(R.string.android_chat_thread_failed_to_send),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (message.expiresAt != null && !isDeleted) {
                    Text(
                        text = stringResource(R.string.android_message_bubble_disappearing_message),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) outgoingMetaTextColor else ChatforiaColors.secondaryText
                    )
                }
            }
        }
    }
}