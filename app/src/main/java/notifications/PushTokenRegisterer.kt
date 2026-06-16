package com.chatforia.android.notifications

interface PushTokenRegisterer {
    suspend fun registerCurrentFcmToken()
}