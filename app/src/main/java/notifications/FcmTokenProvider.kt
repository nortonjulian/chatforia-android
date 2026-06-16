package com.chatforia.android.notifications

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

interface FcmTokenProvider {
    suspend fun currentToken(): String
}

class FirebaseFcmTokenProvider : FcmTokenProvider {
    override suspend fun currentToken(): String {
        return FirebaseMessaging.getInstance().token.await()
    }
}