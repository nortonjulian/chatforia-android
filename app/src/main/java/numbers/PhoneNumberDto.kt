package com.chatforia.android.numbers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MyNumberResponse(
    val number: PhoneNumberDto? = null,
    val policy: NumberPolicyDto? = null
)

@Serializable
data class PhoneNumberDto(
    val id: Int? = null,
    val e164: String,
    val status: String? = null,
    val keepLocked: Boolean? = null,
    val holdUntil: String? = null,
    val releaseAfter: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val isoCountry: String? = null,
    val capabilities: JsonObject? = null
)

@Serializable
data class NumberPolicyDto(
    val mode: String? = null,
    val inactivityDays: Int? = null,
    val holdDays: Int? = null,
    val description: String? = null
)

@Serializable
data class NumberPoolResponse(
    val numbers: List<AvailableNumberDto> = emptyList(),
    val provider: String? = null
)

@Serializable
data class AvailableNumberDto(
    val id: Int? = null,
    val e164: String? = null,
    val number: String? = null,
    val areaCode: String? = null,
    val vanity: Boolean? = null,
    val provider: String? = null,
    val source: String? = null,
    val status: String? = null,
    val isoCountry: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val capabilities: JsonObject? = null,
    val forSale: Boolean? = null,
    val isLeasable: Boolean? = null,
    val isPurchasable: Boolean? = null
)

@Serializable
data class LeaseNumberRequest(
    val e164: String,
    val purchaseIntent: Boolean = false
)

@Serializable
data class LeaseNumberResponse(
    val ok: Boolean = false,
    val number: PhoneNumberDto? = null,
    val policy: NumberPolicyDto? = null,
    val error: String? = null
)