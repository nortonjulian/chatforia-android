package com.chatforia.android.wireless

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
