package com.chatforia.android.numbers

import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.ApiException
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

class PhoneNumberRepository(
    private val apiClient: ApiTransport
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun getMyNumber(): MyNumberResponse {
        return withContext(Dispatchers.IO) {
            json.decodeFromString<MyNumberResponse>(
                apiClient.sendRaw(
                    ApiRequest(
                        path = "numbers/my",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
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
            json.decodeFromString<NumberPoolResponse>(
                apiClient.sendRaw(
                    ApiRequest(
                        path = path,
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
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
            try {
                json.decodeFromString<LeaseNumberResponse>(
                    apiClient.sendRaw(
                        ApiRequest(
                            path = "numbers/lease",
                            method = HttpMethod.POST,
                            bodyJson = bodyJson,
                            requiresAuth = true
                        )
                    )
                )
            } catch (error: ApiException) {
                if (error.statusCode != 409) {
                    throw error
                }

                val regulatory =
                    runCatching {
                        json.decodeFromString<NumberRegulatoryLeaseResponse>(
                            error.responseBody
                        )
                    }.getOrNull()
                        ?: throw error

                val actionable =
                    regulatory.decision in setOf(
                        "VERIFICATION_REQUIRED",
                        "VERIFICATION_PENDING",
                        "VERIFICATION_REJECTED"
                    )

                if (!actionable) {
                    throw error
                }

                throw NumberRegulatoryLeaseException(regulatory)
            }
        }
    }

    suspend fun releaseNumber() {
        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
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