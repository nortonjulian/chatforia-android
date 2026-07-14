package com.chatforia.android.billing

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GooglePlayVerifyRequest(
    val purchaseToken: String,
    val allowProviderMigration: Boolean = false
)

@Serializable
data class GooglePlayVerifyResponse(
    val ok: Boolean,
    val plan: String,
    val entitlementPlan: String? = null,
    val status: String? = null,
    val expiresAt: String? = null,
    val acknowledged: Boolean? = null,
    val productId: String? = null,
    val basePlanId: String? = null,
    val autoRenewEnabled: Boolean? = null,
    val grantsAccess: Boolean? = null
)

class GooglePlayBillingRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        explicitNulls = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun verifyPurchase(
        purchaseToken: String,
        allowProviderMigration: Boolean
    ): GooglePlayVerifyResponse {
        require(purchaseToken.isNotBlank()) {
            "Google Play purchase token is missing."
        }

        val requestBody =
            GooglePlayVerifyRequest(
                purchaseToken = purchaseToken,
                allowProviderMigration = allowProviderMigration
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "billing/google-play/verify",
                    method = HttpMethod.POST,
                    bodyJson = json.encodeToString(requestBody),
                    requiresAuth = true
                )
            )
        }
    }
}