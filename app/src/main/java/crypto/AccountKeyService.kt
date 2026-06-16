package com.chatforia.android.crypto

interface AccountKeyService {
    suspend fun ensureLocalKeysExist(
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    )

    suspend fun resetAccountEncryption(
        uploadPublicKey: suspend (String) -> Unit
    )
}