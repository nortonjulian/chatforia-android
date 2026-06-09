package com.chatforia.android.crypto

import android.util.Base64
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class DeviceProvisioningCrypto {
    private val sodium = LazySodiumAndroid(SodiumAndroid())

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun wrapAccountKeyForDevice(
        accountPrivateKeyB64: String,
        targetDevicePublicKeyB64: String
    ): String {
        val ephemeralPublicKey = ByteArray(32)
        val ephemeralPrivateKey = ByteArray(32)

        if (!sodium.cryptoBoxKeypair(ephemeralPublicKey, ephemeralPrivateKey)) {
            throw IllegalStateException("Failed to create provisioning keypair")
        }

        val sharedSecret = deriveSharedSecret(
            privateKey = ephemeralPrivateKey,
            publicKey = decodeB64Any(targetDevicePublicKeyB64)
        )

        val wrappingKey = deriveProvisioningKey(sharedSecret)

        val nonce = ByteArray(24)
        SecureRandom().nextBytes(nonce)

        val plaintext = json.encodeToString(
            ProvisionedAccountKeyPayload(
                privateKey = accountPrivateKeyB64
            )
        ).toByteArray(StandardCharsets.UTF_8)

        val ciphertext = ByteArray(plaintext.size + 16)

        val ok = sodium.cryptoSecretBoxEasy(
            ciphertext,
            plaintext,
            plaintext.size.toLong(),
            nonce,
            wrappingKey
        )

        if (!ok) {
            throw IllegalStateException("Failed to encrypt account key")
        }

        return json.encodeToString(
            WrappedAccountKeyPayload(
                alg = "x25519-xsalsa20poly1305",
                epk = encodeB64(ephemeralPublicKey),
                nonce = encodeB64(nonce),
                ciphertext = encodeB64(ciphertext)
            )
        )
    }

    fun unwrapProvisionedAccountKey(
        wrappedAccountKeyJson: String,
        currentDevicePrivateKeyB64: String
    ): String {
        val wrapped = json.decodeFromString<WrappedAccountKeyPayload>(wrappedAccountKeyJson)

        if (wrapped.alg != "x25519-xsalsa20poly1305") {
            throw IllegalArgumentException("Unsupported provisioning algorithm: ${wrapped.alg}")
        }

        val sharedSecret = deriveSharedSecret(
            privateKey = decodeB64Any(currentDevicePrivateKeyB64),
            publicKey = decodeB64Any(wrapped.epk)
        )

        val wrappingKey = deriveProvisioningKey(sharedSecret)

        val ciphertext = decodeB64Any(wrapped.ciphertext)
        val decrypted = ByteArray(ciphertext.size - 16)

        val ok = sodium.cryptoSecretBoxOpenEasy(
            decrypted,
            ciphertext,
            ciphertext.size.toLong(),
            decodeB64Any(wrapped.nonce),
            wrappingKey
        )

        if (!ok) {
            throw IllegalStateException("Failed to decrypt provisioned account key")
        }

        val payload = json.decodeFromString<ProvisionedAccountKeyPayload>(
            decrypted.toString(StandardCharsets.UTF_8)
        )

        if (payload.privateKey.isBlank()) {
            throw IllegalStateException("Provisioned account key was empty")
        }

        return payload.privateKey
    }

    private fun deriveSharedSecret(
        privateKey: ByteArray,
        publicKey: ByteArray
    ): ByteArray {
        val sharedSecret = ByteArray(32)

        val ok = sodium.cryptoScalarMult(
            sharedSecret,
            privateKey,
            publicKey
        )

        if (!ok) {
            throw IllegalStateException("Device provisioning scalarMult failed")
        }

        return sharedSecret
    }

    private fun deriveProvisioningKey(sharedSecret: ByteArray): ByteArray {
        return hkdfSha256(
            inputKeyMaterial = sharedSecret,
            salt = "chatforia-device-provision-v1".toByteArray(StandardCharsets.UTF_8),
            info = "account-key".toByteArray(StandardCharsets.UTF_8),
            length = 32
        )
    }

    private fun hkdfSha256(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int
    ): ByteArray {
        val prk = hmacSha256(salt, inputKeyMaterial)

        val result = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1

        while (offset < length) {
            val data = previous + info + byteArrayOf(counter.toByte())
            previous = hmacSha256(prk, data)

            val copyLength = minOf(previous.size, length - offset)
            System.arraycopy(previous, 0, result, offset, copyLength)

            offset += copyLength
            counter++
        }

        return result
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun encodeB64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decodeB64Any(value: String): ByteArray {
        var normalized = value.trim()
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
data class WrappedAccountKeyPayload(
    val alg: String,
    val epk: String,
    val nonce: String,
    val ciphertext: String
)

@Serializable
data class ProvisionedAccountKeyPayload(
    val privateKey: String
)