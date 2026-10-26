package com.chatforia.android.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InvitePhoneNumberTest {
    @Test fun acceptsDomesticUsNumberWithoutCountryCode() {
        assertEquals("+12125550123", normalizeInvitePhoneNumber("2125550123", null))
        assertEquals("+12125550123", normalizeInvitePhoneNumber("(212) 555-0123", null))
        assertEquals("+12125550123", normalizeInvitePhoneNumber("1-212-555-0123", null))
    }

    @Test fun acceptsInternationalE164AndPlatformFormatting() {
        assertEquals("+442071838750", normalizeInvitePhoneNumber("+44 20 7183 8750", null))
        assertEquals("+33123456789", normalizeInvitePhoneNumber("01 23 45 67 89", "+33123456789"))
    }

    @Test fun rejectsInvalidNumbersAndPunctuation() {
        assertNull(normalizeInvitePhoneNumber("", null))
        assertNull(normalizeInvitePhoneNumber("12345", null))
        assertNull(normalizeInvitePhoneNumber("2125550123,", null))
        assertNull(normalizeInvitePhoneNumber("2125550123", "+0123"))
    }
}
