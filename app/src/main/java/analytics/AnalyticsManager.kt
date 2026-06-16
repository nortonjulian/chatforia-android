package com.chatforia.android.analytics

import android.app.Application
import com.chatforia.android.BuildConfig
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

object AnalyticsManager {

    private var isConfigured = false

    fun configure(application: Application) {
        val apiKey = BuildConfig.POSTHOG_API_KEY
        val host = BuildConfig.POSTHOG_HOST.ifBlank {
            "https://us.i.posthog.com"
        }

        if (apiKey.isBlank()) {
            return
        }

        val config = PostHogAndroidConfig(
            apiKey = apiKey,
            host = host
        ).apply {
            captureApplicationLifecycleEvents = true

            // iOS is not manually tracking every screen in your file,
            // so keep Android from creating extra automatic screen events for now.
            captureScreenViews = false

            // Helpful during development, but quiet in release builds.
            debug = BuildConfig.DEBUG
        }

        PostHogAndroid.setup(application, config)
        isConfigured = true
    }

    fun identify(userId: Int, properties: Map<String, Any> = emptyMap()) {
        if (!isConfigured) return

        PostHog.identify(
            distinctId = userId.toString(),
            userProperties = properties
        )
    }

    fun capture(event: String, properties: Map<String, Any> = emptyMap()) {
        if (!isConfigured) return

        val merged = properties.toMutableMap()
        merged["platform"] = "android"

        PostHog.capture(
            event = event,
            properties = merged
        )
    }

    fun reset() {
        if (!isConfigured) return

        PostHog.reset()
    }
}