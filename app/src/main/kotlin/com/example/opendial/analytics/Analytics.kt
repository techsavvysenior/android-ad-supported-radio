package com.example.opendial.analytics

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.appsflyer.AppsFlyerLib
import com.example.opendial.core.analytics.AnalyticsEvent
import com.example.opendial.core.analytics.EventMapping
import com.example.opendial.core.analytics.PendingEvents
import com.example.opendial.core.analytics.SdkEvent
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

/**
 * One place to log an event. It goes to Logcat always, and to AppsFlyer and Meta once each has
 * been started by the startup gate (after consent). Events logged earlier wait in a small buffer
 * and are sent in order when the SDK starts.
 */
class Analytics(private val app: Application) {
    private val appsFlyerPending = PendingEvents<SdkEvent>()
    private val metaPending = PendingEvents<SdkEvent>()

    @Volatile private var appsFlyerStarted = false
    @Volatile private var metaLogger: AppEventsLogger? = null

    fun log(event: AnalyticsEvent) {
        Log.d(TAG, event.toString())
        val af = EventMapping.appsFlyer(event)
        if (appsFlyerStarted) sendToAppsFlyer(af) else appsFlyerPending.add(af)
        val meta = EventMapping.meta(event)
        metaLogger?.let { sendToMeta(it, meta) } ?: metaPending.add(meta)
    }

    /**
     * AppsFlyer SDK 7: init on every cold start, then start only when the session is ready AND
     * consent allows it. TCF collection is runtime-only in v7, so it is re-applied each launch.
     */
    fun initAppsFlyer(devKey: String, onSessionReady: () -> Unit) {
        val af = AppsFlyerLib.getInstance()
        af.setDebugLog(com.example.opendial.BuildConfig.DEBUG)
        af.enableTCFDataCollection(true)
        af.init(devKey, null, app)
        af.registerSessionReadyListener { onSessionReady() }
    }

    fun startAppsFlyer() {
        AppsFlyerLib.getInstance().start()
        appsFlyerStarted = true
        appsFlyerPending.drain().forEach(::sendToAppsFlyer)
    }

    /** Meta SDK: auto-init and auto-logging are off in the manifest until this runs. */
    fun startMeta(appId: String, clientToken: String) {
        FacebookSdk.setApplicationId(appId)
        FacebookSdk.setClientToken(clientToken)
        @Suppress("DEPRECATION")
        FacebookSdk.sdkInitialize(app)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        FacebookSdk.setAutoLogAppEventsEnabled(true)
        val logger = AppEventsLogger.newLogger(app)
        metaLogger = logger
        metaPending.drain().forEach { sendToMeta(logger, it) }
    }

    private fun sendToAppsFlyer(event: SdkEvent) {
        AppsFlyerLib.getInstance().logEvent(app, event.name, event.params)
    }

    private fun sendToMeta(logger: AppEventsLogger, event: SdkEvent) {
        val params = Bundle()
        for ((key, value) in event.params) {
            when (value) {
                is Int -> params.putInt(key, value)
                is Long -> params.putLong(key, value)
                is Double -> params.putDouble(key, value)
                else -> params.putString(key, value.toString())
            }
        }
        logger.logEvent(event.name, params)
    }

    private companion object {
        const val TAG = "OpenDialEvents"
    }
}
