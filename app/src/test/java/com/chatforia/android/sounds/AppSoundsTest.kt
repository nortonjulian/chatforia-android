package com.chatforia.android.sounds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSoundsTest {

    @Test
    fun premiumSoundsRequirePremiumPlan() {
        val premiumTone =
            AppMessageTones.all.first {
                it.requiredPlan == RequiredPlan.Premium
            }

        assertFalse(
            premiumTone.isAvailableForPlan("FREE")
        )
        assertFalse(
            premiumTone.isAvailableForPlan("PLUS")
        )
        assertFalse(
            premiumTone.isAvailableForPlan("WIRELESS")
        )
        assertTrue(
            premiumTone.isAvailableForPlan("PREMIUM")
        )
    }

    @Test
    fun freeSoundsRemainAvailableToAllPlans() {
        val freeTone =
            AppMessageTones.all.first {
                it.requiredPlan == RequiredPlan.Free
            }

        assertTrue(
            freeTone.isAvailableForPlan("FREE")
        )
        assertTrue(
            freeTone.isAvailableForPlan("PLUS")
        )
        assertTrue(
            freeTone.isAvailableForPlan("PREMIUM")
        )
    }

    @Test
    fun resolverFallsBackWhenPlusSelectsPremiumTone() {
        val premiumTone =
            AppMessageTones.all.first {
                it.requiredPlan == RequiredPlan.Premium
            }

        assertEquals(
            "Default.mp3",
            resolvedMessageToneForPlan(
                premiumTone.filename,
                "PLUS"
            )
        )
    }
}
