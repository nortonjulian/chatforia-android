package com.chatforia.android.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDtoPlanAccessTest {

    @Test
    fun plusIsPaidButNotPremium() {
        val user = UserDto(id = 1, plan = "PLUS", isPremium = false)

        assertFalse(user.hasPremiumAccess())
        assertTrue(user.isPaidPlan())
        assertEquals("PLUS", user.accessPlan())
    }

    @Test
    fun activePremiumIsPaidAndPremium() {
        val user = UserDto(id = 1, plan = "PREMIUM", isPremium = true)

        assertTrue(user.hasPremiumAccess())
        assertTrue(user.isPaidPlan())
        assertEquals("PREMIUM", user.accessPlan())
    }

    @Test
    fun backendDeniedPremiumFallsBackToFree() {
        val user = UserDto(id = 1, plan = "PREMIUM", isPremium = false)

        assertFalse(user.hasPremiumAccess())
        assertFalse(user.isPaidPlan())
        assertEquals("FREE", user.accessPlan())
    }

    @Test
    fun premiumWithoutAuthoritativeFlagKeepsLegacyFallback() {
        val user = UserDto(id = 1, plan = "PREMIUM", isPremium = null)

        assertTrue(user.hasPremiumAccess())
        assertTrue(user.isPaidPlan())
        assertEquals("PREMIUM", user.accessPlan())
    }

    @Test
    fun freeAndWirelessRemainFree() {
        for (plan in listOf("FREE", "WIRELESS")) {
            val user = UserDto(id = 1, plan = plan, isPremium = false)

            assertFalse(user.hasPremiumAccess())
            assertFalse(user.isPaidPlan())
            assertEquals("FREE", user.accessPlan())
        }
    }

    @Test
    fun adminGetsPremiumAccessOverride() {
        val user =
            UserDto(
                id = 1,
                plan = "FREE",
                role = "ADMIN",
                isPremium = false
            )

        assertTrue(user.hasPremiumAccess())
        assertTrue(user.isPaidPlan())
        assertEquals("PREMIUM", user.accessPlan())
    }
}
