package com.chatforia.android.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingCallFreshnessTest {
    private val nowMs = 1_000_000L

    @Test
    fun unknownSendTimeIsNotExpired() {
        assertFalse(
            IncomingCallFreshness.isExpired(
                sentTimeMs = 0L,
                nowMs = nowMs
            )
        )
    }

    @Test
    fun messageAtMaximumAgeIsStillAccepted() {
        assertFalse(
            IncomingCallFreshness.isExpired(
                sentTimeMs =
                    nowMs -
                        IncomingCallFreshness.MAX_INCOMING_CALL_AGE_MS,
                nowMs = nowMs
            )
        )
    }

    @Test
    fun messageOlderThanMaximumAgeIsExpired() {
        assertTrue(
            IncomingCallFreshness.isExpired(
                sentTimeMs =
                    nowMs -
                        IncomingCallFreshness.MAX_INCOMING_CALL_AGE_MS -
                        1L,
                nowMs = nowMs
            )
        )
    }

    @Test
    fun futureTimestampIsTreatedAsClockSkew() {
        assertFalse(
            IncomingCallFreshness.isExpired(
                sentTimeMs = nowMs + 5_000L,
                nowMs = nowMs
            )
        )
    }
}
