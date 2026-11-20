package com.example.opendial.core.ads

/**
 * When an interstitial may show. The only trigger is the listener switching stations (a natural
 * break), and even then only if all of these hold:
 * - the session is at least [config].firstAdAfterMs old, so nobody gets an ad on their first tap;
 * - at least [config].minIntervalMs since the last interstitial;
 * - at least [config].switchesBetweenAds station switches since the last one;
 * - no ad-free pass is running.
 * Pure: the app passes in the clock and keeps the returned state.
 */
class InterstitialPolicy(private val config: Config = Config()) {

    data class Config(
        val firstAdAfterMs: Long = 2L * 60_000L,
        val minIntervalMs: Long = 5L * 60_000L,
        val switchesBetweenAds: Int = 3,
    )

    data class State(
        val sessionStartMs: Long,
        val lastShownMs: Long? = null,
        val switchesSinceLast: Int = 0,
    )

    enum class Decision { Show, TooEarlyInSession, TooSoonSinceLast, NotEnoughSwitches, AdFreePassActive }

    fun start(nowMs: Long): State = State(sessionStartMs = nowMs)

    /** Records a station switch and says whether this switch may show an interstitial. */
    fun onStationSwitch(state: State, nowMs: Long, pass: AdFreePass): Pair<State, Decision> {
        val counted = state.copy(switchesSinceLast = state.switchesSinceLast + 1)
        val decision = when {
            pass.isActive(nowMs) -> Decision.AdFreePassActive
            nowMs - state.sessionStartMs < config.firstAdAfterMs -> Decision.TooEarlyInSession
            state.lastShownMs != null && nowMs - state.lastShownMs < config.minIntervalMs ->
                Decision.TooSoonSinceLast
            counted.switchesSinceLast < config.switchesBetweenAds -> Decision.NotEnoughSwitches
            else -> Decision.Show
        }
        return counted to decision
    }

    /** Call when the ad actually showed (not when it was merely allowed), so a missed fill doesn't reset the pacing. */
    fun onShown(state: State, nowMs: Long): State = state.copy(lastShownMs = nowMs, switchesSinceLast = 0)
}
