package com.chatforia.android.notifications

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.Serializable

@Serializable
data class BadgeStateDto(
    val unreadConversations: Int = 0,
    val missedCalls: Int = 0,
    val unreadVoicemails: Int = 0,
    val total: Int = 0
)

class BadgeStateRepository(
    private val apiClient: ApiClient
) {
    fun fetchBadgeState(): BadgeStateDto {
        return apiClient.send(
            ApiRequest(
                path = "badge-state",
                method = HttpMethod.GET,
                requiresAuth = true
            )
        )
    }
}
