package com.chatforia.android.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class AiEntitlementTest {

    @Test
    fun backendLevelIsAuthoritative() {
        assertEquals("NONE", resolveAiRewriteLevel("NONE", "PREMIUM"))
        assertEquals("STANDARD", resolveAiRewriteLevel("STANDARD", "FREE"))
        assertEquals("FULL", resolveAiRewriteLevel("FULL", "FREE"))
    }

    @Test
    fun fallbackPlanIsUsedWhileEntitlementsLoad() {
        assertEquals("NONE", resolveAiRewriteLevel(null, "FREE"))
        assertEquals("STANDARD", resolveAiRewriteLevel(null, "PLUS"))
        assertEquals("FULL", resolveAiRewriteLevel(null, "PREMIUM"))
    }
}
