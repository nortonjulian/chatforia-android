package com.chatforia.android.blocking

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class BlockedAccountsContractsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun blockedDateUsesDeviceTimezone() {
        val timestamp = "2026-10-24T00:05:00.000Z"

        assertEquals(
            "2026-10-23",
            localBlockedDate(timestamp, ZoneId.of("America/Denver"))
        )

        assertEquals(
            "2026-10-24",
            localBlockedDate(timestamp, ZoneId.of("UTC"))
        )
    }

    @Test fun listUsesPermanentUserIdNotContactId() {
        val response = json.decodeFromString<BlockedAccountsResponse>(
            """{"items":[{"id":33,"userId":78,"username":"sam","displayName":"Sam","createdAt":"2026-10-23T10:00:00Z"}]}"""
        )
        assertEquals(33, response.items.single().id)
        assertEquals(78, response.items.single().userId)
        assertEquals("Sam", response.items.single().title)
    }

    @Test fun absentDisplayNameUsesUsername() {
        val response = json.decodeFromString<BlockedAccountsResponse>(
            """{"items":[{"id":2,"userId":6,"username":"papi"}]}"""
        )
        assertEquals("papi", response.items.single().title)
    }

    @Test fun unblockResponseCanBeIdempotent() {
        val response = json.decodeFromString<UnblockAccountResponse>(
            """{"success":true,"removed":false,"userId":14}"""
        )
        assertTrue(response.success)
        assertFalse(response.removed)
        assertEquals(14, response.userId)
    }
}
