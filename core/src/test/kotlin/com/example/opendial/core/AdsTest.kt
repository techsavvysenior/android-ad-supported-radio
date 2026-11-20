package com.example.opendial.core

import com.example.opendial.core.ads.AdFreePass
import com.example.opendial.core.ads.AdUnitConfig
import com.example.opendial.core.ads.InterstitialPolicy
import com.example.opendial.core.ads.InterstitialPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN = 60_000L

class InterstitialPolicyTest {
    private val policy = InterstitialPolicy()
    private val noPass = AdFreePass()

    /** Switches [n] times at [atMs] and returns the last decision. */
    private fun switch(state: InterstitialPolicy.State, atMs: Long, n: Int = 1, pass: AdFreePass = noPass): Pair<InterstitialPolicy.State, Decision> {
        var s = state
        var d = Decision.NotEnoughSwitches
        repeat(n) { policy.onStationSwitch(s, atMs, pass).let { (ns, nd) -> s = ns; d = nd } }
        return s to d
    }

    @Test
    fun `UC-06 no interstitial in the first two minutes of a session`() {
        val (_, d) = switch(policy.start(0), atMs = 1 * MIN + 59_000, n = 5)
        assertEquals(Decision.TooEarlyInSession, d)
    }

    @Test
    fun `UC-06 shows on the third switch once the session is old enough`() {
        val start = policy.start(0)
        val (s2, d2) = switch(start, atMs = 3 * MIN, n = 2)
        assertEquals(Decision.NotEnoughSwitches, d2)
        val (_, d3) = switch(s2, atMs = 3 * MIN)
        assertEquals(Decision.Show, d3)
    }

    @Test
    fun `UC-06 at least five minutes and three switches between interstitials`() {
        var s = switch(policy.start(0), atMs = 3 * MIN, n = 3).first
        s = policy.onShown(s, 3 * MIN)
        val (s1, tooSoon) = switch(s, atMs = 7 * MIN, n = 3)
        assertEquals(Decision.TooSoonSinceLast, tooSoon)
        val (_, ok) = switch(s1, atMs = 8 * MIN)
        assertEquals(Decision.Show, ok)
        val (_, fewSwitches) = switch(policy.onShown(s1, 8 * MIN), atMs = 20 * MIN, n = 2)
        assertEquals(Decision.NotEnoughSwitches, fewSwitches)
    }

    @Test
    fun `UC-06 an ad that was allowed but not shown does not reset the pacing`() {
        val (s, d) = switch(policy.start(0), atMs = 3 * MIN, n = 3)
        assertEquals(Decision.Show, d)
        val (_, next) = switch(s, atMs = 3 * MIN + 10_000)
        assertEquals(Decision.Show, next)
    }

    @Test
    fun `UC-06 UC-07 never during an ad-free pass`() {
        val pass = AdFreePass().grant(nowMs = 0)
        val (_, d) = switch(policy.start(0), atMs = 30 * MIN, n = 10, pass = pass)
        assertEquals(Decision.AdFreePassActive, d)
        val (_, after) = switch(policy.start(0), atMs = 61 * MIN, n = 3, pass = pass)
        assertEquals(Decision.Show, after)
    }
}

class AdFreePassTest {
    @Test
    fun `UC-07 a rewarded ad buys sixty minutes`() {
        val pass = AdFreePass().grant(nowMs = 1_000)
        assertTrue(pass.isActive(1_000))
        assertEquals(60, pass.remainingMinutes(1_000))
        assertTrue(pass.isActive(1_000 + 60 * MIN - 1))
        assertFalse(pass.isActive(1_000 + 60 * MIN))
    }

    @Test
    fun `UC-07 watching again stacks on top, up to three hours ahead`() {
        var pass = AdFreePass()
        repeat(2) { pass = pass.grant(nowMs = 0) }
        assertEquals(120 * MIN, pass.remainingMs(0))
        repeat(5) { pass = pass.grant(nowMs = 0) }
        assertEquals(AdFreePass.MAX_STACK_MS, pass.remainingMs(0))
        assertTrue(pass.isFull(0))
        assertFalse(pass.isFull(1))
    }

    @Test
    fun `UC-07 an expired pass starts fresh from now`() {
        val old = AdFreePass(untilMs = 10 * MIN)
        val fresh = old.grant(nowMs = 100 * MIN)
        assertEquals(160 * MIN, fresh.untilMs)
    }

    @Test
    fun `UC-07 minutes round up so the last minute never reads zero`() {
        val pass = AdFreePass(untilMs = 61_000)
        assertEquals(2, pass.remainingMinutes(0))
        assertEquals(1, pass.remainingMinutes(60_500))
        assertEquals(0, pass.remainingMinutes(61_000))
    }
}

class AdUnitConfigTest {
    @Test
    fun `UC-12 blank properties fall back to Google's demo units, one value at a time`() {
        val all = AdUnitConfig.from(AdUnitConfig.Network.AdMob, "", " ", "", "")
        assertEquals(AdUnitConfig.ADMOB_DEMO, all)
        val gam = AdUnitConfig.from(AdUnitConfig.Network.AdManager, "", "", "", "")
        assertEquals(AdUnitConfig.AD_MANAGER_DEMO, gam)
        val partial = AdUnitConfig.from(AdUnitConfig.Network.AdMob, "", "/1234/radio/home", "", "")
        assertEquals("/1234/radio/home", partial.bannerUnit)
        assertEquals(AdUnitConfig.ADMOB_DEMO.rewardedUnit, partial.rewardedUnit)
    }

    @Test
    fun `UC-12 a release with demo IDs is flagged`() {
        val problems = AdUnitConfig.ADMOB_DEMO.releaseProblems()
        assertTrue(problems.any { "demo app ID" in it })
        assertTrue(problems.any { "demo unit" in it })
    }

    @Test
    fun `UC-12 real AdMob and Ad Manager units pass`() {
        val real = AdUnitConfig(
            appId = "ca-app-pub-1234567890123456~1234567890",
            bannerUnit = "/22912345678/opendial/home_feed",
            interstitialUnit = "ca-app-pub-1234567890123456/2222222222",
            rewardedUnit = "ca-app-pub-1234567890123456/3333333333",
        )
        assertEquals(emptyList<String>(), real.releaseProblems())
    }

    @Test
    fun `UC-12 malformed IDs are flagged`() {
        val bad = AdUnitConfig("my-app", "banner", "ca-app-pub-1/2", "/x")
        val problems = bad.releaseProblems()
        assertTrue(problems.any { "App ID is not" in it })
        assertEquals(3, problems.count { "unit is not" in it })
    }

    @Test
    fun `UC-12 network name parsing defaults to AdMob`() {
        assertEquals(AdUnitConfig.Network.AdManager, AdUnitConfig.parseNetwork(" AdManager "))
        assertEquals(AdUnitConfig.Network.AdMob, AdUnitConfig.parseNetwork("admob"))
        assertEquals(AdUnitConfig.Network.AdMob, AdUnitConfig.parseNetwork(""))
    }
}
