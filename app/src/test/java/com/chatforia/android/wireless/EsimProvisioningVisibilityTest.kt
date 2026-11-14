package com.chatforia.android.wireless

import com.chatforia.android.R
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

    @Test
    fun unlimitedPackDoesNotDependOnZeroMbFields() {
        assertTrue(isUnlimitedPack(true, null))
        assertTrue(isUnlimitedPack(null, "chatforia_esim_global_unlimited_premium"))
        assertFalse(isUnlimitedPack(false, "chatforia_esim_global_5_premium"))
    }

    @Test
    fun purchaseStatesKeepExpiredAndTerminatedDistinctFromQueued() {
        assertEquals(R.string.esim_status_awaiting_activation, purchaseStateLabelRes("AWAITING_ACTIVATION"))
        assertEquals(R.string.esim_status_expired, purchaseStateLabelRes("EXPIRED"))
        assertEquals(R.string.esim_status_terminated, purchaseStateLabelRes("TERMINATED"))
        assertEquals(R.string.esim_status_expiry_needs_review, purchaseStateLabelRes("EXPIRY_NEEDS_REVIEW"))
        assertEquals(R.string.esim_status_provider_unverified, purchaseStateLabelRes("UNKNOWN"))
        assertEquals(R.string.esim_scope_local, purchaseScopeLabelRes("chatforia_esim_local_3_premium"))
        assertEquals(R.string.esim_scope_europe, purchaseScopeLabelRes("chatforia_esim_europe_unlimited_premium"))
        assertEquals(R.string.esim_scope_global, purchaseScopeLabelRes("chatforia_esim_global_5_premium"))
    }
}
