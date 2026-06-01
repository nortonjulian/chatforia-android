package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val chatRoomId: Int,
    val content: String? = null,
    val contentCiphertext: String? = null,
    val encryptedKeys: Map<String, String>? = null,
    val clientMessageId: String
)