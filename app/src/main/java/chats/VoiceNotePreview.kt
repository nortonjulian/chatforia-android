package com.chatforia.android.chats

/**
 * Presentation only: never rewrite the original message or its encrypted text.
 */
internal fun voiceNotePreview(text: String?, hasMedia: Boolean): String? {
    if (!hasMedia) return text
    return if (text?.trim()?.matches(Regex("""(?i)\[(?:voice note|audio)\]""")) == true) {
        "Voice note"
    } else text
}
