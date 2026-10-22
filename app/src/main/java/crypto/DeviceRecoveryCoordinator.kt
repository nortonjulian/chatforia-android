package com.chatforia.android.crypto

/**
 * Commits a replacement device identity only after the server returns a
 * matching registration. A network failure or ambiguous server response
 * leaves the pending identity intact so the request can be retried.
 */
internal fun performRevokedDeviceRecovery(
    oldDeviceId: String,
    stageIdentity: () -> RecoveryDeviceIdentity,
    register: (RecoverDeviceRequest) -> DeviceRegistrationResponse,
    activateIdentity: (String) -> Unit,
    password: String,
    mfaCode: String?
): RecoveryDeviceIdentity {
    val pending = stageIdentity()
    require(pending.deviceId != oldDeviceId) {
        "A revoked device identity cannot be reused"
    }
    val result = register(
        RecoverDeviceRequest(
            oldDeviceId = oldDeviceId,
            newDeviceId = pending.deviceId,
            publicKey = pending.publicKey,
            password = password,
            mfaCode = mfaCode
        )
    )
    val confirmed = result.device
    check(
        confirmed != null &&
            confirmed.deviceId == pending.deviceId &&
            confirmed.publicKey == pending.publicKey
    ) {
        "The server did not confirm the requested recovery identity"
    }
    activateIdentity(pending.deviceId)
    return pending
}
