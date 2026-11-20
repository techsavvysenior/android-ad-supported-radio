package com.example.opendial.core.startup

/**
 * Nothing that serves ads or tracks installs starts before the consent step has finished. This
 * decides, from three signals that arrive in any order and on any thread, what may start:
 *
 * - [onConsentResolved]: Google's consent flow (UMP) finished, and whether ads may be requested.
 * - [onAppsFlyerSessionReady]: AppsFlyer SDK 7 says its session is ready (it no longer starts by itself).
 *
 * Each SDK starts at most once. If consent says ads may not be requested, neither ads nor
 * attribution start this session; the radio still plays.
 */
class StartupGate(
    private val appsFlyerConfigured: Boolean,
    private val metaConfigured: Boolean,
) {
    enum class Action { StartMobileAds, StartAppsFlyer, StartMeta }

    private var consent: Boolean? = null
    private var afSessionReady = false
    private val started = mutableSetOf<Action>()

    @Synchronized
    fun onConsentResolved(canRequestAds: Boolean): List<Action> {
        if (consent == null) consent = canRequestAds
        return pending()
    }

    @Synchronized
    fun onAppsFlyerSessionReady(): List<Action> {
        afSessionReady = true
        return pending()
    }

    @Synchronized
    fun hasStarted(action: Action): Boolean = action in started

    private fun pending(): List<Action> {
        if (consent != true) return emptyList()
        val wanted = buildList {
            add(Action.StartMobileAds)
            if (metaConfigured) add(Action.StartMeta)
            if (appsFlyerConfigured && afSessionReady) add(Action.StartAppsFlyer)
        }
        return wanted.filter { started.add(it) }
    }
}
