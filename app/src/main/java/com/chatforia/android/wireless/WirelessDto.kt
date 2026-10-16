package com.chatforia.android.wireless

import kotlinx.serialization.Serializable

@Serializable
data class WirelessStatusResponse(
    val mode: String? = null,
    val state: String? = null,
    val low: Boolean? = null,
    val exhausted: Boolean? = null,
    val expired: Boolean? = null,
    val source: WirelessStatusSourceDto? = null,
    val queuedPacks: List<WirelessQueuedPackDto> = emptyList(),
    val queuedDataMb: Int? = null,
    val totalAvailableDataMb: Int? = null
)

@Serializable
data class WirelessQueuedPackDto(
    val id: Int? = null,
    val addonKind: String? = null,
    val totalDataMb: Int? = null,
    val remainingDataMb: Int? = null,
    val expiresAt: String? = null
)

@Serializable
data class WirelessStatusSourceDto(
    val type: String? = null,
    val id: Int? = null,
    val addonKind: String? = null,
    val totalDataMb: Int? = null,
    val remainingDataMb: Int? = null,
    val expiresAt: String? = null,
    val daysRemaining: Int? = null
)

@Serializable
data class CurrentEsimResponse(
    val subscriber: EsimSubscriberDto? = null
)

@Serializable
data class EsimSubscriberDto(
    val id: Int? = null,
    val provider: String? = null,
    val providerProfileId: String? = null,
    val iccid: String? = null,
    val iccidHint: String? = null,
    val smdp: String? = null,
    val activationCode: String? = null,
    val lpaUri: String? = null,
    val qrPayload: String? = null,
    val msisdn: String? = null,
    val region: String? = null,
    val status: String? = null,
    val activatedAt: String? = null,
    val suspendedAt: String? = null,
    val expiresAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class EsimReplacementResponse(
    val ok: Boolean = false,
    val replacement: EsimReplacementDto? = null
)

@Serializable
data class EsimReplacementDto(
    val id: String? = null,
    val purchaseId: Int? = null,
    val status: String? = null,
    val oldIccid: String? = null,
    val newIccid: String? = null,
    val remainingDataMb: Int? = null,
    val expiresAt: String? = null,
    val createdAt: String? = null,
    val completedAt: String? = null,
    val installation: EsimReplacementInstallationDto? = null
)

@Serializable
data class EsimReplacementInstallationDto(
    val subscriberId: Int? = null,
    val iccid: String? = null,
    val providerProfileId: String? = null,
    val smdp: String? = null,
    val activationCode: String? = null,
    val lpaUri: String? = null,
    val qrPayload: String? = null
)

@Serializable
data class ReserveEsimRequest(
    val region: String
)

@Serializable
data class ReserveEsimResponse(
    val providerProfileId: String? = null,
    val iccid: String? = null,
    val iccidHint: String? = null,
    val smdp: String? = null,
    val activationCode: String? = null,
    val lpaUri: String? = null,
    val qrPayload: String? = null,
    val region: String? = null
)

@Serializable
data class WirelessCheckoutRequest(
    val product: String,
    val checkoutAttemptId: String,
    val platform: String = "android"
)

@Serializable
data class WirelessCheckoutResponse(
    val url: String? = null,
    val checkoutUrl: String? = null,
    val sessionId: String? = null
) {
    fun resolvedCheckoutUrl(): String? {
        return checkoutUrl
            ?.takeIf { it.isNotBlank() }
            ?: url?.takeIf { it.isNotBlank() }
    }
}

@Serializable
data class WirelessCheckoutStatusResponse(
    val status: String? = null,
    val complete: Boolean = false,
    val paid: Boolean = false,
    val provisioned: Boolean = false,
    val sessionId: String? = null,
    val paymentStatus: String? = null,
    val sessionStatus: String? = null,
    val purchase: WirelessCheckoutPurchaseDto? = null
)

@Serializable
data class WirelessCheckoutPurchaseDto(
    val id: Int? = null,
    val addonKind: String? = null,
    val totalDataMb: Int? = null,
    val remainingDataMb: Int? = null,
    val expiresAt: String? = null
)

data class DataPackOption(
    val product: String,
    val scope: EsimScope,
    val title: String,
    val amountLabel: String,
    val priceLabel: String,
    val description: String,
    val region: String
)

enum class EsimScope(
    val label: String,
    val subtitle: String
) {
    LOCAL(
        label = "Local",
        subtitle = "Coverage for your current country or nearby local use."
    ),
    EUROPE(
        label = "Europe",
        subtitle = "Coverage across supported European destinations."
    ),
    GLOBAL(
        label = "Global",
        subtitle = "Coverage across supported global destinations."
    )
}