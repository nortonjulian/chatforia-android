package com.chatforia.android.upload

import android.content.Context
import android.net.Uri
import com.chatforia.android.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
class UploadRepository(
    private val apiClient: ApiClient,
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    suspend fun uploadMedia(
        uri: Uri
    ): UploadImageResponse {
        val bytes =
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                } ?: throw Exception("Could not read selected media.")
            }

        val mimeType =
            context.contentResolver.getType(uri)
                ?: "application/octet-stream"

        val filename =
            "android-upload-${UUID.randomUUID()}"

        return withContext(Dispatchers.IO) {
            apiClient.uploadMultipart(
                path = "uploads/image",
                fileFieldName = "file",
                filename = filename,
                mimeType = mimeType,
                bytes = bytes,
                requiresAuth = true
            )
        }
    }

    suspend fun uploadAudio(
        file: java.io.File
    ): UploadImageResponse {
        val mimeType = "audio/m4a"
        val filename = file.name.ifBlank {
            "voice-${UUID.randomUUID()}.m4a"
        }

        val bytes = withContext(Dispatchers.IO) {
            file.readBytes()
        }

        val intentBody = json.encodeToString(
            UploadIntentRequest(
                name = filename,
                size = bytes.size,
                mimeType = mimeType
            )
        )

        val intent: UploadIntentResponse = withContext(Dispatchers.IO) {
            apiClient.send(
                com.chatforia.android.network.ApiRequest(
                    path = "uploads/intent",
                    method = com.chatforia.android.network.HttpMethod.POST,
                    bodyJson = intentBody,
                    requiresAuth = true
                )
            )
        }

        withContext(Dispatchers.IO) {
            apiClient.putBytesToUrl(
                uploadUrl = intent.uploadUrl,
                mimeType = mimeType,
                bytes = bytes
            )
        }

        val completeBody = json.encodeToString(
            UploadCompleteRequest(
                key = intent.key,
                name = filename,
                mimeType = mimeType,
                size = bytes.size
            )
        )

        val complete: UploadCompleteResponse = withContext(Dispatchers.IO) {
            apiClient.send(
                com.chatforia.android.network.ApiRequest(
                    path = "uploads/complete",
                    method = com.chatforia.android.network.HttpMethod.POST,
                    bodyJson = completeBody,
                    requiresAuth = true
                )
            )
        }

        return UploadImageResponse(
            ok = complete.ok,
            url = complete.file.url ?: intent.publicUrl ?: intent.key
        )
    }

    suspend fun uploadAvatar(
        uri: Uri
    ): com.chatforia.android.auth.AvatarResponse {
        val bytes =
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                } ?: throw Exception("Could not read selected image.")
            }

        val mimeType =
            context.contentResolver.getType(uri)
                ?: "image/jpeg"

        val filename =
            "avatar-${UUID.randomUUID()}.jpg"

        return withContext(Dispatchers.IO) {
            apiClient.uploadMultipartTyped(
                path = "users/me/avatar",
                fileFieldName = "avatar",
                filename = filename,
                mimeType = mimeType,
                bytes = bytes,
                requiresAuth = true
            )
        }
    }
}