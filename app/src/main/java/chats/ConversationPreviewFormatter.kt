package com.chatforia.android.chats

internal fun formatConversationPreview(last: ConversationLastDto?): String {
    if (last == null) return "No messages yet"

    val text = last.text?.trim().orEmpty()

    val labels = mapOf(
        "[image]" to "Photo",
        "[photo]" to "Photo",
        "[video]" to "Video",
        "[gif]" to "GIF",
        "[audio]" to "Voice message",
        "[voice note]" to "Voice message",
        "[voice message]" to "Voice message",
        "[attachment]" to "Attachment",
        "[file]" to "Attachment",
        "[media]" to "Media"
    )

    labels[text.lowercase()]?.let { return it }

    if (text.isNotEmpty()) return text

    if (last.hasMedia == true) {
        val kinds = last.mediaKinds.orEmpty().map { it.uppercase() }
        return when {
            "IMAGE" in kinds -> "Photo"
            "VIDEO" in kinds -> "Video"
            "GIF" in kinds -> "GIF"
            "AUDIO" in kinds -> "Voice message"
            else -> "Media"
        }
    }

    return "No messages yet"
}
