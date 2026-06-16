package com.chatforia.android.crypto

import com.chatforia.android.messages.MessageDto

interface DisplayMessageDecryptor {
    fun decryptMessageOrNull(
        message: MessageDto,
        currentUserPrivateKeyB64: String?,
        currentUserId: Int
    ): String?
}