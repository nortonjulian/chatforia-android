package com.chatforia.android.messages

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsThreadDtoTest {

    @Test
    fun resolvedTitle_prefersDisplayName() {
        val thread = SmsThreadDto(
            id = 1,
            displayName = "  Display Name  ",
            contactName = "Contact Name",
            contactPhone = "+15551234567"
        )

        assertEquals("Display Name", thread.resolvedTitle)
    }

    @Test
    fun resolvedTitle_usesContactNameWhenDisplayNameIsBlank() {
        val thread = SmsThreadDto(
            id = 1,
            displayName = "   ",
            contactName = "  Contact Name  ",
            contactPhone = "+15551234567"
        )

        assertEquals("Contact Name", thread.resolvedTitle)
    }

    @Test
    fun resolvedTitle_usesContactPhoneWhenNamesAreBlank() {
        val thread = SmsThreadDto(
            id = 1,
            displayName = "   ",
            contactName = "",
            contactPhone = "  +15551234567  "
        )

        assertEquals("+15551234567", thread.resolvedTitle)
    }

    @Test
    fun resolvedTitle_fallsBackToSmsNumber() {
        val thread = SmsThreadDto(
            id = 42,
            displayName = null,
            contactName = null,
            contactPhone = null
        )

        assertEquals("SMS #42", thread.resolvedTitle)
    }

    @Test
    fun sortedMessages_sortsByCreatedAtThenId() {
        val later = smsMessage(
            id = 2,
            createdAt = "2026-01-01T00:00:02Z"
        )

        val earlierSecond = smsMessage(
            id = 3,
            createdAt = "2026-01-01T00:00:01Z"
        )

        val earlierFirst = smsMessage(
            id = 1,
            createdAt = "2026-01-01T00:00:01Z"
        )

        val thread = SmsThreadDto(
            id = 1,
            messages = listOf(
                later,
                earlierSecond,
                earlierFirst
            )
        )

        assertEquals(
            listOf(1, 3, 2),
            thread.sortedMessages.map { it.id }
        )
    }

    private fun smsMessage(
        id: Int,
        createdAt: String
    ): SmsMessageDto {
        return SmsMessageDto(
            id = id,
            threadId = 1,
            direction = "in",
            body = "message $id",
            createdAt = createdAt
        )
    }
}