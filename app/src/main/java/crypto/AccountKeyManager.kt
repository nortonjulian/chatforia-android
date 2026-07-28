package com.chatforia.android.crypto

import android.util.Base64
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid

class AccountKeyManager(
    private val keyStorage: KeyStorage
) : AccountKeyService {

    private val sodium =
        LazySodiumAndroid(
            SodiumAndroid()
        )

    fun generateNewAccountKeys(): Pair<String, String> {
        val publicKey = ByteArray(32)
        val privateKey = ByteArray(32)

        val ok =
            sodium.cryptoBoxKeypair(
                publicKey,
                privateKey
            )

        if (!ok) {
            throw IllegalStateException(
                "Failed to generate Curve25519 keypair"
            )
        }

        return Pair(
            Base64.encodeToString(
                publicKey,
                Base64.NO_WRAP
            ),
            Base64.encodeToString(
                privateKey,
                Base64.NO_WRAP
            )
        )
    }

    override suspend fun ensureLocalKeysExist(
        userId: Int,
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        keyStorage.migrateLegacyKeysIfMatching(
            userId = userId,
            serverPublicKey = serverPublicKey
        )

        val localPublicKey =
            keyStorage.readPublicKey(userId)

        val hasPrivateKey =
            keyStorage.hasPrivateKey(userId)

        val hasPublicKey =
            !localPublicKey.isNullOrBlank()

        if (hasPrivateKey != hasPublicKey) {
            throw IllegalStateException(
                "This device has incomplete secure message information. " +
                    "Start fresh with secure messages only if recovery does not work."
            )
        }

        if (
            !serverPublicKey.isNullOrBlank() &&
            !hasPrivateKey
        ) {
            throw IllegalStateException(
                "This device is missing your secure message key. " +
                    "Restore your secure message backup or start fresh with secure messages."
            )
        }

        if (
            hasPrivateKey &&
            !localPublicKey.isNullOrBlank() &&
            !serverPublicKey.isNullOrBlank() &&
            localPublicKey.trim() != serverPublicKey.trim()
        ) {
            throw IllegalStateException(
                "The secure message key on this device does not match your account. " +
                    "Restore your secure message backup or start fresh with secure messages."
            )
        }

        if (hasPrivateKey) {
            val existingPublicKey =
                localPublicKey
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException(
                        "This device has incomplete secure message information."
                    )

            /*
             * A prior first-time upload may have failed after the local key
             * was created. Retry the same local public key instead of
             * generating another account identity.
             */
            if (serverPublicKey.isNullOrBlank()) {
                uploadPublicKey(existingPublicKey)
            }

            return
        }

        val (publicKey, privateKey) =
            generateNewAccountKeys()

        /*
         * Keep this newly generated key locally. If the network upload fails,
         * the next login retries this exact public key instead of generating
         * a different account key.
         */
        keyStorage.saveKeyPair(
            userId = userId,
            publicKey = publicKey,
            privateKey = privateKey
        )

        uploadPublicKey(publicKey)
    }

    override suspend fun resetAccountEncryption(
        userId: Int,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        val (publicKey, privateKey) =
            generateNewAccountKeys()

        /*
         * Do not overwrite the existing local account key until the
         * server has accepted the replacement public key.
         *
         * When the server rejects or fails the request, the existing
         * local key remains untouched.
         */
        uploadPublicKey(publicKey)

        keyStorage.saveKeyPair(
            userId = userId,
            publicKey = publicKey,
            privateKey = privateKey
        )
    }

    @Deprecated(
        message = "A user ID is required for account encryption."
    )
    override suspend fun ensureLocalKeysExist(
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        throw IllegalStateException(
            "A user ID is required for account encryption."
        )
    }

    @Deprecated(
        message = "A user ID is required for account encryption."
    )
    override suspend fun resetAccountEncryption(
        uploadPublicKey: suspend (String) -> Unit
    ) {
        throw IllegalStateException(
            "A user ID is required for account encryption."
        )
    }
}
