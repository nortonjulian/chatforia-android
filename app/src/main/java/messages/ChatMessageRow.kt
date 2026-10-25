package com.chatforia.android.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageRow(
    message: MessageDto,
    isMine: Boolean,
    blurByDefault: Boolean = false,
    holdToReveal: Boolean = false,
    onEdit: ((MessageDto) -> Unit)? = null,
    onDelete: ((MessageDto) -> Unit)? = null,
    onReport: ((MessageDto) -> Unit)? = null,
    onMessageInfo: ((MessageDto) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val requireHold = blurByDefault && holdToReveal
    // A privacy-mode switch must not preserve a prior tap-to-reveal.
    var revealed by rememberSaveable(message.id, message.clientMessageId, requireHold) { mutableStateOf(false) }
    var fingerDown by remember(message.id, message.clientMessageId, requireHold) { mutableStateOf(false) }
    val currentlyRevealed = isPrivacyMessageRevealed(blurByDefault, holdToReveal, revealed, fingerDown)
    val concealed = shouldConcealChatMessage(
        blurByDefault = blurByDefault,
        revealed = currentlyRevealed,
        deletedForAll = message.deletedForAll == true,
        deletedForSender = message.deletedBySender == true
    )
    val clipboard = LocalClipboardManager.current

    val attachments =
        if (message.attachments.isNotEmpty()) {
            message.attachments
        } else {
            message.attachmentsInline
        }

    val hasAttachments = attachments.isNotEmpty()
    val copyText = if (concealed) null else message.visibleTextOrNull(isMine)
    val canEdit =
        !concealed &&
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
                    .then(
                        if (requireHold) {
                            Modifier.pointerInput(message.id, message.clientMessageId) {
                                detectTapGestures(
                                    onPress = {
                                        fingerDown = true
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            fingerDown = false
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { showMenu = true }
                            )
                        }
                    )
            ) {
                if (concealed) {
                    Text(
                        text = if (requireHold) "Message hidden · Hold to reveal" else "Message hidden · Tap to reveal",
                        modifier = Modifier
                            .then(if (requireHold) Modifier else Modifier.clickable { revealed = true })
                            .semantics { contentDescription = if (requireHold) "Message hidden. Hold to reveal" else "Message hidden. Tap to reveal" }
                            .padding(12.dp)
                    )
                } else {
                    if (hasAttachments) {
                        MessageAttachmentsView(
                            attachments = attachments,
                            isMine = isMine
                        )
                    }

                    if (shouldShowTextBubble(message, hasAttachments, isMine)) {
                        MessageBubble(
                            message = message,
                            isMine = isMine
                        )
                    }
                }
            }
            if (requireHold && message.deletedForAll != true) {
                // Hold is reserved for reveal. Keep delete/report/info available
                // outside the gesture detector instead of making them unreachable.
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.android_chat_message_row_actions)
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
                    text = { Text(stringResource(R.string.android_chat_message_row_copy)) },
                    onClick = {
                        clipboard.setText(AnnotatedString(copyText))
                        showMenu = false
                    }
                )
            }

            if (canEdit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.android_chat_message_row_edit)) },
                    onClick = {
                        showMenu = false
                        onEdit?.invoke(message)
                    }
                )
            }

            if (canDeleteForMe || canDeleteForEveryone) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.android_chats_delete)) },
                    onClick = {
                        showMenu = false
                        onDelete?.invoke(message)
                    }
                )
            }

            if (!isMine && message.deletedForAll != true) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.android_chat_message_row_report)) },
                    onClick = {
                        showMenu = false
                        onReport?.invoke(message)
                    }
                )
            }

            if (isMine && message.deletedForAll != true) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.android_chat_message_row_message_info)) },
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
    hasAttachments: Boolean,
    isMine: Boolean
): Boolean {
    if (message.deletedForAll == true || message.deletedBySender == true) return true

    val text = message.visibleTextOrNull(isMine) ?: ""

    val normalized = text.trim().lowercase()

    val placeholderTexts = setOf(
        "[image]",
        "[video]",
        "[audio]",
        "[voice note]",
        "[file]",
        "[attachment]",
        "[gif]",
        "attachment",
        "media attachment"
    )

    if (normalized.isBlank()) return false

    if (hasAttachments && placeholderTexts.contains(normalized)) {
        return false
    }

    return true
}

private fun MessageDto.visibleTextOrNull(isMine: Boolean): String? {
    val attachmentCaption =
        attachments.firstOrNull { !it.caption.isNullOrBlank() }?.caption
            ?: attachmentsInline.firstOrNull { !it.caption.isNullOrBlank() }?.caption

    return if (isMine) {
        listOf(
            rawContent,
            content,
            decryptedContent,
            attachmentCaption
        ).firstOrNull { !it.isNullOrBlank() }
    } else {
        listOf(
            translatedForMe,
            decryptedContent,
            rawContent,
            content,
            attachmentCaption
        ).firstOrNull { !it.isNullOrBlank() }
    }
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