package com.chatforia.android.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun MessageAttachmentsView(
    attachments: List<AttachmentDto>,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    Column(
        modifier = modifier.widthIn(max = 300.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            AttachmentCard(
                attachment = attachment,
                isMine = isMine
            )
        }
    }
}

@Composable
private fun AttachmentCard(
    attachment: AttachmentDto,
    isMine: Boolean
) {
    val kind = attachment.kind.uppercase()
    val mime = attachment.mimeType.orEmpty().lowercase()
    val url = attachment.url

    when {
        kind == "GIF" || mime == "image/gif" -> {
            ImageAttachment(url = url, caption = attachment.caption)
        }

        kind == "IMAGE" || mime.startsWith("image/") -> {
            ImageAttachment(
                url = attachment.thumbUrl ?: url,
                caption = attachment.caption
            )
        }

        kind == "VIDEO" || mime.startsWith("video/") -> {
            VideoAttachment(
                url = attachment.thumbUrl ?: url,
                caption = attachment.caption
            )
        }

        kind == "AUDIO" || mime.startsWith("audio/") -> {
            FileLikeAttachment(
                title = attachment.caption ?: "Audio message",
                subtitle = attachment.mimeType ?: "Audio attachment",
                iconType = FileIconType.Audio,
                isMine = isMine
            )
        }

        else -> {
            FileLikeAttachment(
                title = attachment.caption ?: "Attachment",
                subtitle = attachment.mimeType ?: "File",
                iconType = FileIconType.File,
                isMine = isMine
            )
        }
    }
}

@Composable
private fun ImageAttachment(
    url: String,
    caption: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AsyncImage(
            model = url,
            contentDescription = caption ?: "Image attachment",
            modifier = Modifier
                .widthIn(max = 260.dp)
                .heightIn(max = 260.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ChatforiaColors.cardBackground),
            contentScale = ContentScale.Fit
        )

        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = ChatforiaColors.secondaryText
            )
        }
    }
}

@Composable
private fun VideoAttachment(
    url: String,
    caption: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ChatforiaColors.cardBackground)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = caption ?: "Video attachment",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(30.dp)
                )
            }
        }

        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = ChatforiaColors.secondaryText
            )
        }
    }
}

private enum class FileIconType {
    File,
    Audio
}

@Composable
private fun FileLikeAttachment(
    title: String,
    subtitle: String,
    iconType: FileIconType,
    isMine: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color =
            if (isMine) {
                ChatforiaColors.accent.copy(alpha = 0.18f)
            } else {
                ChatforiaColors.cardBackground
            },
        modifier = Modifier.widthIn(max = 260.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector =
                    if (iconType == FileIconType.Audio) {
                        Icons.Default.Audiotrack
                    } else {
                        Icons.Default.AttachFile
                    },
                contentDescription = null,
                tint = ChatforiaColors.accent
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    color = ChatforiaColors.primaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = subtitle,
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}