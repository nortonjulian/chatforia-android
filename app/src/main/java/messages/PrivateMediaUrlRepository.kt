package com.chatforia.android.messages

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.Environment
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Only local private attachment paths use the new participant-authorized media API.
 * Public provider / R2 / external URLs keep their existing behavior. */
internal fun isLocalPrivateMessageMedia(value: String?): Boolean {
    if (value.isNullOrBlank()) return false
    return Regex("^/?(?:uploads/)?(?:media|thumbs)/[A-Za-z0-9_.-]+$")
        .matches(value.trim())
}

@Serializable
private data class PrivateMediaGrant(
    val url: String,
    val expiresSec: Int = 60
)

/**
 * Request an authenticated short-lived media URL without exposing access
 * tokens to the image/video/audio player. Must not be called on the UI thread.
 */
class PrivateMediaUrlRepository(
    private val api: ApiTransport,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun authorize(
        attachmentId: Int,
        original: String?,
        variant: String = "file"
    ): String? {
        if (original.isNullOrBlank()) return null
        if (!isLocalPrivateMessageMedia(original)) return original
        if (attachmentId <= 0 || variant !in setOf("file", "thumb")) return null

        return withContext(Dispatchers.IO) {
            val response = api.sendRaw(
                ApiRequest(
                    path = "private-media/message/$attachmentId?variant=$variant",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
            val grant = json.decodeFromString<PrivateMediaGrant>(response)
            if (!grant.url.startsWith("/private-media/file?token=")) return@withContext null
            Environment.API_BASE_URL.trimEnd('/') + grant.url
        }
    }
}
