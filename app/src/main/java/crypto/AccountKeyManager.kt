package com.chatforia.android.crypto

import android.util.Base64
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid

class AccountKeyManager(
    private val keyStorage: KeyStorage
) {
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

    suspend fun ensureLocalKeysExist(
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        val localPublicKey = keyStorage.readPublicKey()
        val hasPrivateKey = keyStorage.hasPrivateKey()

        if (hasPrivateKey && localPublicKey.isNullOrBlank()) {
            throw IllegalStateException(
                "This device has a private key but no public key. Reset encryption."
            )
        }

        if (!serverPublicKey.isNullOrBlank() && !hasPrivateKey) {
            throw IllegalStateException(
                "This device is missing your encryption key. Restore your backup key or reset encryption."
            )
        }

        if (
            hasPrivateKey &&
            !localPublicKey.isNullOrBlank() &&
            !serverPublicKey.isNullOrBlank() &&
            localPublicKey.trim() != serverPublicKey.trim()
        ) {
            throw IllegalStateException(
                "Local encryption key does not match server public key. Restore your backup key or reset encryption."
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

    suspend fun resetAccountEncryption(
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