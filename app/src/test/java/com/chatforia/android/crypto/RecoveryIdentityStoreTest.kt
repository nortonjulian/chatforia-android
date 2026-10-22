package com.chatforia.android.crypto

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecoveryIdentityStoreTest {
    private fun prefs(name: String): SharedPreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.getSharedPreferences(name, Context.MODE_PRIVATE).also {
            it.edit().clear().commit()
        }
    }

    @Test
    fun stagingDoesNotOverwriteOldDeviceOrSeparateAccountKeys() {
        val device = prefs("recovery-device-preserve")
        val account = prefs("recovery-account-preserve")
        device.edit()
            .putString("chatforia.device.id", "revoked-id")
            .putString("chatforia.device.publicKey", "old-public")
            .putString("chatforia.device.privateKey", "old-private")
            .commit()
        account.edit().putString("account.privateKey", "account-secret").commit()

        val staged = RecoveryIdentityStore(device,
            createKeyPair = { "new-public" to "new-private" },
            createDeviceId = { "fresh-id" }
        ).stage()

        assertEquals("fresh-id", staged.deviceId)
        assertEquals("revoked-id", device.getString("chatforia.device.id", null))
        assertEquals("old-public", device.getString("chatforia.device.publicKey", null))
        assertEquals("old-private", device.getString("chatforia.device.privateKey", null))
        assertEquals("account-secret", account.getString("account.privateKey", null))
    }

    @Test
    fun interruptedRecoveryReusesStagedIdentityAcrossStoreInstances() {
        val device = prefs("recovery-device-interrupted")
        var creations = 0
        val factory = { creations++; "new-public" to "new-private" }
        val first = RecoveryIdentityStore(device, factory) { "fresh-id" }.stage()
        val second = RecoveryIdentityStore(device, factory) { "should-not-be-used" }.stage()
        assertEquals(first, second)
        assertEquals(1, creations)
    }

    @Test
    fun activationRequiresCorrectStagedIdentityAndClearsStagingAtomically() {
        val device = prefs("recovery-device-activate")
        device.edit().putString("chatforia.device.id", "revoked-id").commit()
        val store = RecoveryIdentityStore(device,
            createKeyPair = { "new-public" to "new-private" },
            createDeviceId = { "fresh-id" }
        )
        store.stage()
        assertThrows(IllegalArgumentException::class.java) {
            store.activate("wrong-id")
        }
        assertEquals("revoked-id", device.getString("chatforia.device.id", null))
        store.activate("fresh-id")
        assertEquals("fresh-id", device.getString("chatforia.device.id", null))
        assertEquals("new-public", device.getString("chatforia.device.publicKey", null))
        assertEquals("new-private", device.getString("chatforia.device.privateKey", null))
        assertNull(device.getString("recovery.device.id", null))
        assertNull(device.getString("recovery.device.privateKey", null))
    }

    @Test
    fun incompleteStagedIdentityIsNotSilentlyReplaced() {
        val device = prefs("recovery-device-partial")
        device.edit().putString("recovery.device.id", "partial-id").commit()
        assertThrows(IllegalStateException::class.java) {
            RecoveryIdentityStore(device,
                createKeyPair = { "public" to "private" }
            ).stage()
        }
        assertEquals("partial-id", device.getString("recovery.device.id", null))
    }
}
