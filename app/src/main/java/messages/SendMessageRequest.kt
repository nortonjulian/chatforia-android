package com.chatforia.android.messages

import kotlinx.serialization.Serializable
import com.chatforia.android.crypto.EncryptedMessagePayloadForUser

@Serializable
data class SendMessageRequest(
    val chatRoomId: Int,
    val content: String? = null,
    val contentCiphertext: String? = null,
    val encryptedKeys: Map<String, String>? = null,
    val encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>? = null,
    val encryptionVersion: Int? = null,
    val clientMessageId: String,
    val expireSeconds: Int = 0,
    val attachmentsInline: List<AttachmentDto> = emptyList()
)
