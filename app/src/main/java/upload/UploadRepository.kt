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
    ): UploadImageResponse = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // Match the server's current default upload limit.
        // Revisit together with MAX_FILE_SIZE_BYTES configuration.
        val maxUploadBytes = 10L * 1024L * 1024L

        val bytes = resolver.openInputStream(uri)?.use { input ->
            readBoundedMedia(input, maxUploadBytes)
        } ?: throw Exception("Could not read selected media.")

        if (bytes.isEmpty()) {
            throw Exception("Selected media is empty.")
        }

        val mimeType =
            resolver.getType(uri)
                ?: "application/octet-stream"

        val extension = when (mimeType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            "video/3gpp" -> "3gp"
            else -> android.webkit.MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?.takeIf {
                    it.matches(Regex("[a-zA-Z0-9]{1,8}"))
                }
                ?: "bin"
        }

        val filename =
            "android-upload-${UUID.randomUUID()}.$extension"

        val intent: UploadIntentResponse = apiClient.send(
            com.chatforia.android.network.ApiRequest(
                path = "uploads/intent",
                method = com.chatforia.android.network.HttpMethod.POST,
                bodyJson = json.encodeToString(
                    UploadIntentRequest(
                        name = filename,
                        size = bytes.size,
                        mimeType = mimeType
                    )
                ),
                requiresAuth = true
            )
        )

        apiClient.putBytesToUrl(
            uploadUrl = intent.uploadUrl,
            mimeType = mimeType,
            bytes = bytes
        )

        val complete: UploadCompleteResponse = apiClient.send(
            com.chatforia.android.network.ApiRequest(
                path = "uploads/complete",
                method = com.chatforia.android.network.HttpMethod.POST,
                bodyJson = json.encodeToString(
                    UploadCompleteRequest(
                        key = intent.key,
                        name = filename,
                        mimeType = mimeType,
                        size = bytes.size
                    )
                ),
                requiresAuth = true
            )
        )

        if (!complete.ok) {
            throw Exception("Media upload could not be completed.")
        }

        val canonicalKey =
            complete.file.key
                ?.takeIf { it.isNotBlank() }
                ?: intent.key

        UploadImageResponse(
            ok = true,
            url = canonicalKey
        )
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