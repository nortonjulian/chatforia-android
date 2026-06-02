package com.chatforia.android.crypto

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class KeyBackupCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun decryptRemoteBackup(
        backup: RemoteKeyBackupDto,
        password: String
    ): RestoredKeyPair {
        if (password.isBlank()) {
            throw IllegalArgumentException("Backup password is required")
        }

        val encryptedBundle =
            backup.encryptedPrivateKeyBundle
                ?: throw IllegalArgumentException("Backup is missing encrypted key bundle")

        val saltB64 =
            backup.privateKeyWrapSalt
                ?: throw IllegalArgumentException("Backup is missing wrap salt")

        val iterations =
            backup.privateKeyWrapIterations ?: 250_000

        if (backup.privateKeyWrapKdf != null &&
            backup.privateKeyWrapKdf != "PBKDF2-SHA256"
        ) {
            throw IllegalArgumentException("Unsupported backup KDF: ${backup.privateKeyWrapKdf}")
        }

        val encryptedPayload =
            json.decodeFromString<EncryptedPrivateKeyPayload>(encryptedBundle)

        val aesKey =
            deriveBackupAesKey(
                password = password,
                saltB64 = saltB64,
                iterations = iterations
            )

        val plaintext =
            aesGcmDecrypt(
                key = aesKey,
                ivB64 = encryptedPayload.ivB64,
                ciphertextB64 = encryptedPayload.ctB64
            )

        val restored =
            json.decodeFromString<RestoredKeyPair>(plaintext)

        if (restored.publicKey.isBlank() || restored.privateKey.isBlank()) {
            throw IllegalArgumentException("Restored keypair is invalid")
        }

        if (!backup.publicKey.isNullOrBlank() && backup.publicKey.trim() != restored.publicKey.trim()) {
            throw IllegalArgumentException("Backup public key does not match restored keypair")
        }

        return restored
    }

    private fun deriveBackupAesKey(
        password: String,
        saltB64: String,
        iterations: Int
    ): SecretKeySpec {
        val salt = decodeB64Any(saltB64)

        val spec =
            PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                256
            )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val rawKey = factory.generateSecret(spec).encoded

        return SecretKeySpec(rawKey, "AES")
    }

    private fun aesGcmDecrypt(
        key: SecretKeySpec,
        ivB64: String,
        ciphertextB64: String
    ): String {
        val iv = decodeB64Any(ivB64)
        val ciphertextWithTag = decodeB64Any(ciphertextB64)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        val plaintext = cipher.doFinal(ciphertextWithTag)

        return plaintext.toString(StandardCharsets.UTF_8)
    }

    private fun decodeB64Any(value: String): ByteArray {
        var normalized =
            value.trim()
                .replace("\\s".toRegex(), "")
                .replace("-", "+")
                .replace("_", "/")

        val padding = normalized.length % 4
        if (padding != 0) {
            normalized += "=".repeat(4 - padding)
        }

        return Base64.decode(normalized, Base64.DEFAULT)
    }
}

@Serializable
data class EncryptedPrivateKeyPayload(
    val ivB64: String,
    val ctB64: String
)

@Serializable
data class RestoredKeyPair(
    val publicKey: String,
    val privateKey: String
)