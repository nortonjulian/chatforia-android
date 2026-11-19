package com.chatforia.android.socket

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomResumeTrackerTest {
    @Test
    fun staleNoneCannotClearNewMatch() {
        val tracker = RandomResumeTracker()
        val pending = tracker.begin()
        tracker.matched(42)
        assertNull(tracker.acceptNone(pending))
        assertTrue(tracker.ended(42))
    }

    @Test
    fun onlyLatestResumeCanExpireSession() {
        val tracker = RandomResumeTracker()
        tracker.matched(42)
        val old = tracker.begin()
        val current = tracker.begin()
        assertNull(tracker.acceptNone(old))
        assertEquals(42, tracker.acceptNone(current))
        assertFalse(tracker.ended(42))
    }

    @Test
    fun staleResumedAndEndedEventsCannotReplaceActiveRoom() {
        val tracker = RandomResumeTracker()
        val pending = tracker.begin()
        tracker.matched(43)
        assertFalse(tracker.acceptResumed(pending, 42))
        assertFalse(tracker.ended(42))
        assertTrue(tracker.ended(43))
    }

    @Test
    fun validResumeRebindsAndInvalidatesItsNegativeResponse() {
        val tracker = RandomResumeTracker()
        val pending = tracker.begin()
        assertFalse(tracker.acceptResumed(null, 42))
        assertTrue(tracker.acceptResumed(pending, 42))
        assertNull(tracker.acceptNone(pending))
        assertTrue(tracker.ended(42))
    }
}
