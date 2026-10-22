package com.chatforia.android.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRecoveryCoordinatorTest {
    private val pending = RecoveryDeviceIdentity("new-device-id", "new-public")

    @Test
    fun serverFailureLeavesOldIdentityUntouchedAndStagedForRetry() {
        var activeId = "revoked-id"
        var activations = 0
        assertThrows(IllegalStateException::class.java) {
            performRevokedDeviceRecovery(
                oldDeviceId = activeId,
                stageIdentity = { pending },
                register = { throw IllegalStateException("Network unavailable") },
                activateIdentity = { activations++; activeId = it },
                password = "test-password",
                mfaCode = null
            )
        }
        assertEquals("revoked-id", activeId)
        assertEquals(0, activations)
    }

    @Test
    fun successfulRecoveryActivatesExactlyTheIdentityConfirmedByServer() {
        var activeId = "revoked-id"
        var requested: RecoverDeviceRequest? = null
        performRevokedDeviceRecovery(
            oldDeviceId = activeId,
            stageIdentity = { pending },
            register = {
                requested = it
                DeviceRegistrationResponse(
                    device = LinkedDeviceDto(deviceId = it.newDeviceId, publicKey = it.publicKey)
                )
            },
            activateIdentity = { activeId = it },
            password = "test-password",
            mfaCode = "123456"
        )
        assertEquals("new-device-id", activeId)
        assertEquals("revoked-id", requested?.oldDeviceId)
        assertEquals("new-public", requested?.publicKey)
        assertEquals("123456", requested?.mfaCode)
    }

    @Test
    fun ambiguousSuccessCannotActivateUnverifiedDevice() {
        var activated = false
        assertThrows(IllegalStateException::class.java) {
            performRevokedDeviceRecovery(
                oldDeviceId = "revoked-id",
                stageIdentity = { pending },
                register = { DeviceRegistrationResponse(device = null) },
                activateIdentity = { activated = true },
                password = "test-password", mfaCode = null
            )
        }
        assertFalse(activated)
    }

    @Test
    fun mismatchedServerPublicKeyCannotActivateDevice() {
        var activated = false
        assertThrows(IllegalStateException::class.java) {
            performRevokedDeviceRecovery(
                oldDeviceId = "revoked-id",
                stageIdentity = { pending },
                register = {
                    DeviceRegistrationResponse(
                        device = LinkedDeviceDto(deviceId = pending.deviceId, publicKey = "different-key")
                    )
                },
                activateIdentity = { activated = true },
                password = "test-password", mfaCode = null
            )
        }
        assertFalse(activated)
    }

    @Test
    fun recoveryRejectsReusingRevokedDeviceIdBeforeNetworkRequest() {
        var requested = false
        assertThrows(IllegalArgumentException::class.java) {
            performRevokedDeviceRecovery(
                oldDeviceId = "revoked-id",
                stageIdentity = { RecoveryDeviceIdentity("revoked-id", "some-key") },
                register = { requested = true; DeviceRegistrationResponse() },
                activateIdentity = {},
                password = "test-password", mfaCode = null
            )
        }
        assertFalse(requested)
    }
}
