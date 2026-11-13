package com.chatforia.android.chats

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisappearingPreviewTest {
    @Test
    fun expiredPreviewIsHiddenAtDeadlineWithoutChangingOtherThreads() {
        val rows = listOf(
            ConversationDto(kind = "chat", id = 1, title = "A",
                last = ConversationLastDto(text = "secret", messageId = 41,
                    expiresAt = "2026-01-01T00:00:00Z")),
            ConversationDto(kind = "chat", id = 2, title = "B",
                last = ConversationLastDto(text = "later", messageId = 42,
                    expiresAt = "2026-01-02T00:00:00Z")),
            ConversationDto(kind = "sms", id = 3, title = "C",
                last = ConversationLastDto(text = "SMS", messageId = 43))
        )
        val result = withoutExpiredPreviews(rows, Instant.parse("2026-01-01T00:00:00Z"))
        assertNull(result[0].last)
        assertEquals(42, result[1].last?.messageId)
        assertEquals(43, result[2].last?.messageId)
    }
}
