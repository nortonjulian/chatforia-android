package com.chatforia.android.messages

/**
 * A conservative English-language display filter, not a content classifier.
 * Applied AFTER decryption and only to presented text. Never change stored
 * messages, translation payloads, report evidence, or clipboard contents.
 */
private val explicitDisplayPattern = Regex(
    """(?i)\b(?:motherfucker|bullshit|fucking|fucked|fuck|shitty|shit|asshole|bitch|cunt|blow[\s-]?job|hand[\s-]?job)\b"""
)

fun maskExplicitText(text: String, allowExplicitContent: Boolean): String {
    if (allowExplicitContent || text.isEmpty()) return text
    return explicitDisplayPattern.replace(text) { match ->
        match.value.map { character ->
            if (character.isWhitespace() || character == '-') character else '*'
        }.joinToString("")
    }
}
