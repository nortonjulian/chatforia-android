package com.chatforia.android.crypto

interface AccountKeyService {

    /**
     * Account-aware production entry point.
     *
     * The default implementation preserves compatibility with existing
     * test fakes that implement the older method signature.
     */
    suspend fun ensureLocalKeysExist(
        userId: Int,
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        ensureLocalKeysExist(
            serverPublicKey = serverPublicKey,
            uploadPublicKey = uploadPublicKey
        )
    }

    suspend fun resetAccountEncryption(
        userId: Int,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        resetAccountEncryption(
            uploadPublicKey = uploadPublicKey
        )
    }

    @Deprecated(
        message = "Use the account-aware overload with userId."
    )
    suspend fun ensureLocalKeysExist(
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    )

    @Deprecated(
        message = "Use the account-aware overload with userId."
    )
    suspend fun resetAccountEncryption(
        uploadPublicKey: suspend (String) -> Unit
    )
}
