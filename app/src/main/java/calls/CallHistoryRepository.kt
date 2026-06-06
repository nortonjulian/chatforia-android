package com.chatforia.android.calls

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod

class CallHistoryRepository(
    private val apiClient: ApiClient
) {
    fun fetchCalls(): List<CallDto> {
        val response: CallsResponse =
            apiClient.send(
                ApiRequest(
                    path = "calls/history",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )

        return response.resolvedItems
    }
}