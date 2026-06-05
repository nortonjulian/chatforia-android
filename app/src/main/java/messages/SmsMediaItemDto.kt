package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class SmsMediaItemDto(
    val url: String,
    val contentType: String? = null
) {
    val normalizedContentType: String
        get() = contentType?.lowercase().orEmpty()

    val isImage: Boolean
        get() =
            normalizedContentType.startsWith("image/") ||
                    url.lowercase().let {
                        it.contains(".jpg") ||
                                it.contains(".jpeg") ||
                                it.contains(".png") ||
                                it.contains(".gif") ||
                                it.contains(".webp")
                    }

    val isVideo: Boolean
        get() =
            normalizedContentType.startsWith("video/") ||
                    url.lowercase().let {
                        it.contains(".mp4") ||
                                it.contains(".mov") ||
                                it.contains(".webm")
                    }

    val isAudio: Boolean
        get() =
            normalizedContentType.startsWith("audio/") ||
                    url.lowercase().let {
                        it.contains(".mp3") ||
                                it.contains(".m4a") ||
                                it.contains(".wav") ||
                                it.contains(".aac") ||
                                it.contains(".ogg")
                    }
}