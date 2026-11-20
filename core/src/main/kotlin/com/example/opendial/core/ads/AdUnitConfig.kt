package com.example.opendial.core.ads

/**
 * Which app ID and ad units the build uses. Blank Gradle properties mean Google's public demo
 * IDs, so the sample runs without an account and can never earn (or get an account banned) by
 * accident. [releaseProblems] lists what would be wrong with shipping this config.
 */
data class AdUnitConfig(
    val appId: String,
    val bannerUnit: String,
    val interstitialUnit: String,
    val rewardedUnit: String,
) {
    val usesDemoUnits: Boolean
        get() = listOf(bannerUnit, interstitialUnit, rewardedUnit).any { it in DEMO_UNITS }

    val usesDemoAppId: Boolean get() = appId == DEMO_APP_ID

    fun releaseProblems(): List<String> = buildList {
        if (usesDemoAppId) add("App ID is Google's demo app ID")
        if (usesDemoUnits) add("At least one ad unit is a Google demo unit")
        if (!APP_ID.matches(appId)) add("App ID is not in the ca-app-pub-<publisher>~<app> form")
        for ((name, unit) in listOf("banner" to bannerUnit, "interstitial" to interstitialUnit, "rewarded" to rewardedUnit)) {
            if (!ADMOB_UNIT.matches(unit) && !AD_MANAGER_UNIT.matches(unit)) add("The $name unit is not an AdMob or Ad Manager unit ID")
        }
    }

    enum class Network { AdMob, AdManager }

    companion object {
        /** Google's sample AdMob app ID. Ads requested with it are always test ads. */
        const val DEMO_APP_ID = "ca-app-pub-3940256099942544~3347511713"

        /** Google's demo units for the GMA Next-Gen SDK (AdMob) and Ad Manager. */
        val ADMOB_DEMO = AdUnitConfig(
            appId = DEMO_APP_ID,
            bannerUnit = "ca-app-pub-3940256099942544/9214589741",
            interstitialUnit = "ca-app-pub-3940256099942544/1033173712",
            rewardedUnit = "ca-app-pub-3940256099942544/5224354917",
        )
        val AD_MANAGER_DEMO = AdUnitConfig(
            appId = DEMO_APP_ID,
            bannerUnit = "/21775744923/example/adaptive-banner",
            interstitialUnit = "/21775744923/example/interstitial",
            rewardedUnit = "/21775744923/example/rewarded",
        )
        private val DEMO_UNITS = setOf(
            ADMOB_DEMO.bannerUnit, ADMOB_DEMO.interstitialUnit, ADMOB_DEMO.rewardedUnit,
            AD_MANAGER_DEMO.bannerUnit, AD_MANAGER_DEMO.interstitialUnit, AD_MANAGER_DEMO.rewardedUnit,
        )
        private val APP_ID = Regex("ca-app-pub-\\d{16}~\\d{10}")
        private val ADMOB_UNIT = Regex("ca-app-pub-\\d{16}/\\d{10}")
        private val AD_MANAGER_UNIT = Regex("/\\d+(/[A-Za-z0-9_.\\-]+)+")

        /**
         * Builds the config from the Gradle properties. Any blank value falls back to the demo set for
         * [network], one value at a time, so a partly configured build still runs.
         */
        fun from(network: Network, appId: String, banner: String, interstitial: String, rewarded: String): AdUnitConfig {
            val demo = if (network == Network.AdManager) AD_MANAGER_DEMO else ADMOB_DEMO
            return AdUnitConfig(
                appId = appId.trim().ifEmpty { demo.appId },
                bannerUnit = banner.trim().ifEmpty { demo.bannerUnit },
                interstitialUnit = interstitial.trim().ifEmpty { demo.interstitialUnit },
                rewardedUnit = rewarded.trim().ifEmpty { demo.rewardedUnit },
            )
        }

        fun parseNetwork(value: String): Network =
            if (value.trim().equals("admanager", ignoreCase = true)) Network.AdManager else Network.AdMob
    }
}
