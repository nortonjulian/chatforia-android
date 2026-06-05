package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class QueuedMessageJob(
    val clientMessageId: String,
    val roomId: Int,
    val text: String? = null,
    val attachmentsInline: List<AttachmentDto> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastAttemptAtMillis: Long? = null,
    val state: QueuedMessageState = QueuedMessageState.PENDING
)

@Serializable
enum class QueuedMessageState {
    PENDING,
    SENDING,
    RETRYING,
    FAILED
}