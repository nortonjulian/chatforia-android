package com.chatforia.android.crypto

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class RemoteKeyBackupRepository(
    private val apiClient: ApiClient
) {
    suspend fun fetchBackup(): RemoteKeyBackupDto? {

        val raw =
            withContext(Dispatchers.IO) {
                apiClient.sendRaw(
                    ApiRequest(
                        path = "auth/keys/backup",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )
            }


        val response =
            apiClient.json.decodeFromString<RemoteKeyBackupResponse>(raw)


        if (!response.hasBackup) return null

        return response.keys
    }

    suspend fun uploadBackup(
        payload: RemoteKeyBackupUploadPayload
    ): RemoteKeyBackupDto? {
        val bodyJson =
            apiClient.json.encodeToString(payload)

        val raw =
            withContext(Dispatchers.IO) {
                apiClient.sendRaw(
                    ApiRequest(
                        path = "auth/keys/backup",
                        method = HttpMethod.POST,
                        bodyJson = bodyJson,
                        requiresAuth = true
                    )
                )
            }


        val response =
            apiClient.json.decodeFromString<RemoteKeyBackupResponse>(raw)


        return response.keys
    }
}

@Serializable
data class RemoteKeyBackupResponse(
    val hasBackup: Boolean = false,
    val keys: RemoteKeyBackupDto? = null
)

@Serializable
data class RemoteKeyBackupDto(
    val publicKey: String? = null,
    val encryptedPrivateKeyBundle: String? = null,
    val privateKeyWrapSalt: String? = null,
    val privateKeyWrapKdf: String? = null,
    val privateKeyWrapIterations: Int? = null,
    val privateKeyWrapVersion: Int? = null
)