package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class MessagesResponse(
    val items: List<MessageDto>,
    val nextCursor: String? = null,
    val nextCursorId: Int? = null,
    val count: Int? = null
)

@Serializable
data class MessageDto(
    val id: Int,

    val rawContent: String? = null,
    val content: String? = null,
    val translatedForMe: String? = null,
    val decryptedContent: String? = null,

    val contentCiphertext: String? = null,
    val encryptedKeyForMe: String? = null,
    val encryptedKeys: Map<String, String>? = null,
    val encryptionVersion: Int? = null,

    val createdAt: String,
    val expiresAt: String? = null,
    val editedAt: String? = null,
    val deletedAt: String? = null,
    val deletedForAll: Boolean? = null,
    val deletedBySender: Boolean? = null,
    val revision: Int? = null,

    val sender: SenderDto,
    val senderId: Int? = null,
    val chatRoomId: Int? = null,

    val clientMessageId: String? = null,

    val readBy: List<SenderDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList(),
    val attachmentsInline: List<AttachmentDto> = emptyList(),

    val optimistic: Boolean = false,
    val failed: Boolean = false
)

@Serializable
data class SenderDto(
    val id: Int,
    val username: String? = null,
    val avatarUrl: String? = null,
    val publicKey: String? = null
)

@Serializable
data class AttachmentDto(
    val id: Int? = null,
    val kind: String,
    val url: String,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationSec: Double? = null,
    val caption: String? = null,
    val thumbUrl: String? = null,
    val createdAt: String? = null
)