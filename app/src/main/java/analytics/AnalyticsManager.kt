package analytics

import android.app.Application
import com.chatforia.android.BuildConfig
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

object AnalyticsManager : AnalyticsTracker {

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
            captureScreenViews = false
            debug = BuildConfig.DEBUG
        }

        PostHogAndroid.setup(application, config)
        isConfigured = true
    }

    override fun identify(userId: Int, properties: Map<String, Any>) {
        if (!isConfigured) return

        PostHog.identify(
            distinctId = userId.toString(),
            userProperties = properties
        )
    }

    override fun capture(event: String, properties: Map<String, Any>) {
        if (!isConfigured) return

        val merged = properties.toMutableMap()
        merged["platform"] = "android"

        PostHog.capture(
            event = event,
            properties = merged
        )
    }

    override fun reset() {
        if (!isConfigured) return

        PostHog.reset()
    }
}