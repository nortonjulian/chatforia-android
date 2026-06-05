package com.chatforia.android.upload

import kotlinx.serialization.Serializable

@Serializable
data class UploadImageResponse(
    val ok: Boolean? = null,
    val url: String
)