package com.chatforia.android.crypto

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LinkedDeviceDto(
    val id: String,
    val name: String? = null,
    val platform: String? = null,
    val publicKey: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val lastSeenAt: String? = null
)

@Serializable
data class LinkedDevicesResponse(
    val items: List<LinkedDeviceDto> = emptyList(),
    val devices: List<LinkedDeviceDto> = emptyList()
) {
    val resolvedItems: List<LinkedDeviceDto>
        get() = if (items.isNotEmpty()) items else devices
}

@Serializable
data class DeviceIdRequest(
    val deviceId: String
)

class LinkedDevicesRepository(
    private val apiClient: ApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun fetchMine(): List<LinkedDeviceDto> {
        val response: LinkedDevicesResponse =
            apiClient.send(
                ApiRequest(
                    path = "devices/mine",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )

        return response.resolvedItems
    }

    fun fetchPendingPairing(): List<LinkedDeviceDto> {
        val response: LinkedDevicesResponse =
            apiClient.send(
                ApiRequest(
                    path = "devices/pairing/pending",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )

        return response.resolvedItems
    }

    fun reject(deviceId: String) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/pairing/reject",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(DeviceIdRequest(deviceId)),
                requiresAuth = true
            )
        )
    }
}