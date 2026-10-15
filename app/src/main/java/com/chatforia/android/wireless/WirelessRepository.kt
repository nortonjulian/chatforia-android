package com.chatforia.android.wireless

import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WirelessRepository(
    private val apiClient: ApiTransport
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private inline fun <reified T> send(
        request: ApiRequest
    ): T {
        return json.decodeFromString(
            apiClient.sendRaw(request)
        )
    }

    suspend fun getWirelessStatus(): WirelessStatusResponse {
        return withContext(Dispatchers.IO) {
            send(
                ApiRequest(
                    path = "api/wireless/status",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun getCurrentEsim(): CurrentEsimResponse {
        return withContext(Dispatchers.IO) {
            send(
                ApiRequest(
                    path = "esim/me",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun startCheckout(
        product: String,
        checkoutAttemptId: String
    ): WirelessCheckoutResponse {
        require(product.isNotBlank()) {
            "A wireless product is required."
        }

        require(checkoutAttemptId.isNotBlank()) {
            "A checkout attempt ID is required."
        }

        val requestBody = WirelessCheckoutRequest(
            product = product,
            checkoutAttemptId = checkoutAttemptId,
            platform = "android"
        )

        return withContext(Dispatchers.IO) {
            send(
                ApiRequest(
                    path = "billing/checkout",
                    method = HttpMethod.POST,
                    bodyJson = json.encodeToString(requestBody),
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun replaceEsim(
        purchaseId: Int,
        idempotencyKey: String
    ): EsimReplacementResponse {
        require(purchaseId > 0) {
            "A valid wireless purchase ID is required."
        }

        val key = idempotencyKey.trim()

        require(key.isNotEmpty() && key.length <= 200) {
            "A valid eSIM replacement idempotency key is required."
        }

        return withContext(Dispatchers.IO) {
            send(
                ApiRequest(
                    path = "api/wireless/esim/$purchaseId/replace",
                    method = HttpMethod.POST,
                    requiresAuth = true,
                    headers = mapOf(
                        "Idempotency-Key" to key
                    )
                )
            )
        }
    }

    suspend fun getCheckoutStatus(
        sessionId: String
    ): WirelessCheckoutStatusResponse {
        require(sessionId.isNotBlank()) {
            "A checkout session ID is required."
        }

        val encodedSessionId =
            URLEncoder.encode(
                sessionId,
                StandardCharsets.UTF_8.toString()
            )

        return withContext(Dispatchers.IO) {
            send(
                ApiRequest(
                    path =
                        "billing/checkout-status" +
                            "?session_id=$encodedSessionId",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun reserveEsim(
        region: String
    ): ReserveEsimResponse {
        throw IllegalStateException(
            "Complete checkout, then return to Chatforia to install your eSIM."
        )
    }
}