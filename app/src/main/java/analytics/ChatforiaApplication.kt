package com.chatforia.android

import analytics.AnalyticsManager
import android.app.Application
import com.chatforia.android.notifications.NotificationCoordinator

class ChatforiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AnalyticsManager.configure(this)

        NotificationCoordinator(this)
    }
}