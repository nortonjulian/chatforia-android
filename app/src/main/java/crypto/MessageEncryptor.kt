package com.chatforia.android.crypto

import android.util.Base64
import com.chatforia.android.messages.RoomParticipantDto
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MessageEncryptor {
    private val lazySodium = LazySodiumAndroid(SodiumAndroid())
    private val secureRandom = SecureRandom()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encryptMessage(
        plaintext: String,
        participants: List<RoomParticipantDto>
    ): EncryptedMessagePayload {
        val sessionKey = randomBytes(32)

        val contentCiphertext =
            aesGcmEncryptPacked(
                key = sessionKey,
                plaintext = plaintext.toByteArray(StandardCharsets.UTF_8)
            )

        val encryptedKeys =
            participants
                .mapNotNull { participant ->
                    val userId = participant.userId
                    val publicKey = participant.user?.publicKey

                    if (publicKey.isNullOrBlank()) {
                        return@mapNotNull null
                    }

                    userId.toString() to wrapSessionKeyForUser(
                        sessionKey = sessionKey,
                        recipientUserId = userId,
                        recipientPublicKeyB64 = publicKey
                    )
                }
                .toMap()

        if (encryptedKeys.isEmpty()) {
            throw IllegalStateException("No participant public keys available for E2EE.")
        }

        return EncryptedMessagePayload(
            contentCiphertext = contentCiphertext,
            encryptedKeys = encryptedKeys,
            encryptionVersion = 1
        )
    }

    fun encryptForSingleUser(
        plaintext: String,
        recipientUserId: Int,
        recipientPublicKeyB64: String,
        language: String? = null,
        sourceLanguage: String? = null
    ): EncryptedMessagePayloadForUser {
        if (recipientPublicKeyB64.isBlank()) {
            throw IllegalArgumentException("Missing recipient public key.")
        }

        val sessionKey = randomBytes(32)

        val contentCiphertext =
            aesGcmEncryptPacked(
                key = sessionKey,
                plaintext = plaintext.toByteArray(StandardCharsets.UTF_8)
            )

        val encryptedKey =
            wrapSessionKeyForUser(
                sessionKey = sessionKey,
                recipientUserId = recipientUserId,
                recipientPublicKeyB64 = recipientPublicKeyB64
            )

        return EncryptedMessagePayloadForUser(
            contentCiphertext = contentCiphertext,
            encryptedKey = encryptedKey,
            language = language,
            sourceLanguage = sourceLanguage
        )
    }

    private fun wrapSessionKeyForUser(
        sessionKey: ByteArray,
        recipientUserId: Int,
        recipientPublicKeyB64: String
    ): String {
        val ephemeralPublicKey = ByteArray(32)
        val ephemeralPrivateKey = ByteArray(32)

        val ok =
            lazySodium.cryptoBoxKeypair(
                ephemeralPublicKey,
                ephemeralPrivateKey
            )

        if (!ok) {
            throw IllegalStateException("Failed to generate ephemeral keypair.")
        }

        val recipientPublicKey =
            decodeB64Any(recipientPublicKeyB64)

        val sharedSecret = ByteArray(32)

        val scalarOk =
            lazySodium.cryptoScalarMult(
                sharedSecret,
                ephemeralPrivateKey,
                recipientPublicKey
            )

        if (!scalarOk) {
            throw IllegalStateException("X25519 scalarMult failed.")
        }

        val wrappingKey =
            hkdfSha256(
                inputKeyMaterial = sharedSecret,
                salt = "chatforia-msg-wrap-v1".toByteArray(StandardCharsets.UTF_8),
                info = "user:$recipientUserId".toByteArray(StandardCharsets.UTF_8),
                length = 32
            )

        val wrappedKey =
            aesGcmEncryptPackedBytes(
                key = wrappingKey,
                plaintext = sessionKey
            )

        return json.encodeToString(
            WrappedMessageKey(
                alg = "x25519-aesgcm",
                epk = encodeB64(ephemeralPublicKey),
                wrappedKey = wrappedKey
            )
        )
    }

    private fun aesGcmEncryptPacked(
        key: ByteArray,
        plaintext: ByteArray
    ): String {
        return encodeB64(
            aesGcmEncryptPackedRaw(
                key = key,
                plaintext = plaintext
            )
        )
    }

    private fun aesGcmEncryptPackedBytes(
        key: ByteArray,
        plaintext: ByteArray
    ): String {
        return encodeB64(
            aesGcmEncryptPackedRaw(
                key = key,
                plaintext = plaintext
            )
        )
    }

    private fun aesGcmEncryptPackedRaw(
        key: ByteArray,
        plaintext: ByteArray
    ): ByteArray {
        val iv = randomBytes(12)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertextWithTag =
            cipher.doFinal(plaintext)

        return iv + ciphertextWithTag
    }

    private fun hkdfSha256(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int
    ): ByteArray {
        val prk = hmacSha256(key = salt, data = inputKeyMaterial)

        val result = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1

        while (offset < length) {
            val data = previous + info + byteArrayOf(counter.toByte())

            previous = hmacSha256(key = prk, data = data)

            val copyLength = minOf(previous.size, length - offset)
            System.arraycopy(previous, 0, result, offset, copyLength)

            offset += copyLength
            counter++
        }

        return result
    }

    private fun hmacSha256(
        key: ByteArray,
        data: ByteArray
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun randomBytes(size: Int): ByteArray {
        return ByteArray(size).also {
            secureRandom.nextBytes(it)
        }
    }

    private fun encodeB64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
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
data class EncryptedMessagePayload(
    val contentCiphertext: String,
    val encryptedKeys: Map<String, String>,
    val encryptionVersion: Int
)

@Serializable
data class EncryptedMessagePayloadForUser(
    val contentCiphertext: String,
    val encryptedKey: String,
    val language: String? = null,
    val sourceLanguage: String? = null
)