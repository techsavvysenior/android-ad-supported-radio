package com.example.opendial.core

import com.example.opendial.core.startup.StartupGate
import com.example.opendial.core.startup.StartupGate.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupGateTest {
    @Test
    fun `UC-04 nothing starts before consent is resolved`() {
        val gate = StartupGate(appsFlyerConfigured = true, metaConfigured = true)
        assertEquals(emptyList<Action>(), gate.onAppsFlyerSessionReady())
    }

    @Test
    fun `UC-04 consent first, then AppsFlyer when its session is ready`() {
        val gate = StartupGate(appsFlyerConfigured = true, metaConfigured = true)
        assertEquals(listOf(Action.StartMobileAds, Action.StartMeta), gate.onConsentResolved(canRequestAds = true))
        assertEquals(listOf(Action.StartAppsFlyer), gate.onAppsFlyerSessionReady())
    }

    @Test
    fun `UC-04 AppsFlyer session ready first, then consent`() {
        val gate = StartupGate(appsFlyerConfigured = true, metaConfigured = false)
        assertEquals(emptyList<Action>(), gate.onAppsFlyerSessionReady())
        assertEquals(listOf(Action.StartMobileAds, Action.StartAppsFlyer), gate.onConsentResolved(canRequestAds = true))
    }

    @Test
    fun `UC-04 each SDK starts once however often the signals repeat`() {
        val gate = StartupGate(appsFlyerConfigured = true, metaConfigured = true)
        val all = mutableListOf<Action>()
        repeat(3) {
            all += gate.onConsentResolved(true)
            all += gate.onAppsFlyerSessionReady()
        }
        assertEquals(listOf(Action.StartMobileAds, Action.StartMeta, Action.StartAppsFlyer), all)
        assertTrue(gate.hasStarted(Action.StartAppsFlyer))
    }

    @Test
    fun `UC-04 when ads may not be requested nothing starts this session`() {
        val gate = StartupGate(appsFlyerConfigured = true, metaConfigured = true)
        assertEquals(emptyList<Action>(), gate.onConsentResolved(canRequestAds = false))
        assertEquals(emptyList<Action>(), gate.onAppsFlyerSessionReady())
        assertEquals(emptyList<Action>(), gate.onConsentResolved(canRequestAds = true))
    }

    @Test
    fun `UC-12 unconfigured attribution SDKs never start`() {
        val gate = StartupGate(appsFlyerConfigured = false, metaConfigured = false)
        assertEquals(listOf(Action.StartMobileAds), gate.onConsentResolved(true))
        assertEquals(emptyList<Action>(), gate.onAppsFlyerSessionReady())
    }
}
