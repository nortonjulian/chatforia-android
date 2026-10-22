package com.chatforia.android.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import java.util.UUID

data class RecoveryDeviceIdentity(val deviceId: String, val publicKey: String)

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

    // Keep the old/revoked identity intact until the server confirms the
    // new registration. A crash during recovery must not lose either keypair.
    @Synchronized
    fun stageRecoveryIdentity(): RecoveryDeviceIdentity {
        val savedId = prefs.getString("recovery.device.id", null)
        val savedPublic = prefs.getString("recovery.device.publicKey", null)
        if (!savedId.isNullOrBlank() && !savedPublic.isNullOrBlank() &&
            !prefs.getString("recovery.device.privateKey", null).isNullOrBlank()
        ) return RecoveryDeviceIdentity(savedId, savedPublic)

        val pub = ByteArray(32)
        val secret = ByteArray(32)
        if (!sodium.cryptoBoxKeypair(pub, secret)) {
            throw IllegalStateException("Could not prepare device recovery")
        }
        val id = UUID.randomUUID().toString().lowercase()
        val publicB64 = Base64.encodeToString(pub, Base64.NO_WRAP)
        val privateB64 = Base64.encodeToString(secret, Base64.NO_WRAP)
        check(prefs.edit()
            .putString("recovery.device.id", id)
            .putString("recovery.device.publicKey", publicB64)
            .putString("recovery.device.privateKey", privateB64)
            .commit()) { "Could not save pending recovery identity" }
        return RecoveryDeviceIdentity(id, publicB64)
    }

    @Synchronized
    fun activateRecoveryIdentity(expectedId: String) {
        val id = prefs.getString("recovery.device.id", null)
        val pub = prefs.getString("recovery.device.publicKey", null)
        val secret = prefs.getString("recovery.device.privateKey", null)
        require(id == expectedId && !pub.isNullOrBlank() && !secret.isNullOrBlank()) {
            "Pending recovery identity was not found"
        }
        check(prefs.edit()
            .putString("chatforia.device.id", id)
            .putString("chatforia.device.publicKey", pub)
            .putString("chatforia.device.privateKey", secret)
            .remove("recovery.device.id")
            .remove("recovery.device.publicKey")
            .remove("recovery.device.privateKey")
            .commit()) { "Could not finish device recovery" }
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