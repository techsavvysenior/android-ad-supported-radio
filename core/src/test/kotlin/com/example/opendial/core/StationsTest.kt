package com.example.opendial.core

import com.example.opendial.core.stations.Genre
import com.example.opendial.core.stations.RadioBrowserJson
import com.example.opendial.core.stations.RadioBrowserQuery
import com.example.opendial.core.stations.ServerRotation
import com.example.opendial.core.stations.StationMapper
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationsTest {
    private val fixture = requireNotNull(javaClass.getResource("/stations.json")).readText()
    private val stations = StationMapper.map(RadioBrowserJson.decodeStations(fixture))

    @Test
    fun `UC-01 keeps only stations whose last check passed and whose stream is HTTPS`() {
        val ids = stations.map { it.id }
        assertEquals(listOf("9617a958-0601-11e8-ae97-52543be04c81", "hls-bool"), ids)
        assertFalse("http-only" in ids)
        assertFalse("broken" in ids)
        assertFalse("no-name" in ids)
    }

    @Test
    fun `UC-01 one station per stream URL, keeping the one with the most votes`() {
        val king = stations.single { it.streamUrl.contains("classicalking") }
        assertEquals("9617a958-0601-11e8-ae97-52543be04c81", king.id)
        assertEquals(1200, king.votes)
    }

    @Test
    fun `UC-01 cleans names, tags, codec and country`() {
        val king = stations.first()
        assertEquals("Classical KING FM", king.name)
        assertEquals(listOf("classical", "seattle", "public radio", "orchestra"), king.tags)
        assertEquals("AAC", king.codec)
        assertEquals("US", king.countryCode)
        assertEquals("AAC · 64 kbps · US", king.detail)
    }

    @Test
    fun `UC-01 reads hls and lastcheckok as numbers or booleans`() {
        val ambient = stations.single { it.id == "hls-bool" }
        assertTrue(ambient.isHls)
        assertFalse(stations.first().isHls)
    }

    @Test
    fun `UC-01 drops insecure favicons and homepages and unknown details`() {
        val ambient = stations.single { it.id == "hls-bool" }
        assertNull(ambient.faviconUrl)
        assertNull(ambient.homepage)
        assertNull(ambient.countryCode)
        assertEquals("", ambient.detail)
        assertEquals("https://www.king.org/favicon.png", stations.first().faviconUrl)
    }

    @Test
    fun `UC-01 genre query asks the server for HTTPS, working, most-clicked stations`() {
        assertEquals(
            "/json/stations/search?tag=lofi&tagExact=true&is_https=true&hidebroken=true&order=clickcount&reverse=true&limit=60",
            RadioBrowserQuery.byGenre(Genre.LoFi),
        )
    }

    @Test
    fun `UC-02 name search is encoded and ignores one-letter queries`() {
        assertEquals(
            "/json/stations/search?name=jazz%20%26%20blues&is_https=true&hidebroken=true&order=clickcount&reverse=true&limit=20",
            RadioBrowserQuery.byName("  jazz & blues ", limit = 20),
        )
        assertNull(RadioBrowserQuery.byName("j"))
        assertNull(RadioBrowserQuery.byName("   "))
    }

    @Test
    fun `UC-02 popular-in-country takes only a two-letter code`() {
        assertEquals(
            "/json/stations/search?countrycode=US&is_https=true&hidebroken=true&order=clickcount&reverse=true&limit=60",
            RadioBrowserQuery.popularIn("us"),
        )
        assertNull(RadioBrowserQuery.popularIn("USA"))
        assertNull(RadioBrowserQuery.popularIn(""))
    }

    @Test
    fun `UC-13 click path counts a play for one station`() {
        assertEquals("/json/url/9617a958-0601-11e8-ae97-52543be04c81", RadioBrowserQuery.click("9617a958-0601-11e8-ae97-52543be04c81"))
    }

    @Test
    fun `UC-13 server rotation keeps directory mirrors only, once each, in random order`() {
        val hosts = listOf("de1.api.radio-browser.info.", "fi1.api.radio-browser.info", "DE1.api.radio-browser.info", "all.api.radio-browser.info", "evil.example.com", "nl1.api.radio-browser.info")
        val a = ServerRotation.order(hosts, Random(1))
        val b = ServerRotation.order(hosts, Random(1))
        assertEquals(a, b)
        assertEquals(setOf("de1.api.radio-browser.info", "fi1.api.radio-browser.info", "nl1.api.radio-browser.info"), a.toSet())
        assertEquals(3, a.size)
        val orders = (1..20).map { ServerRotation.order(hosts, Random(it)) }.toSet()
        assertTrue("different seeds should give different orders", orders.size > 1)
    }
}
