package com.chatforia.android.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorage(
    context: Context
) : AuthTokenStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "chatforia_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun save(token: String) {
        prefs.edit()
            .putString("chatforia.auth.token", token)
            .apply()
    }

    override fun read(): String? {
        return prefs.getString("chatforia.auth.token", null)
    }

    override fun clear() {
        prefs.edit()
            .remove("chatforia.auth.token")
            .apply()
    }
}