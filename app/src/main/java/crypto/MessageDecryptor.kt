package com.chatforia.android.crypto

import android.util.Base64
import com.chatforia.android.messages.MessageDto
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MessageDecryptor {
    private val lazySodium = LazySodiumAndroid(SodiumAndroid())

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun decryptMessageOrNull(
        message: MessageDto,
        currentUserPrivateKeyB64: String?,
        currentUserId: Int
    ): String? {
        val ciphertext = message.contentCiphertext ?: return plaintextFallback(message)

        val encryptedKeyForMe =
            message.encryptedKeyForMe
                ?: message.encryptedKeys?.values?.firstOrNull()
                ?: return null

        if (message.id == 653 || message.id == 654) {
            println("🔐 WRAPPED id=${message.id} = $encryptedKeyForMe")
        }

        if (currentUserPrivateKeyB64.isNullOrBlank()) return null

        return try {

            if (message.id == 653) {
                println(
                    """
            🔐 DEBUG
            id=${message.id}
            sender=${message.sender.id}
            senderPub=${message.sender.publicKey}
            encryptedKeyForMe=${message.encryptedKeyForMe}
            cipher=${message.contentCiphertext}
            version=${message.encryptionVersion}
            """.trimIndent()
                )
            }

            val sessionKey =
                unwrapMessageKey(
                    encryptedKeyJson = encryptedKeyForMe,
                    currentUserPrivateKeyB64 = currentUserPrivateKeyB64,
                    currentUserId = currentUserId
                )

            val result =
                decryptContentCiphertext(
                    ciphertextB64 = ciphertext,
                    sessionKey = sessionKey
                )

            if (message.id == 653) {
                println("🔐 SUCCESS id=653 result=$result")
            }

            result

        } catch (e: Exception) {
            println(
                "❌ Android decrypt failed for message ${message.id}: " +
                        "${e::class.simpleName}: ${e.message}"
            )
            null
        }
    }

    fun decryptMessages(
        messages: List<MessageDto>,
        currentUserPrivateKeyB64: String?,
        currentUserId: Int
    ): List<MessageDto> {
        return messages.map { message ->
            val decrypted =
                decryptMessageOrNull(
                    message = message,
                    currentUserPrivateKeyB64 = currentUserPrivateKeyB64,
                    currentUserId = currentUserId
                )

            if (!decrypted.isNullOrBlank()) {
                message.copy(decryptedContent = decrypted)
            } else {
                message
            }
        }
    }

    private fun plaintextFallback(message: MessageDto): String {
        return message.decryptedContent
            ?: message.translatedForMe
            ?: message.rawContent
            ?: message.content
            ?: ""
    }

    private fun unwrapMessageKey(
        encryptedKeyJson: String,
        currentUserPrivateKeyB64: String,
        currentUserId: Int
    ): ByteArray {
        val wrapped = json.decodeFromString<WrappedMessageKey>(encryptedKeyJson)

        if (wrapped.alg != "x25519-aesgcm") {
            throw IllegalArgumentException("Unsupported wrapped key alg: ${wrapped.alg}")
        }

        val privateKey = decodeB64Any(currentUserPrivateKeyB64)
        val ephemeralPublicKey = decodeB64Any(wrapped.epk)

        val sharedSecret = ByteArray(32)

        val ok =
            lazySodium.cryptoScalarMult(
                sharedSecret,
                privateKey,
                ephemeralPublicKey
            )

        if (!ok) {
            throw IllegalStateException("X25519 scalarMult failed")
        }

        val wrappingKey =
            hkdfSha256(
                inputKeyMaterial = sharedSecret,
                salt = "chatforia-msg-wrap-v1".toByteArray(StandardCharsets.UTF_8),
                info = "user:$currentUserId".toByteArray(StandardCharsets.UTF_8),
                length = 32
            )

        val wrappedCombined = decodeB64Any(wrapped.wrappedKey)

        if (wrappedCombined.size <= 12) {
            throw IllegalArgumentException("wrappedKey too short")
        }

        val iv = wrappedCombined.copyOfRange(0, 12)
        val ciphertextWithTag = wrappedCombined.copyOfRange(12, wrappedCombined.size)

        return aesGcmDecryptRaw(
            key = wrappingKey,
            iv = iv,
            ciphertextWithTag = ciphertextWithTag
        )
    }

    private fun decryptContentCiphertext(
        ciphertextB64: String,
        sessionKey: ByteArray
    ): String {
        val packed = decodeB64Any(ciphertextB64)

        if (packed.size <= 12) {
            throw IllegalArgumentException("message ciphertext too short")
        }

        val iv = packed.copyOfRange(0, 12)
        val ciphertextWithTag = packed.copyOfRange(12, packed.size)

        val plaintext =
            aesGcmDecryptRaw(
                key = sessionKey,
                iv = iv,
                ciphertextWithTag = ciphertextWithTag
            )

        return plaintext.toString(StandardCharsets.UTF_8)
    }

    private fun aesGcmDecryptRaw(
        key: ByteArray,
        iv: ByteArray,
        ciphertextWithTag: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return cipher.doFinal(ciphertextWithTag)
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
data class WrappedMessageKey(
    val alg: String,
    val epk: String,
    val wrappedKey: String
)