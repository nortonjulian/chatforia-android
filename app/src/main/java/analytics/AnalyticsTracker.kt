package analytics

interface AnalyticsTracker {
    fun identify(
        userId: Int,
        properties: Map<String, Any> = emptyMap()
    )

    fun capture(
        event: String,
        properties: Map<String, Any> = emptyMap()
    )

    fun reset()
}