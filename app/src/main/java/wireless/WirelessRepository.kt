package com.chatforia.android.wireless

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WirelessRepository(
    private val apiClient: ApiClient
) {
    suspend fun getWirelessStatus(): WirelessStatusResponse {
        return withContext(Dispatchers.IO) {
            apiClient.send(
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
        throw IllegalStateException(
            "Complete checkout on Chatforia.com, then return to the app to install your eSIM."
        )
    }
}