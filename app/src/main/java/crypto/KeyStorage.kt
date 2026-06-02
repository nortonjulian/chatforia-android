package com.chatforia.android.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "chatforia_e2ee_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKeyPair(publicKey: String, privateKey: String) {
        prefs.edit()
            .putString("chatforia.e2ee.publicKey", publicKey)
            .putString("chatforia.e2ee.privateKey", privateKey)
            .apply()
    }

    fun readPublicKey(): String? {
        return prefs.getString("chatforia.e2ee.publicKey", null)
    }

    fun readPrivateKey(): String? {
        return prefs.getString("chatforia.e2ee.privateKey", null)
    }

    fun hasPrivateKey(): Boolean {
        return !readPrivateKey().isNullOrBlank()
    }

    fun clearKeys() {
        prefs.edit()
            .remove("chatforia.e2ee.publicKey")
            .remove("chatforia.e2ee.privateKey")
            .apply()
    }
}