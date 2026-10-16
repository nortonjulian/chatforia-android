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

    fun deleteCall(callId: Int) {
        apiClient.sendRaw(
            ApiRequest(
                path = "calls/$callId",
                method = HttpMethod.DELETE,
                requiresAuth = true
            )
        )
    }

    fun deleteSelected(callIds: Collection<Int>) {
        val ids =
            callIds
                .filter { it > 0 }
                .distinct()

        if (ids.isEmpty()) return

        val body =
            """{"ids":[${ids.joinToString(",") }]}"""

        apiClient.sendRaw(
            ApiRequest(
                path = "calls/history/delete-selected",
                method = HttpMethod.POST,
                bodyJson = body,
                requiresAuth = true
            )
        )
    }

    fun clearHistory() {
        apiClient.sendRaw(
            ApiRequest(
                path = "calls/history",
                method = HttpMethod.DELETE,
                requiresAuth = true
            )
        )
    }

    fun acknowledgeMissedCalls() {
        apiClient.sendRaw(
            ApiRequest(
                path = "calls/missed/acknowledge",
                method = HttpMethod.PATCH,
                requiresAuth = true
            )
        )
    }
}