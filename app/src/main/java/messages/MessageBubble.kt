package com.chatforia.android.messages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chatforia.android.R
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
        bottomStart = if (isMine) 18.dp else 6.dp,
        bottomEnd = if (isMine) 6.dp else 18.dp
    )

    val bubbleBrush =
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

    val outgoingTextColor = ChatforiaColors.outgoingBubbleText
    val outgoingMetaTextColor = ChatforiaColors.outgoingBubbleText.copy(alpha = 0.75f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .align(if (isMine) Alignment.BottomEnd else Alignment.BottomStart)
                    .offset {
                        IntOffset(
                            x = if (isMine) 5.dp.roundToPx() else (-5).dp.roundToPx(),
                            y = (-1).dp.roundToPx()
                        )
                    }
                    .size(12.dp)
            ) {
                val path = Path()

                if (isMine) {
                    path.moveTo(0f, 0f)
                    path.lineTo(size.width, size.height)
                    path.lineTo(0f, size.height)
                } else {
                    path.moveTo(size.width, 0f)
                    path.lineTo(0f, size.height)
                    path.lineTo(size.width, size.height)
                }

                path.close()
                drawPath(path = path, brush = bubbleBrush)
            }

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleBrush)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
}