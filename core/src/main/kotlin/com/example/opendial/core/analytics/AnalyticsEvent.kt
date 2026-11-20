package com.example.opendial.core.analytics

/**
 * Everything the app reports. Deliberately small, and nothing in it identifies a person: no search
 * text, no free-form input, no location beyond what the attribution SDKs collect themselves after
 * consent.
 */
sealed interface AnalyticsEvent {
    /** A station started playing because the listener picked it. */
    data class StationPlayed(val stationId: String, val genre: String?) : AnalyticsEvent

    data class FavoriteAdded(val stationId: String) : AnalyticsEvent

    /** Total listening this session crossed [minutes] (10, 30, 60). See [ListenMilestones]. */
    data class ListenMilestone(val minutes: Int) : AnalyticsEvent

    /** A full-screen or banner ad was seen. [format] is banner, interstitial or rewarded. */
    data class AdShown(val format: String) : AnalyticsEvent

    /** The listener finished a rewarded ad and got [minutes] ad-free. */
    data class AdFreePassGranted(val minutes: Int) : AnalyticsEvent
}

/** An event as one attribution SDK wants it: a name and flat string/number parameters. */
data class SdkEvent(val name: String, val params: Map<String, Any>)

/**
 * Maps app events to AppsFlyer and Meta. Standard events are used wherever one fits, so campaign
 * optimisation in both dashboards can use them without custom mapping:
 *
 * | App event         | AppsFlyer            | Meta                       |
 * |-------------------|----------------------|----------------------------|
 * | StationPlayed     | af_content_view      | fb_mobile_content_view     |
 * | FavoriteAdded     | af_add_to_wishlist   | fb_mobile_add_to_wishlist  |
 * | AdShown           | af_ad_view           | AdImpression               |
 * | ListenMilestone   | listen_milestone     | listen_milestone           |
 * | AdFreePassGranted | ad_free_pass_granted | ad_free_pass_granted       |
 *
 * The names are plain strings here so they can be unit tested on the JVM; an app test checks they
 * still equal the SDKs' own constants.
 */
object EventMapping {
    const val CONTENT_TYPE_STATION = "radio_station"

    fun appsFlyer(event: AnalyticsEvent): SdkEvent = when (event) {
        is AnalyticsEvent.StationPlayed -> SdkEvent(
            "af_content_view",
            buildMap {
                put("af_content_id", event.stationId)
                put("af_content_type", CONTENT_TYPE_STATION)
                event.genre?.let { put("genre", it) }
            },
        )
        is AnalyticsEvent.FavoriteAdded -> SdkEvent(
            "af_add_to_wishlist",
            mapOf("af_content_id" to event.stationId, "af_content_type" to CONTENT_TYPE_STATION),
        )
        is AnalyticsEvent.AdShown -> SdkEvent("af_ad_view", mapOf("af_adrev_ad_type" to event.format))
        is AnalyticsEvent.ListenMilestone -> SdkEvent("listen_milestone", mapOf("minutes" to event.minutes))
        is AnalyticsEvent.AdFreePassGranted -> SdkEvent("ad_free_pass_granted", mapOf("minutes" to event.minutes))
    }

    fun meta(event: AnalyticsEvent): SdkEvent = when (event) {
        is AnalyticsEvent.StationPlayed -> SdkEvent(
            "fb_mobile_content_view",
            buildMap {
                put("fb_content_id", event.stationId)
                put("fb_content_type", CONTENT_TYPE_STATION)
                event.genre?.let { put("genre", it) }
            },
        )
        is AnalyticsEvent.FavoriteAdded -> SdkEvent(
            "fb_mobile_add_to_wishlist",
            mapOf("fb_content_id" to event.stationId, "fb_content_type" to CONTENT_TYPE_STATION),
        )
        is AnalyticsEvent.AdShown -> SdkEvent("AdImpression", mapOf("ad_type" to event.format))
        is AnalyticsEvent.ListenMilestone -> SdkEvent("listen_milestone", mapOf("minutes" to event.minutes))
        is AnalyticsEvent.AdFreePassGranted -> SdkEvent("ad_free_pass_granted", mapOf("minutes" to event.minutes))
    }
}
