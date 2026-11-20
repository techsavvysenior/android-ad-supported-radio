package com.example.opendial.core

import com.example.opendial.core.analytics.AnalyticsEvent
import com.example.opendial.core.analytics.EventMapping
import com.example.opendial.core.analytics.ListenMilestones
import com.example.opendial.core.analytics.PendingEvents
import com.example.opendial.core.analytics.SdkEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EventMappingTest {
    @Test
    fun `UC-09 a played station is a standard content view in both SDKs`() {
        val event = AnalyticsEvent.StationPlayed("abc", genre = "classical")
        assertEquals(
            SdkEvent("af_content_view", mapOf("af_content_id" to "abc", "af_content_type" to "radio_station", "genre" to "classical")),
            EventMapping.appsFlyer(event),
        )
        assertEquals(
            SdkEvent("fb_mobile_content_view", mapOf("fb_content_id" to "abc", "fb_content_type" to "radio_station", "genre" to "classical")),
            EventMapping.meta(event),
        )
    }

    @Test
    fun `UC-09 genre is left out when the station came from search`() {
        val af = EventMapping.appsFlyer(AnalyticsEvent.StationPlayed("abc", genre = null))
        assertFalse("genre" in af.params)
    }

    @Test
    fun `UC-09 favorites, ad views, milestones and passes map to the agreed names`() {
        val cases = listOf(
            AnalyticsEvent.FavoriteAdded("s1") to ("af_add_to_wishlist" to "fb_mobile_add_to_wishlist"),
            AnalyticsEvent.AdShown("rewarded") to ("af_ad_view" to "AdImpression"),
            AnalyticsEvent.ListenMilestone(30) to ("listen_milestone" to "listen_milestone"),
            AnalyticsEvent.AdFreePassGranted(60) to ("ad_free_pass_granted" to "ad_free_pass_granted"),
        )
        for ((event, names) in cases) {
            assertEquals(names.first, EventMapping.appsFlyer(event).name)
            assertEquals(names.second, EventMapping.meta(event).name)
        }
        assertEquals(mapOf("af_adrev_ad_type" to "rewarded"), EventMapping.appsFlyer(AnalyticsEvent.AdShown("rewarded")).params)
        assertEquals(mapOf("minutes" to 30), EventMapping.meta(AnalyticsEvent.ListenMilestone(30)).params)
    }

    @Test
    fun `UC-09 no parameter carries free text`() {
        val events = listOf(
            AnalyticsEvent.StationPlayed("id", "jazz"),
            AnalyticsEvent.FavoriteAdded("id"),
            AnalyticsEvent.AdShown("banner"),
            AnalyticsEvent.ListenMilestone(10),
            AnalyticsEvent.AdFreePassGranted(60),
        )
        val allowedKeys = setOf(
            "af_content_id", "af_content_type", "fb_content_id", "fb_content_type",
            "genre", "af_adrev_ad_type", "ad_type", "minutes",
        )
        for (e in events) {
            for (sdk in listOf(EventMapping.appsFlyer(e), EventMapping.meta(e))) {
                assertEquals(emptySet<String>(), sdk.params.keys - allowedKeys)
            }
        }
    }
}

class PendingEventsTest {
    @Test
    fun `UC-10 events wait in order and are handed over once`() {
        val pending = PendingEvents<Int>(capacity = 5)
        (1..3).forEach(pending::add)
        assertEquals(listOf(1, 2, 3), pending.drain())
        assertEquals(emptyList<Int>(), pending.drain())
    }

    @Test
    fun `UC-10 the buffer is bounded and drops the oldest`() {
        val pending = PendingEvents<Int>(capacity = 3)
        (1..5).forEach(pending::add)
        assertEquals(3, pending.size)
        assertEquals(2, pending.droppedCount)
        assertEquals(listOf(3, 4, 5), pending.drain())
    }

    @Test
    fun `UC-10 safe to fill from several threads`() {
        val pending = PendingEvents<Int>(capacity = 10_000)
        val threads = (0 until 8).map { t -> Thread { repeat(1000) { pending.add(t * 1000 + it) } } }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)
        assertEquals(8000, pending.drain().toSet().size)
    }
}

class ListenMilestonesTest {
    private val min = 60_000L

    @Test
    fun `UC-11 each milestone is reported once`() {
        val m = ListenMilestones()
        assertEquals(emptyList<Int>(), m.add(9 * min))
        assertEquals(listOf(10), m.add(1 * min))
        assertEquals(emptyList<Int>(), m.add(1 * min))
        assertEquals(listOf(30), m.add(19 * min))
        assertEquals(listOf(60), m.add(30 * min))
        assertEquals(emptyList<Int>(), m.add(500 * min))
    }

    @Test
    fun `UC-11 a long gap can cross several milestones at once, in order`() {
        val m = ListenMilestones()
        assertEquals(listOf(10, 30, 60), m.add(61 * min))
        assertEquals(61 * min, m.totalListenedMs)
    }

    @Test
    fun `UC-11 zero or negative time is ignored`() {
        val m = ListenMilestones()
        assertEquals(emptyList<Int>(), m.add(0))
        assertEquals(emptyList<Int>(), m.add(-5 * min))
        assertEquals(0L, m.totalListenedMs)
    }
}
