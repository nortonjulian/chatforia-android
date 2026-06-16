package analytics

class FakeAnalyticsTracker : AnalyticsTracker {

    data class Event(
        val name: String,
        val properties: Map<String, Any>
    )

    val identifiedUsers = mutableListOf<Int>()
    val identifyProperties = mutableListOf<Map<String, Any>>()
    val events = mutableListOf<Event>()
    var resetCalled = false

    override fun identify(
        userId: Int,
        properties: Map<String, Any>
    ) {
        identifiedUsers.add(userId)
        identifyProperties.add(properties)
    }

    override fun capture(
        event: String,
        properties: Map<String, Any>
    ) {
        events.add(
            Event(
                name = event,
                properties = properties
            )
        )
    }

    override fun reset() {
        resetCalled = true
    }
}