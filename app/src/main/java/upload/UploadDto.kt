package com.chatforia.android.upload

import kotlinx.serialization.Serializable

@Serializable
data class UploadImageResponse(
    val ok: Boolean? = null,
    val url: String
)

@Serializable
data class UploadIntentRequest(
    val name: String,
    val size: Int,
    val mimeType: String,
    val sha256: String? = null
)

@Serializable
data class UploadIntentResponse(
    val uploadUrl: String,
    val key: String,
    val publicUrl: String? = null,
    val requiresComplete: Boolean? = null
)

@Serializable
data class UploadCompleteRequest(
    val key: String,
    val name: String,
    val mimeType: String,
    val size: Int,
    val width: Int? = null,
    val height: Int? = null,
    val durationSec: Int? = null,
    val sha256: String? = null
)

@Serializable
data class UploadCompleteResponse(
    val ok: Boolean,
    val file: UploadCompleteFile
)

@Serializable
data class UploadCompleteFile(
    val url: String? = null,
    val key: String? = null
)