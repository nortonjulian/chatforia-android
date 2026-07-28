package com.chatforia.android.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyStorage(
    context: Context,
    private val accountUserId: Int? = null
) : PrivateKeyReader {

    private val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun saveKeyPair(
        publicKey: String,
        privateKey: String
    ) {
        saveKeyPair(
            userId = requireAccountUserId(),
            publicKey = publicKey,
            privateKey = privateKey
        )
    }

    fun saveKeyPair(
        userId: Int,
        publicKey: String,
        privateKey: String
    ) {
        requireValidUserId(userId)

        prefs.edit()
            .putString(publicKeyName(userId), publicKey)
            .putString(privateKeyName(userId), privateKey)
            .apply()
    }

    fun readPublicKey(): String? {
        return readPublicKey(requireAccountUserId())
    }

    fun readPublicKey(userId: Int): String? {
        requireValidUserId(userId)

        return prefs.getString(
            publicKeyName(userId),
            null
        )
    }

    override fun readPrivateKey(): String? {
        return readPrivateKey(requireAccountUserId())
    }

    fun readPrivateKey(userId: Int): String? {
        requireValidUserId(userId)

        return prefs.getString(
            privateKeyName(userId),
            null
        )
    }

    fun hasPrivateKey(): Boolean {
        return hasPrivateKey(requireAccountUserId())
    }

    fun hasPrivateKey(userId: Int): Boolean {
        return !readPrivateKey(userId).isNullOrBlank()
    }

    fun clearKeys() {
        clearKeys(requireAccountUserId())
    }

    fun clearKeys(userId: Int) {
        requireValidUserId(userId)

        prefs.edit()
            .remove(publicKeyName(userId))
            .remove(privateKeyName(userId))
            .apply()
    }

    /**
     * Safely adopts the legacy app-wide key only when its public key
     * exactly matches the authenticated account's server public key.
     *
     * A key belonging to another account is never migrated or removed.
     */
    @Synchronized
    fun migrateLegacyKeysIfMatching(
        userId: Int,
        serverPublicKey: String?
    ): Boolean {
        requireValidUserId(userId)

        val expectedPublicKey =
            serverPublicKey
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return false

        val scopedPublicKey = readPublicKey(userId)
        val scopedPrivateKey = readPrivateKey(userId)

        if (
            !scopedPublicKey.isNullOrBlank() ||
            !scopedPrivateKey.isNullOrBlank()
        ) {
            return false
        }

        val legacyPublicKey =
            prefs.getString(
                LEGACY_PUBLIC_KEY,
                null
            )
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return false

        val legacyPrivateKey =
            prefs.getString(
                LEGACY_PRIVATE_KEY,
                null
            )
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return false

        if (legacyPublicKey != expectedPublicKey) {
            return false
        }

        val saved =
            prefs.edit()
                .putString(
                    publicKeyName(userId),
                    legacyPublicKey
                )
                .putString(
                    privateKeyName(userId),
                    legacyPrivateKey
                )
                .remove(LEGACY_PUBLIC_KEY)
                .remove(LEGACY_PRIVATE_KEY)
                .commit()

        if (!saved) {
            throw IllegalStateException(
                "Failed to migrate this account's secure message key."
            )
        }

        return true
    }

    private fun requireAccountUserId(): Int {
        return accountUserId
            ?.takeIf { it > 0 }
            ?: throw IllegalStateException(
                "Account-scoped key storage requires a valid user ID."
            )
    }

    private fun requireValidUserId(userId: Int) {
        require(userId > 0) {
            "Account user ID must be positive."
        }
    }

    private fun publicKeyName(userId: Int): String {
        return "$SCOPED_PREFIX.$userId.publicKey"
    }

    private fun privateKeyName(userId: Int): String {
        return "$SCOPED_PREFIX.$userId.privateKey"
    }

    companion object {
        private const val PREFERENCES_NAME =
            "chatforia_e2ee_keys"

        private const val SCOPED_PREFIX =
            "chatforia.e2ee.account"

        private const val LEGACY_PUBLIC_KEY =
            "chatforia.e2ee.publicKey"

        private const val LEGACY_PRIVATE_KEY =
            "chatforia.e2ee.privateKey"
    }
}
