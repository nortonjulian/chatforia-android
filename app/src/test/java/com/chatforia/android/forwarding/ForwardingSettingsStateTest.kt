package com.chatforia.android.forwarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardingSettingsStateTest {
    @Test fun zoneMustBeAnIanaIdentifier() {
        assertTrue(ForwardingSettingsState(forwardQuietHoursTimeZone = "MST").validationErrors.containsKey("timeZone"))
        assertTrue(ForwardingSettingsState(forwardQuietHoursTimeZone = "+07:00").validationErrors.containsKey("timeZone"))
        assertFalse(ForwardingSettingsState(forwardQuietHoursTimeZone = "America/Denver").validationErrors.containsKey("timeZone"))
    }
    @Test fun destinationsAreRequiredOnlyWhenEnabled() {
        assertTrue(ForwardingSettingsState(forwardSmsToEmail = true).validationErrors.isEmpty())
        assertTrue(ForwardingSettingsState(forwardingEnabledSms = true).validationErrors.containsKey("smsToggle"))
        assertTrue(ForwardingSettingsState(
            forwardingEnabledSms = true, forwardSmsToEmail = true, forwardEmail = ""
        ).validationErrors.containsKey("forwardEmail"))
        assertTrue(ForwardingSettingsState(
            forwardingEnabledCalls = true, forwardToPhoneE164 = "123"
        ).validationErrors.containsKey("forwardToPhoneE164"))
    }

    @Test fun quietHoursRequireACompleteValidPairAndAllowOvernight() {
        assertTrue(ForwardingSettingsState(forwardQuietHoursStart = 22).validationErrors.containsKey("quiet"))
        assertTrue(ForwardingSettingsState(forwardQuietHoursEnd = 6).validationErrors.containsKey("quiet"))
        assertTrue(ForwardingSettingsState(
            forwardQuietHoursStart = 24, forwardQuietHoursEnd = 6
        ).validationErrors.containsKey("quiet"))
        assertTrue(ForwardingSettingsState(
            forwardQuietHoursStart = 8, forwardQuietHoursEnd = 8
        ).validationErrors.containsKey("quiet"))
        assertFalse(ForwardingSettingsState(
            forwardQuietHoursStart = 22, forwardQuietHoursEnd = 6
        ).validationErrors.containsKey("quiet"))
    }
}
