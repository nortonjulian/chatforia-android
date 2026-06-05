package com.chatforia.android.messages

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatMessageRow(
    message: MessageDto,
    isMine: Boolean
) {
    val attachments =
        if (message.attachments.isNotEmpty()) {
            message.attachments
        } else {
            message.attachmentsInline
        }

    val hasAttachments = attachments.isNotEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (hasAttachments) {
                MessageAttachmentsView(
                    attachments = attachments,
                    isMine = isMine
                )
            }

            if (shouldShowTextBubble(message, hasAttachments)) {
                MessageBubble(
                    message = message,
                    isMine = isMine
                )
            }
        }
    }
}

private fun shouldShowTextBubble(
    message: MessageDto,
    hasAttachments: Boolean
): Boolean {
    if (message.deletedForAll == true || message.deletedBySender == true) return true

    val text =
        message.decryptedContent
            ?: message.translatedForMe
            ?: message.rawContent
            ?: message.content
            ?: ""

    val normalized = text.trim().lowercase()

    val placeholderTexts = setOf(
        "[image]",
        "[video]",
        "[audio]",
        "[file]",
        "[attachment]",
        "[gif]",
        "attachment",
        "media attachment"
    )

    if (normalized.isBlank()) return false
    if (placeholderTexts.contains(normalized)) return false

    return true
}