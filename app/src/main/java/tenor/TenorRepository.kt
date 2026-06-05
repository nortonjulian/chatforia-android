package com.chatforia.android.tenor

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class TenorRepository(
    private val apiClient: ApiClient
) {
    suspend fun searchGifs(
        query: String,
        limit: Int = 24
    ): List<TenorGifDto> {
        val encoded =
            URLEncoder.encode(query.trim(), "UTF-8")

        val response: TenorSearchResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "stickers/search?q=$encoded&limit=$limit",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )
            }

        return response.results
    }

    suspend fun trendingGifs(
        limit: Int = 24
    ): List<TenorGifDto> {
        return searchGifs(
            query = "trending",
            limit = limit
        )
    }
}