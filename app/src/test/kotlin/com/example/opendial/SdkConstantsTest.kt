package com.example.opendial

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.example.opendial.core.analytics.AnalyticsEvent
import com.example.opendial.core.analytics.EventMapping
import com.facebook.appevents.AppEventsConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * :core maps events with plain strings so it can be tested without Android. These tests pin those
 * strings to the SDKs' own constants, so an SDK rename shows up here instead of as silently
 * missing events in a dashboard.
 */
class SdkConstantsTest {
    @Test
    fun `UC-09 AppsFlyer names match the SDK constants`() {
        val played = EventMapping.appsFlyer(AnalyticsEvent.StationPlayed("id", null))
        assertEquals(AFInAppEventType.CONTENT_VIEW, played.name)
        assertTrue(AFInAppEventParameterName.CONTENT_ID in played.params)
        assertTrue(AFInAppEventParameterName.CONTENT_TYPE in played.params)
        assertEquals(AFInAppEventType.ADD_TO_WISH_LIST, EventMapping.appsFlyer(AnalyticsEvent.FavoriteAdded("id")).name)
        assertEquals(AFInAppEventType.AD_VIEW, EventMapping.appsFlyer(AnalyticsEvent.AdShown("banner")).name)
    }

    @Test
    fun `UC-09 Meta names match the SDK constants`() {
        val played = EventMapping.meta(AnalyticsEvent.StationPlayed("id", null))
        assertEquals(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT, played.name)
        assertTrue(AppEventsConstants.EVENT_PARAM_CONTENT_ID in played.params)
        assertTrue(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE in played.params)
        assertEquals(AppEventsConstants.EVENT_NAME_ADDED_TO_WISHLIST, EventMapping.meta(AnalyticsEvent.FavoriteAdded("id")).name)
        val ad = EventMapping.meta(AnalyticsEvent.AdShown("banner"))
        assertEquals(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, ad.name)
        assertTrue(AppEventsConstants.EVENT_PARAM_AD_TYPE in ad.params)
    }
}
