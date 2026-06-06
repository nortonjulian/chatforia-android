package com.chatforia.android.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageRow(
    message: MessageDto,
    isMine: Boolean,
    onEdit: ((MessageDto) -> Unit)? = null,
    onDelete: ((MessageDto) -> Unit)? = null,
    onReport: ((MessageDto) -> Unit)? = null,
    onMessageInfo: ((MessageDto) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    val attachments =
        if (message.attachments.isNotEmpty()) {
            message.attachments
        } else {
            message.attachmentsInline
        }

    val hasAttachments = attachments.isNotEmpty()
    val copyText = message.visibleTextOrNull()

    val canEdit =
        isMine &&
                message.deletedForAll != true &&
                message.failed != true &&
                message.isWithinActionWindow()

    val canDeleteForEveryone =
        isMine &&
                message.deletedForAll != true &&
                message.isWithinActionWindow()

    val canDeleteForMe =
        message.deletedForAll != true

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            Column(
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            showMenu = true
                        }
                    )
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

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.align(if (isMine) Alignment.TopEnd else Alignment.TopStart)
        ) {
            if (!copyText.isNullOrBlank()) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        clipboard.setText(AnnotatedString(copyText))
                        showMenu = false
                    }
                )
            }

            if (canEdit) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEdit?.invoke(message)
                    }
                )
            }

            if (canDeleteForMe || canDeleteForEveryone) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete?.invoke(message)
                    }
                )
            }

            if (!isMine && message.deletedForAll != true) {
                DropdownMenuItem(
                    text = { Text("Report") },
                    onClick = {
                        showMenu = false
                        onReport?.invoke(message)
                    }
                )
            }

            if (isMine && message.deletedForAll != true) {
                DropdownMenuItem(
                    text = { Text("Message Info") },
                    onClick = {
                        showMenu = false
                        onMessageInfo?.invoke(message)
                    }
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

    val text = message.visibleTextOrNull() ?: ""

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

private fun MessageDto.visibleTextOrNull(): String? {
    val attachmentCaption =
        attachments.firstOrNull { !it.caption.isNullOrBlank() }?.caption
            ?: attachmentsInline.firstOrNull { !it.caption.isNullOrBlank() }?.caption

    return decryptedContent
        ?: translatedForMe
        ?: rawContent
        ?: content
        ?: attachmentCaption
}

fun MessageDto.isWithinActionWindow(): Boolean {
    return try {
        val created = java.time.Instant.parse(createdAt)
        val ageMillis = java.time.Duration.between(
            created,
            java.time.Instant.now()
        ).toMillis()

        ageMillis <= 15 * 60 * 1000
    } catch (_: Exception) {
        false
    }
}