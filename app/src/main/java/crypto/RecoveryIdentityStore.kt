package com.chatforia.android.crypto

import android.content.SharedPreferences
import java.util.UUID

/**
 * Two-phase device-identity enrollment. This store only writes to the
 * device-identity preferences, never to the account's secure-message keys.
 *
 * A staged identity stays available after a failed request or a process
 * restart. Only an explicit, validated server response may commit it.
 */
internal class RecoveryIdentityStore(
    private val preferences: SharedPreferences,
    private val createKeyPair: () -> Pair<String, String>,
    private val createDeviceId: () -> String = {
        UUID.randomUUID().toString().lowercase()
    }
) {
    @Synchronized
    fun stage(): RecoveryDeviceIdentity {
        val existingId = preferences.getString(STAGED_ID, null)
        val existingPublic = preferences.getString(STAGED_PUBLIC, null)
        val existingPrivate = preferences.getString(STAGED_PRIVATE, null)

        val hasAny = listOf(existingId, existingPublic, existingPrivate)
            .any { !it.isNullOrBlank() }
        if (hasAny) {
            check(!existingId.isNullOrBlank() &&
                !existingPublic.isNullOrBlank() &&
                !existingPrivate.isNullOrBlank()) {
                "Pending device recovery information is incomplete"
            }
            return RecoveryDeviceIdentity(existingId, existingPublic)
        }

        val (publicKey, privateKey) = createKeyPair()
        val id = createDeviceId()
        require(id.isNotBlank() && publicKey.isNotBlank() && privateKey.isNotBlank()) {
            "Could not prepare device recovery identity"
        }
        check(preferences.edit()
            .putString(STAGED_ID, id)
            .putString(STAGED_PUBLIC, publicKey)
            .putString(STAGED_PRIVATE, privateKey)
            .commit()) {
            "Could not save pending recovery identity"
        }
        return RecoveryDeviceIdentity(id, publicKey)
    }

    @Synchronized
    fun activate(expectedId: String) {
        val id = preferences.getString(STAGED_ID, null)
        val publicKey = preferences.getString(STAGED_PUBLIC, null)
        val privateKey = preferences.getString(STAGED_PRIVATE, null)
        require(expectedId.isNotBlank() && id == expectedId &&
            !publicKey.isNullOrBlank() && !privateKey.isNullOrBlank()) {
            "Pending recovery identity was not found"
        }
        check(preferences.edit()
            .putString(ACTIVE_ID, id)
            .putString(ACTIVE_PUBLIC, publicKey)
            .putString(ACTIVE_PRIVATE, privateKey)
            .remove(STAGED_ID)
            .remove(STAGED_PUBLIC)
            .remove(STAGED_PRIVATE)
            .commit()) {
            "Could not finish device recovery"
        }
    }

    private companion object {
        const val ACTIVE_ID = "chatforia.device.id"
        const val ACTIVE_PUBLIC = "chatforia.device.publicKey"
        const val ACTIVE_PRIVATE = "chatforia.device.privateKey"
        const val STAGED_ID = "recovery.device.id"
        const val STAGED_PUBLIC = "recovery.device.publicKey"
        const val STAGED_PRIVATE = "recovery.device.privateKey"
    }
}
