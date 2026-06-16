package com.chatforia.android

import analytics.AnalyticsManager
import android.app.Application

class ChatforiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AnalyticsManager.configure(this)
    }
}