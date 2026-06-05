package com.chatforia.android.tenor

import kotlinx.serialization.Serializable

@Serializable
data class TenorGifDto(
    val id: String,
    val kind: String? = null,
    val url: String,
    val thumb: String? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null
) {
    val previewUrl: String?
        get() = thumb ?: url
}