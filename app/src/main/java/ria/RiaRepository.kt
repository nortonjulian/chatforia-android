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