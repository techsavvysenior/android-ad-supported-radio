package com.example.opendial.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.opendial.BuildConfig
import com.example.opendial.core.ads.AdUnitConfig
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "OpenDialAds"

/**
 * The ad side of the app, on the Google Mobile Ads Next-Gen SDK. One app ID, three units:
 * an inline adaptive banner in the feed, an interstitial at station switches, and a rewarded ad
 * that buys an ad-free hour. AppLovin fills through AdMob mediation when Google doesn't; that is
 * configured in the AdMob console and needs no code beyond the adapter dependency.
 *
 * SDK callbacks arrive on a background thread, so anything that touches UI state is posted to
 * [main].
 */
class Ads(private val context: Context, private val main: CoroutineScope) {

    val units: AdUnitConfig = AdUnitConfig.from(
        network = AdUnitConfig.parseNetwork(BuildConfig.ADS_NETWORK),
        appId = BuildConfig.ADMOB_APP_ID,
        banner = BuildConfig.BANNER_UNIT,
        interstitial = BuildConfig.INTERSTITIAL_UNIT,
        rewarded = BuildConfig.REWARDED_UNIT,
    )

    private val _ready = MutableStateFlow(false)
    /** True once the SDK and the mediation adapters have initialized. Banners wait for this. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var rewarded: RewardedAd? = null
    @Volatile private var loadingInterstitial = false
    @Volatile private var loadingRewarded = false

    init {
        if (!BuildConfig.DEBUG && units.releaseProblems().isNotEmpty()) {
            Log.e(TAG, "Release build with ad config problems: ${units.releaseProblems()}")
        }
    }

    /** Only called through the startup gate, after consent. Initializes off the main thread. */
    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context, InitializationConfig.Builder(units.appId).build()) {
                _ready.value = true
                preloadInterstitial()
                preloadRewarded()
            }
        }
    }

    fun preloadInterstitial() {
        if (!_ready.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            AdRequest.Builder(units.interstitialUnit).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    loadingInterstitial = false
                    Log.w(TAG, "Interstitial failed to load: $adError")
                }
            },
        )
    }

    fun preloadRewarded() {
        if (!_ready.value || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(
            AdRequest.Builder(units.rewardedUnit).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    loadingRewarded = false
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    loadingRewarded = false
                    Log.w(TAG, "Rewarded failed to load: $adError")
                }
            },
        )
    }

    /**
     * Shows the interstitial if one is loaded. [onClosed] always runs exactly once on the main
     * thread: after the ad is dismissed, when it fails to show, or straight away when none is ready.
     * Returns whether an ad is being shown.
     */
    fun showInterstitial(activity: Activity, onShown: () -> Unit, onClosed: () -> Unit): Boolean {
        val ad = interstitial ?: run {
            preloadInterstitial()
            onClosed()
            return false
        }
        interstitial = null
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                main.launch { onShown() }
            }

            override fun onAdDismissedFullScreenContent() {
                main.launch { onClosed() }
                preloadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "Interstitial failed to show: $fullScreenContentError")
                main.launch { onClosed() }
                preloadInterstitial()
            }
        }
        ad.show(activity)
        return true
    }

    /**
     * Shows the rewarded ad. [onReward] runs on the main thread only if the listener earned it;
     * [onClosed] runs when the ad goes away for any reason.
     */
    fun showRewarded(activity: Activity, onShown: () -> Unit, onReward: () -> Unit, onClosed: () -> Unit) {
        val ad = rewarded ?: run {
            preloadRewarded()
            onClosed()
            return
        }
        rewarded = null
        _rewardedReady.value = false
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                main.launch { onShown() }
            }

            override fun onAdDismissedFullScreenContent() {
                main.launch { onClosed() }
                preloadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "Rewarded failed to show: $fullScreenContentError")
                main.launch { onClosed() }
                preloadRewarded()
            }
        }
        ad.show(
            activity,
            object : OnUserEarnedRewardListener {
                override fun onUserEarnedReward(reward: RewardItem) {
                    main.launch { onReward() }
                }
            },
        )
    }
}
