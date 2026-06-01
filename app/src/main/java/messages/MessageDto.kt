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
    val translatedForMe: String? = null,

    val createdAt: String,

    val sender: SenderDto,

    val chatRoomId: Int? = null,

    val clientMessageId: String? = null
)

@Serializable
data class SenderDto(
    val id: Int,
    val username: String? = null
)