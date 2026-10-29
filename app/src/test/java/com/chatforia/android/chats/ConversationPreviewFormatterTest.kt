package com.chatforia.android.chats

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationPreviewFormatterTest {

    @Test
    fun bracketedAttachmentPreviewsUseCleanLabels() {
        val cases = mapOf(
            "[image]" to "Photo",
            "[video]" to "Video",
            "[gif]" to "GIF",
            "[audio]" to "Voice message",
            "[voice note]" to "Voice message",
            "[attachment]" to "Attachment",
            "[media]" to "Media"
        )

        cases.forEach { (input, expected) ->
            assertEquals(
                expected,
                formatConversationPreview(ConversationLastDto(text = input))
            )
        }
    }

    @Test
    fun mediaMetadataProvidesFallbackLabel() {
        assertEquals(
            "Photo",
            formatConversationPreview(
                ConversationLastDto(
                    hasMedia = true,
                    mediaKinds = listOf("IMAGE")
                )
            )
        )
    }

    @Test
    fun ordinaryTextRemainsUnchanged() {
        assertEquals(
            "Hello there!",
            formatConversationPreview(
                ConversationLastDto(text = "Hello there!")
            )
        )
    }

    @Test
    fun emptyConversationUsesDefaultPreview() {
        assertEquals(
            "No messages yet",
            formatConversationPreview(null)
        )
    }
}
