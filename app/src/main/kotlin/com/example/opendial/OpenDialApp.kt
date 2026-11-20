package com.example.opendial

import android.app.Application
import com.example.opendial.ads.Ads
import com.example.opendial.analytics.Analytics
import com.example.opendial.consent.ConsentManager
import com.example.opendial.core.startup.StartupGate
import com.example.opendial.data.Preferences
import com.example.opendial.data.RadioBrowserClient
import com.example.opendial.data.StationRepository
import com.example.opendial.playback.PlayerConnection
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/** Builds the app's few long-lived objects once. No DI framework: the graph is small and explicit. */
class OpenDialApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

class AppGraph(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val repository = StationRepository(
        RadioBrowserClient(http, userAgent = "OpenDial/${BuildConfig.VERSION_NAME} (Android sample app)"),
    )
    val preferences = Preferences(app)
    val player = PlayerConnection(app)
    val consent = ConsentManager(app)
    val ads = Ads(app, scope)
    val analytics = Analytics(app)

    val appsFlyerConfigured = BuildConfig.APPSFLYER_DEV_KEY.isNotBlank()
    val metaConfigured = BuildConfig.META_APP_ID.isNotBlank() && BuildConfig.META_CLIENT_TOKEN.isNotBlank()
    val gate = StartupGate(appsFlyerConfigured, metaConfigured)

    /** Runs whatever the gate allows. Called from the consent result and from AppsFlyer's session callback. */
    fun run(actions: List<StartupGate.Action>) {
        for (action in actions) when (action) {
            StartupGate.Action.StartMobileAds -> ads.start()
            StartupGate.Action.StartAppsFlyer -> analytics.startAppsFlyer()
            StartupGate.Action.StartMeta -> analytics.startMeta(BuildConfig.META_APP_ID, BuildConfig.META_CLIENT_TOKEN)
        }
    }
}
