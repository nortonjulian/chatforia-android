package com.chatforia.android.groups

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupInvitationLinkTest {
    @Test fun parsesWebAndAppLinks() {
        val code = "AbC_def-1234"
        assertEquals(code, GroupInvitationLink.extract("https://www.chatforia.com/join/$code"))
        assertEquals(code, GroupInvitationLink.extract("https://chatforia.com/join/$code"))
        assertEquals(code, GroupInvitationLink.extract("chatforia://join/$code"))
    }

    @Test fun rejectsUntrustedOrMalformedLinks() {
        assertNull(GroupInvitationLink.extract("https://other.example/join/AbC_def-1234"))
        assertNull(GroupInvitationLink.extract("https://www.chatforia.com/join/AbC_def-1234/extra"))
        assertNull(GroupInvitationLink.extract("chatforia://oauth/AbC_def-1234"))
        assertNull(GroupInvitationLink.extract("https://www.chatforia.com/join/../admin"))
    }
}
