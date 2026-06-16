package com.chatforia.android.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import java.util.UUID

class DeviceIdentityStorage(context: Context) : DeviceIdentityStore {
    private val sodium = LazySodiumAndroid(SodiumAndroid())

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "chatforia_device_identity",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getOrCreateDeviceId(): String {
        val existing = prefs.getString("chatforia.device.id", null)
        if (!existing.isNullOrBlank()) return existing

        val newId = UUID.randomUUID().toString().lowercase()

        prefs.edit()
            .putString("chatforia.device.id", newId)
            .apply()

        return newId
    }

    override fun getOrCreateKeyPair(): Pair<String, String> {
        val existingPublic = readPublicKey()
        val existingPrivate = readPrivateKey()

        if (!existingPublic.isNullOrBlank() && !existingPrivate.isNullOrBlank()) {
            return existingPublic to existingPrivate
        }

        val publicKey = ByteArray(32)
        val privateKey = ByteArray(32)

        val ok = sodium.cryptoBoxKeypair(publicKey, privateKey)

        if (!ok) {
            throw IllegalStateException("Failed to create device identity keypair")
        }

        val publicKeyB64 = Base64.encodeToString(publicKey, Base64.NO_WRAP)
        val privateKeyB64 = Base64.encodeToString(privateKey, Base64.NO_WRAP)

        prefs.edit()
            .putString("chatforia.device.publicKey", publicKeyB64)
            .putString("chatforia.device.privateKey", privateKeyB64)
            .apply()

        return publicKeyB64 to privateKeyB64
    }

    fun readPublicKey(): String? {
        return prefs.getString("chatforia.device.publicKey", null)
    }

    fun readPrivateKey(): String? {
        return prefs.getString("chatforia.device.privateKey", null)
    }

    fun clear() {
        prefs.edit()
            .remove("chatforia.device.id")
            .remove("chatforia.device.publicKey")
            .remove("chatforia.device.privateKey")
            .apply()
    }
}