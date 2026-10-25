package com.chatforia.android.messages

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagePrivacyRulesTest {
    @Test fun defaultBlurConcealsExistingAndIncomingMessages() {
        for (age in listOf("old", "new")) {
            assertTrue(
                "$age message should be hidden",
                shouldConcealChatMessage(true, false, false, false)
            )
        }
    }

    @Test fun revealIsExplicitAndLocal() {
        assertFalse(shouldConcealChatMessage(true, true, false, false))
        assertTrue(shouldConcealChatMessage(true, false, false, false))
    }

    @Test fun blurOffShowsOrdinaryMessages() {
        assertFalse(shouldConcealChatMessage(false, false, false, false))
    }

    @Test fun deletionNoticesRemainVisible() {
        assertFalse(shouldConcealChatMessage(true, false, true, false))
        assertFalse(shouldConcealChatMessage(true, false, false, true))
    }
}
