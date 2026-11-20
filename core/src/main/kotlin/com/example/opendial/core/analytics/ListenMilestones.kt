package com.example.opendial.core.analytics

/**
 * Tracks listening time in a session and reports each milestone once. Engagement milestones are
 * the events ad networks optimise for once installs are counted, and they are cheap: at most three
 * per session.
 */
class ListenMilestones(private val thresholdsMinutes: List<Int> = listOf(10, 30, 60)) {
    private var listenedMs = 0L
    private var next = 0

    init {
        require(thresholdsMinutes == thresholdsMinutes.sorted()) { "thresholds must be ascending" }
    }

    val totalListenedMs: Long get() = listenedMs

    /** Adds [deltaMs] of playback and returns the milestones crossed by it, in order. */
    fun add(deltaMs: Long): List<Int> {
        if (deltaMs <= 0) return emptyList()
        listenedMs += deltaMs
        val crossed = mutableListOf<Int>()
        while (next < thresholdsMinutes.size && listenedMs >= thresholdsMinutes[next] * 60_000L) {
            crossed += thresholdsMinutes[next]
            next++
        }
        return crossed
    }
}
