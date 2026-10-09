package com.chatforia.android.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdEligibilityTest {

    @Test
    fun freeFallbackShowsAdsWhileEntitlementsAreLoading() {
        assertTrue(
            resolveAdsEnabled(
                isAdmin = false,
                backendAdsEnabled = null,
                fallbackPlanAdsEnabled = true
            )
        )
    }

    @Test
    fun paidFallbackHidesAdsWhileEntitlementsAreLoading() {
        assertFalse(
            resolveAdsEnabled(
                isAdmin = false,
                backendAdsEnabled = null,
                fallbackPlanAdsEnabled = false
            )
        )
    }

    @Test
    fun backendAdsEnabledOverridesPlanFallback() {
        assertFalse(
            resolveAdsEnabled(
                isAdmin = false,
                backendAdsEnabled = false,
                fallbackPlanAdsEnabled = true
            )
        )

        assertTrue(
            resolveAdsEnabled(
                isAdmin = false,
                backendAdsEnabled = true,
                fallbackPlanAdsEnabled = false
            )
        )
    }

    @Test
    fun adminAlwaysRemainsAdFree() {
        assertFalse(
            resolveAdsEnabled(
                isAdmin = true,
                backendAdsEnabled = true,
                fallbackPlanAdsEnabled = true
            )
        )
    }
}
