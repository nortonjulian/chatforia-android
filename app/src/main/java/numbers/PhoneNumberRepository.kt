package com.chatforia.android.numbers

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

class PhoneNumberRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun getMyNumber(): MyNumberResponse {
        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "numbers/my",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun searchPool(
        areaCode: String,
        country: String,
        capability: String,
        premium: Boolean
    ): NumberPoolResponse {
        val path =
            if (premium) {
                "numbers/pool/buyable?country=${country.encode()}&areaCode=${areaCode.encode()}&capability=${capability.encode()}&limit=20"
            } else {
                "numbers/pool?country=${country.encode()}&areaCode=${areaCode.encode()}&capability=${capability.encode()}&limit=20&forSale=false"
            }

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = path,
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun leaseNumber(
        e164: String,
        premium: Boolean
    ): LeaseNumberResponse {
        val bodyJson =
            json.encodeToString(
                LeaseNumberRequest(
                    e164 = e164,
                    purchaseIntent = premium
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "numbers/lease",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun releaseNumber() {
        withContext(Dispatchers.IO) {
            apiClient.send<Unit>(
                ApiRequest(
                    path = "numbers/release",
                    method = HttpMethod.POST,
                    requiresAuth = true
                )
            )
        }
    }

    private fun String.encode(): String =
        URLEncoder.encode(this, "UTF-8")
}