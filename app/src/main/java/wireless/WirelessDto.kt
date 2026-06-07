package com.chatforia.android.wireless

import kotlinx.serialization.Serializable

@Serializable
data class WirelessStatusResponse(
    val mode: String? = null,
    val state: String? = null,
    val low: Boolean? = null,
    val exhausted: Boolean? = null,
    val expired: Boolean? = null,
    val source: WirelessStatusSourceDto? = null
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
    val status: String? = null
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