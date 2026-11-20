package com.example.opendial.core.analytics

/**
 * Holds events logged before an attribution SDK has started (consent still open, or AppsFlyer's
 * session not ready yet) and hands them over in order once it starts. Bounded, oldest dropped
 * first, so a listener who never answers the consent form can't grow it forever. Thread-safe:
 * SDK callbacks arrive on background threads.
 */
class PendingEvents<T>(private val capacity: Int = 50) {
    private val items = ArrayDeque<T>()
    private var dropped = 0

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    @Synchronized
    fun add(item: T) {
        if (items.size == capacity) {
            items.removeFirst()
            dropped++
        }
        items.addLast(item)
    }

    /** Everything held, oldest first, and empties the buffer. */
    @Synchronized
    fun drain(): List<T> {
        val out = items.toList()
        items.clear()
        return out
    }

    @get:Synchronized
    val droppedCount: Int get() = dropped

    @get:Synchronized
    val size: Int get() = items.size
}
