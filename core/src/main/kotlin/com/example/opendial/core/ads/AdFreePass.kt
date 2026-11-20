package com.example.opendial.core.ads

/**
 * What a rewarded ad buys: an hour with no banners and no interstitials. Watching another ad while
 * a pass is running adds an hour on top, up to [MAX_STACK_MS] ahead of now, so a listener can bank
 * time without the app handing out unlimited ad-free listening.
 *
 * Stored as one timestamp (`until`), so it survives restarts and needs no clock of its own.
 */
data class AdFreePass(val untilMs: Long = 0L) {

    fun isActive(nowMs: Long): Boolean = nowMs < untilMs

    fun remainingMs(nowMs: Long): Long = (untilMs - nowMs).coerceAtLeast(0L)

    /** Minutes left, rounded up, so the UI never says "0 min" while the pass is still on. */
    fun remainingMinutes(nowMs: Long): Int {
        val ms = remainingMs(nowMs)
        return ((ms + 59_999L) / 60_000L).toInt()
    }

    fun grant(nowMs: Long, rewardMs: Long = REWARD_MS): AdFreePass {
        require(rewardMs > 0) { "rewardMs must be positive" }
        val start = maxOf(untilMs, nowMs)
        val until = minOf(start + rewardMs, nowMs + MAX_STACK_MS)
        return AdFreePass(until)
    }

    /** True when another reward would add nothing, so the "watch an ad" button can be hidden. */
    fun isFull(nowMs: Long): Boolean = remainingMs(nowMs) >= MAX_STACK_MS

    companion object {
        const val REWARD_MS = 60L * 60_000L
        const val MAX_STACK_MS = 3L * 60L * 60_000L
    }
}
