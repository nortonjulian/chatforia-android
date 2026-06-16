package com.chatforia.android.crypto

interface LinkedDevicesDataSource {
    fun fetchMine(): List<LinkedDeviceDto>

    fun fetchPendingPairing(): List<LinkedDeviceDto>

    fun registerCurrentDevice(
        request: DeviceRegisterRequest
    )

    fun approve(
        deviceId: String,
        wrappedAccountKey: String
    )

    fun reject(
        deviceId: String
    )

    fun revoke(
        deviceId: String
    )

    fun heartbeat(
        deviceId: String
    )

    fun requestPairing(
        request: DeviceRegisterRequest
    )

    fun fetchPairingStatus(
        deviceId: String
    ): LinkedDeviceDto?

    fun registerPushToken(
        deviceId: String,
        pushToken: String
    )
}