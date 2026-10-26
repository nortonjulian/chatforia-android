package com.chatforia.android.wireless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimProvisioningVisibilityTest {

    @Test
    fun activeEsimHidesProvisioningDetails() {
        assertFalse(
            shouldShowEsimProvisioningDetails("ACTIVE")
        )
    }

    @Test
    fun activeEsimStatusIsCaseInsensitive() {
        assertFalse(
            shouldShowEsimProvisioningDetails("active")
        )
    }

    @Test
    fun activeEsimMasksIccidForManagement() {
        assertEquals(
            "•••• •••• •••• 0814",
            maskedEsimIccid("8910300000059080814")
        )
    }

    @Test
    fun missingIccidIsNotShown() {
        assertNull(maskedEsimIccid(null))
    }

    @Test
    fun provisioningEsimShowsProvisioningDetails() {
        assertTrue(
            shouldShowEsimProvisioningDetails("PROVISIONING")
        )
    }

    @Test
    fun pendingEsimShowsProvisioningDetails() {
        assertTrue(
            shouldShowEsimProvisioningDetails("PENDING")
        )
    }
}
