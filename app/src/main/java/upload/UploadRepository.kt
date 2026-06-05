package com.chatforia.android.upload

import android.content.Context
import android.net.Uri
import com.chatforia.android.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class UploadRepository(
    private val apiClient: ApiClient,
    private val context: Context
) {
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
}