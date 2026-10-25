package com.chatforia.android.ria

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RiaRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    // Server-authoritative, per-member group consent; unrelated to standalone Ria.
    suspend fun loadGroupConsent(roomId: Int): Boolean = withContext(Dispatchers.IO) {
        require(roomId > 0)
        val response: GroupRiaRoomConsent = apiClient.send(
            ApiRequest(path = "chatrooms/$roomId", method = HttpMethod.GET, requiresAuth = true)
        )
        response.me.allowAIBot
    }

    suspend fun saveGroupConsent(roomId: Int, allowed: Boolean): Boolean = withContext(Dispatchers.IO) {
        require(roomId > 0)
        val response: GroupRiaConsentResult = apiClient.send(
            ApiRequest(
                path = "chatrooms/$roomId/ai-opt",
                method = HttpMethod.PATCH,
                bodyJson = json.encodeToString(GroupRiaConsentUpdate(allow = allowed)),
                requiresAuth = true
            )
        )
        check(response.allowAIBot == allowed) { "Could not verify Ria consent update." }
        val confirmed: GroupRiaRoomConsent = apiClient.send(
            ApiRequest(path = "chatrooms/$roomId", method = HttpMethod.GET, requiresAuth = true)
        )
        check(confirmed.me.allowAIBot == allowed) { "Could not verify saved Ria consent." }
        confirmed.me.allowAIBot
    }

    suspend fun chat(
        messages: List<RiaContextMessageDto>,
        memoryEnabled: Boolean,
        filterProfanity: Boolean
    ): String {
        val bodyJson = json.encodeToString(
            RiaChatRequest(
                messages = messages,
                memoryEnabled = memoryEnabled,
                filterProfanity = filterProfanity
            )
        )

        val response: RiaChatResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "ai/chat",
                        method = HttpMethod.POST,
                        bodyJson = bodyJson,
                        requiresAuth = true
                    )
                )
            }

        return response.reply
    }

    suspend fun suggestReplies(
        messages: List<RiaContextMessageDto>,
        draft: String,
        filterProfanity: Boolean
    ): List<String> {
        val bodyJson = json.encodeToString(
            SuggestRepliesRequest(
                messages = messages,
                draft = draft,
                filterProfanity = filterProfanity
            )
        )

        val response: SuggestRepliesResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "ai/suggest-replies",
                        method = HttpMethod.POST,
                        bodyJson = bodyJson,
                        requiresAuth = true
                    )
                )
            }

        return response.suggestions
    }

    suspend fun rewriteText(
        text: String,
        tone: String,
        filterProfanity: Boolean
    ): List<String> {
        val bodyJson = json.encodeToString(
            RewriteTextRequest(
                text = text,
                tone = tone,
                filterProfanity = filterProfanity
            )
        )

        val response: RewriteTextResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "ai/rewrite",
                        method = HttpMethod.POST,
                        bodyJson = bodyJson,
                        requiresAuth = true
                    )
                )
            }

        return response.rewrites
    }
}
@kotlinx.serialization.Serializable
data class GroupRiaRoomConsent(val me: GroupRiaMemberConsent)

@kotlinx.serialization.Serializable
data class GroupRiaMemberConsent(val allowAIBot: Boolean)

@kotlinx.serialization.Serializable
data class GroupRiaConsentUpdate(val allow: Boolean)

@kotlinx.serialization.Serializable
data class GroupRiaConsentResult(val allowAIBot: Boolean)
