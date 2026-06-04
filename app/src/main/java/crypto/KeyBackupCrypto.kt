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
import java.security.SecureRandom
import kotlinx.serialization.encodeToString

class KeyBackupCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun createRemoteBackup(
        publicKey: String,
        privateKey: String,
        password: String
    ): RemoteKeyBackupUploadPayload {
        if (publicKey.isBlank() || privateKey.isBlank()) {
            throw IllegalArgumentException("Missing keypair for backup")
        }

        if (password.trim().length < 8) {
            throw IllegalArgumentException("Recovery Passcode must be at least 8 characters.")
        }

        val iterations = 250_000
        val salt = randomBytes(16)
        val iv = randomBytes(12)

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        val aesKey =
            deriveBackupAesKey(
                password = password.trim(),
                saltB64 = saltB64,
                iterations = iterations
            )

        val plaintext =
            json.encodeToString(
                RestoredKeyPair(
                    publicKey = publicKey,
                    privateKey = privateKey
                )
            )

        val ciphertextB64 =
            aesGcmEncrypt(
                key = aesKey,
                iv = iv,
                plaintext = plaintext
            )

        return RemoteKeyBackupUploadPayload(
            publicKey = publicKey,
            encryptedPrivateKeyBundle =
                json.encodeToString(
                    EncryptedPrivateKeyPayload(
                        ivB64 = ivB64,
                        ctB64 = ciphertextB64
                    )
                ),
            privateKeyWrapSalt = saltB64,
            privateKeyWrapKdf = "PBKDF2-SHA256",
            privateKeyWrapIterations = iterations,
            privateKeyWrapVersion = 1
        )
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
                password = password.trim(),
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

    private fun aesGcmEncrypt(
        key: SecretKeySpec,
        iv: ByteArray,
        plaintext: String
    ): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val ciphertext =
            cipher.doFinal(
                plaintext.toByteArray(StandardCharsets.UTF_8)
            )

        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
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
data class RemoteKeyBackupUploadPayload(
    val publicKey: String,
    val encryptedPrivateKeyBundle: String,
    val privateKeyWrapSalt: String,
    val privateKeyWrapKdf: String,
    val privateKeyWrapIterations: Int,
    val privateKeyWrapVersion: Int
)

@Serializable
data class RestoredKeyPair(
    val publicKey: String,
    val privateKey: String
)