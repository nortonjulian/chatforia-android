package com.chatforia.android.ria

import kotlinx.serialization.Serializable
import java.util.UUID

data class RiaChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String
)

@Serializable
data class RiaContextMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class RiaChatRequest(
    val messages: List<RiaContextMessageDto>,
    val memoryEnabled: Boolean = true,
    val filterProfanity: Boolean = false
)

@Serializable
data class RiaChatResponse(
    val reply: String
)

@Serializable
data class SuggestRepliesRequest(
    val messages: List<RiaContextMessageDto>,
    val draft: String,
    val filterProfanity: Boolean = false
)

@Serializable
data class SuggestRepliesResponse(
    val suggestions: List<String> = emptyList()
)

@Serializable
data class RewriteTextRequest(
    val text: String,
    val tone: String,
    val filterProfanity: Boolean = false
)

@Serializable
data class RewriteTextResponse(
    val rewrites: List<String> = emptyList()
)