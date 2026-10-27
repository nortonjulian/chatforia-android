package com.chatforia.android.socket

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomSearchIntentTest {
    @Test fun noSearchDoesNotJoinOnConnect() {
        val intent = RandomSearchIntent()
        assertFalse(intent.shouldJoin(connected = true))
    }

    @Test fun outstandingSearchRejoinsOnlyWhenConnected() {
        val intent = RandomSearchIntent()
        intent.request()
        assertFalse(intent.shouldJoin(connected = false))
        assertTrue(intent.shouldJoin(connected = true))
        // Repeated reconnects can rejoin after the server clears the old socket.
        assertTrue(intent.shouldJoin(connected = true))
    }

    @Test fun cancelOrMatchedSessionDoesNotAutomaticallyRequeue() {
        val intent = RandomSearchIntent()
        intent.request()
        intent.clear()
        assertFalse(intent.shouldJoin(connected = true))
        intent.request()
        assertTrue(intent.shouldJoin(connected = true))
        intent.clear()
        assertFalse(intent.shouldJoin(connected = true))
    }
}
