package com.chatforia.android.wireless

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WirelessRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun getWirelessStatus(): WirelessStatusResponse {
        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "wireless/status",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun getCurrentEsim(): CurrentEsimResponse {
        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "esim/me",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun reserveEsim(region: String): ReserveEsimResponse {
        val bodyJson =
            json.encodeToString(
                ReserveEsimRequest(region = region)
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "esim/profiles",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }
}