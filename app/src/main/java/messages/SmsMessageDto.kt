package com.chatforia.android.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmsMessageDto(
    val id: Int,
    val threadId: Int? = null,
    val direction: String = "in",
    val fromNumber: String? = null,
    val toNumber: String? = null,
    val body: String? = null,
    val provider: String? = null,
    val providerMessageId: String? = null,

    @SerialName("mediaUrls")
    val media: List<SmsMediaItemDto> = emptyList(),

    val createdAt: String,
    val editedAt: String? = null,

    val optimistic: Boolean = false,
    val failed: Boolean = false
) {
    val isOutgoing: Boolean
        get() = direction.trim().lowercase() == "out"

    val trimmedBody: String?
        get() = body?.trim()?.takeIf { it.isNotEmpty() }

    val hasText: Boolean
        get() = trimmedBody != null

    val hasMedia: Boolean
        get() = media.isNotEmpty()

    val displayFallbackText: String
        get() {
            trimmedBody?.let { return it }

            if (media.any { it.isImage }) return "Photo"
            if (media.any { it.isVideo }) return "Video"
            if (media.any { it.isAudio }) return "Audio"

            return if (media.isNotEmpty()) "Attachment" else ""
        }

    companion object {
        fun optimisticOutgoing(
            threadId: Int,
            to: String,
            body: String?,
            mediaUrls: List<String> = emptyList()
        ): SmsMessageDto {
            val now = java.time.Instant.now().toString()

            return SmsMessageDto(
                id = -kotlin.math.abs(now.hashCode()),
                threadId = threadId,
                direction = "out",
                toNumber = to,
                body = body,
                media = mediaUrls.map { SmsMediaItemDto(url = it) },
                createdAt = now,
                optimistic = true,
                failed = false
            )
        }
    }
}