package com.chatforia.android

import analytics.AnalyticsManager
import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.chatforia.android.notifications.NotificationCoordinator

object ChatforiaAppState {

    @Volatile
    var isInForeground: Boolean = false
        private set

    internal fun setForeground(isForeground: Boolean) {
        isInForeground = isForeground
    }
}

class ChatforiaApplication :
    Application(),
    Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this)

        AnalyticsManager.configure(this)

        NotificationCoordinator(this)
    }

    override fun onActivityResumed(activity: Activity) {
        ChatforiaAppState.setForeground(true)
    }

    override fun onActivityPaused(activity: Activity) {
        ChatforiaAppState.setForeground(false)
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
