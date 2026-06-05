package com.chatforia.android.tenor

import kotlinx.serialization.Serializable

@Serializable
data class TenorSearchResponse(
    val results: List<TenorGifDto> = emptyList()
)