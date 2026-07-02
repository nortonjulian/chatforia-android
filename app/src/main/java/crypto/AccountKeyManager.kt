package com.chatforia.android.crypto

import android.util.Base64
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid

class AccountKeyManager(
    private val keyStorage: KeyStorage
) : AccountKeyService {
    private val sodium = LazySodiumAndroid(SodiumAndroid())

    fun generateNewAccountKeys(): Pair<String, String> {
        val publicKey = ByteArray(32)
        val privateKey = ByteArray(32)

        val ok = sodium.cryptoBoxKeypair(publicKey, privateKey)

        if (!ok) {
            throw IllegalStateException("Failed to generate Curve25519 keypair")
        }

        return Pair(
            Base64.encodeToString(publicKey, Base64.NO_WRAP),
            Base64.encodeToString(privateKey, Base64.NO_WRAP)
        )
    }

    override suspend fun ensureLocalKeysExist(
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        val localPublicKey = keyStorage.readPublicKey()
        val hasPrivateKey = keyStorage.hasPrivateKey()

        if (hasPrivateKey && localPublicKey.isNullOrBlank()) {
            throw IllegalStateException(
                "This device has incomplete secure message information. Start fresh with secure messages only if recovery does not work."
            )
        }

        if (!serverPublicKey.isNullOrBlank() && !hasPrivateKey) {
            throw IllegalStateException(
                "This device is missing your secure message key. Restore your secure message backup or start fresh with secure messages."
            )
        }

        if (
            hasPrivateKey &&
            !localPublicKey.isNullOrBlank() &&
            !serverPublicKey.isNullOrBlank() &&
            localPublicKey.trim() != serverPublicKey.trim()
        ) {
            throw IllegalStateException(
                "The secure message key on this device does not match your account. Restore your secure message backup or start fresh with secure messages."
            )
        }

        if (hasPrivateKey) return

        val (publicKey, privateKey) = generateNewAccountKeys()

        keyStorage.saveKeyPair(
            publicKey = publicKey,
            privateKey = privateKey
        )

        uploadPublicKey(publicKey)
    }

    override suspend fun resetAccountEncryption(
        uploadPublicKey: suspend (String) -> Unit
    ) {
        val (publicKey, privateKey) = generateNewAccountKeys()

        keyStorage.saveKeyPair(
            publicKey = publicKey,
            privateKey = privateKey
        )

        uploadPublicKey(publicKey)
    }
}