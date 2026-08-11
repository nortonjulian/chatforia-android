package com.chatforia.android.calls

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwilioIncomingCallStoreTest {

    @After
    fun tearDown() {
        TwilioIncomingCallStore.clear()
    }

    @Test
    fun locallyClaimedCallMatchesOnlyItsCanonicalCallId() {
        TwilioIncomingCallStore
            .markLocallyClaimed(699)

        assertTrue(
            TwilioIncomingCallStore
                .isLocallyClaimed(699)
        )

        assertFalse(
            TwilioIncomingCallStore
                .isLocallyClaimed(700)
        )
    }

    @Test
    fun clearingMatchingClaimRemovesAuthority() {
        TwilioIncomingCallStore
            .markLocallyClaimed(699)

        TwilioIncomingCallStore
            .clearLocalClaim(699)

        assertFalse(
            TwilioIncomingCallStore
                .isLocallyClaimed(699)
        )
    }

    @Test
    fun clearingDifferentCallDoesNotRemoveAuthority() {
        TwilioIncomingCallStore
            .markLocallyClaimed(699)

        TwilioIncomingCallStore
            .clearLocalClaim(700)

        assertTrue(
            TwilioIncomingCallStore
                .isLocallyClaimed(699)
        )
    }
}
