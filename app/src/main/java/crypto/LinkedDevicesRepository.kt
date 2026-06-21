package com.chatforia.android.crypto

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LinkedDeviceDto(
    val id: Int? = null,
    val userId: Int? = null,
    val deviceId: String,
    val name: String? = null,
    val platform: String? = null,
    val publicKey: String? = null,
    val keyAlgorithm: String? = null,
    val keyVersion: Int? = null,
    val isPrimary: Boolean? = null,
    val status: String? = null,
    val pairingStatus: String? = null,
    val createdAt: String? = null,
    val lastSeenAt: String? = null,
    val wrappedAccountKey: String? = null,
    val wrappedAccountKeyAlgo: String? = null,
    val wrappedAccountKeyVer: Int? = null
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
data class DeviceResponse(
    val device: LinkedDeviceDto? = null
)

@Serializable
data class DeviceIdRequest(
    val deviceId: String
)

@Serializable
data class PushTokenRequest(
    val deviceId: String,
    val pushToken: String,
    val pushProvider: String = "fcm"
)

@Serializable
data class DeviceRegisterRequest(
    val deviceId: String,
    val name: String,
    val platform: String,
    val publicKey: String,
    val keyAlgorithm: String = "curve25519",
    val keyVersion: Int = 1
)

@Serializable
data class ApproveDeviceRequest(
    val deviceId: String,
    val wrappedAccountKey: String,
    val wrappedAccountKeyAlgo: String = "x25519-xsalsa20poly1305",
    val wrappedAccountKeyVer: Int = 1
)

class LinkedDevicesRepository(
    private val apiClient: ApiClient
) : LinkedDevicesDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    override fun fetchMine(): List<LinkedDeviceDto> {
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

    override fun fetchPendingPairing(): List<LinkedDeviceDto> {
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

    override fun registerCurrentDevice(request: DeviceRegisterRequest) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/register",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(request),
                requiresAuth = true
            )
        )
    }

    override fun approve(deviceId: String, wrappedAccountKey: String) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/pairing/approve",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    ApproveDeviceRequest(
                        deviceId = deviceId,
                        wrappedAccountKey = wrappedAccountKey
                    )
                ),
                requiresAuth = true
            )
        )
    }

    override fun reject(deviceId: String) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/pairing/reject",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(DeviceIdRequest(deviceId)),
                requiresAuth = true
            )
        )
    }

    override fun revoke(deviceId: String) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/revoke",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(DeviceIdRequest(deviceId)),
                requiresAuth = true
            )
        )
    }

    override fun heartbeat(deviceId: String) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/heartbeat",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(DeviceIdRequest(deviceId)),
                requiresAuth = true
            )
        )
    }

    override fun requestPairing(request: DeviceRegisterRequest) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/pairing/request",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(request),
                requiresAuth = true
            )
        )
    }

    override fun fetchPairingStatus(deviceId: String): LinkedDeviceDto? {
        val response: DeviceResponse =
            apiClient.send(
                ApiRequest(
                    path = "devices/pairing/status/$deviceId",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )

        return response.device
    }

    override fun registerPushToken(
        deviceId: String,
        pushToken: String
    ) {
        apiClient.sendRaw(
            ApiRequest(
                path = "devices/push-token",
                method = HttpMethod.POST,
                bodyJson = json.encodeToString(
                    PushTokenRequest(
                        deviceId = deviceId,
                        pushToken = pushToken,
                        pushProvider = "fcm"
                    )
                ),
                requiresAuth = true
            )
        )
    }
}