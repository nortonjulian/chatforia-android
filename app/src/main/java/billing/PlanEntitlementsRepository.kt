package com.chatforia.android.billing

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class PlanUsageSnapshot(
    val used: Int,
    val limit: Int,
    val remaining: Int
)

@Serializable
data class AppPlanEntitlementsSnapshot(
    val riaActions: Int,
    val translationChars: Int,
    val hostedParticipantMinutes: Int,
    val smsMessages: Int,
    val pstnMinutes: Int,
    val forwardingMinutes: Int,
    val voicemailTranscriptionMinutes: Int,
    val cloudStorageBytes: Long,
    val messageHistoryDays: Int? = null,
    val adsEnabled: Boolean,
    val aiRewriteLevel: String,
    val supportLevel: String
)

@Serializable
data class PlanEntitlementsResponse(
    val plan: String,
    val entitlements: AppPlanEntitlementsSnapshot,
    val monthKey: String,
    val usage: Map<String, PlanUsageSnapshot>
)

class PlanEntitlementsRepository(
    private val apiClient: ApiClient
) {
    suspend fun fetch(): PlanEntitlementsResponse =
        withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "premium/entitlements",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
}

object PlanEntitlementsStore {
    private val _state =
        MutableStateFlow<PlanEntitlementsResponse?>(null)

    val state: StateFlow<PlanEntitlementsResponse?> =
        _state.asStateFlow()

    suspend fun refresh(
        apiClient: ApiClient
    ): PlanEntitlementsResponse {
        val response =
            PlanEntitlementsRepository(apiClient).fetch()

        _state.value = response
        return response
    }

    fun clear() {
        _state.value = null
    }
}
