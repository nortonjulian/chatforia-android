package com.chatforia.android.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreatorReferralStoreTest {
    @Test
    fun acceptsOnlyChatforiaReferralLinks() {
        assertEquals("CREATOR_7", CreatorReferralStore.codeFromLink("https://chatforia.com/ref/creator_7"))
        assertEquals("CREATOR-7", CreatorReferralStore.codeFromLink("https://www.chatforia.com/register?ref=creator-7"))
        assertEquals("CREATOR7", CreatorReferralStore.codeFromLink("chatforia://ref/creator7"))
        assertNull(CreatorReferralStore.codeFromLink("https://notchatforia.com/ref/creator7"))
        assertNull(CreatorReferralStore.codeFromLink("http://chatforia.com/ref/creator7"))
        assertNull(CreatorReferralStore.codeFromLink("https://chatforia.com/ref/a"))
        assertNull(CreatorReferralStore.codeFromLink("https://chatforia.com/ref/creator7/other"))
        assertNull(CreatorReferralStore.codeFromLink("https://chatforia.com/register?ref=creator%2F7"))
    }
}
