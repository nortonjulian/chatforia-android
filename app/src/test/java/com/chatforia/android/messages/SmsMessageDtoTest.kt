package com.chatforia.android.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMessageDtoTest {

    @Test
    fun isOutgoing_isTrueWhenDirectionIsOut() {
        val message = smsMessage(
            direction = "out"
        )

        assertTrue(message.isOutgoing)
    }

    @Test
    fun isOutgoing_isCaseInsensitiveAndTrimsWhitespace() {
        val message = smsMessage(
            direction = " OUT "
        )

        assertTrue(message.isOutgoing)
    }

    @Test
    fun isOutgoing_isFalseWhenDirectionIsIn() {
        val message = smsMessage(
            direction = "in"
        )

        assertFalse(message.isOutgoing)
    }

    @Test
    fun trimmedBody_returnsTrimmedBody() {
        val message = smsMessage(
            body = "  hello there  "
        )

        assertEquals("hello there", message.trimmedBody)
        assertTrue(message.hasText)
    }

    @Test
    fun blankBody_hasNoText() {
        val message = smsMessage(
            body = "   "
        )

        assertEquals(null, message.trimmedBody)
        assertFalse(message.hasText)
    }

    @Test
    fun displayFallbackText_prefersTrimmedBody() {
        val message = smsMessage(
            body = "  real text  ",
            media = listOf(
                SmsMediaItemDto(
                    url = "https://example.com/photo.jpg"
                )
            )
        )

        assertEquals("real text", message.displayFallbackText)
    }

    @Test
    fun displayFallbackText_returnsPhotoForImageMedia() {
        val message = smsMessage(
            body = null,
            media = listOf(
                SmsMediaItemDto(
                    url = "https://example.com/photo.jpg"
                )
            )
        )

        assertEquals("Photo", message.displayFallbackText)
    }

    @Test
    fun displayFallbackText_returnsVideoForVideoMedia() {
        val message = smsMessage(
            body = null,
            media = listOf(
                SmsMediaItemDto(
                    url = "https://example.com/video.mp4"
                )
            )
        )

        assertEquals("Video", message.displayFallbackText)
    }

    @Test
    fun displayFallbackText_returnsAudioForAudioMedia() {
        val message = smsMessage(
            body = null,
            media = listOf(
                SmsMediaItemDto(
                    url = "https://example.com/audio.mp3"
                )
            )
        )

        assertEquals("Audio", message.displayFallbackText)
    }

    @Test
    fun displayFallbackText_returnsAttachmentForGenericMedia() {
        val message = smsMessage(
            body = null,
            media = listOf(
                SmsMediaItemDto(
                    url = "https://example.com/file.bin"
                )
            )
        )

        assertEquals("Attachment", message.displayFallbackText)
    }

    @Test
    fun displayFallbackText_returnsEmptyStringWhenNoBodyOrMedia() {
        val message = smsMessage(
            body = null,
            media = emptyList()
        )

        assertEquals("", message.displayFallbackText)
    }

    @Test
    fun optimisticOutgoing_createsOutgoingOptimisticMessage() {
        val message = SmsMessageDto.optimisticOutgoing(
            threadId = 123,
            to = "+15551234567",
            body = "hello",
            mediaUrls = listOf("https://example.com/photo.jpg")
        )

        assertTrue(message.id < 0)
        assertEquals(123, message.threadId)
        assertEquals("out", message.direction)
        assertEquals("+15551234567", message.toNumber)
        assertEquals("hello", message.body)
        assertEquals(1, message.media.size)
        assertTrue(message.optimistic)
        assertFalse(message.failed)
    }

    private fun smsMessage(
        id: Int = 1,
        threadId: Int? = 10,
        direction: String = "in",
        body: String? = "hello",
        media: List<SmsMediaItemDto> = emptyList(),
        createdAt: String = "2026-01-01T00:00:00Z"
    ): SmsMessageDto {
        return SmsMessageDto(
            id = id,
            threadId = threadId,
            direction = direction,
            body = body,
            media = media,
            createdAt = createdAt
        )
    }
}