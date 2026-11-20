package com.example.opendial.core.stations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * One entry from the Radio Browser directory (https://www.radio-browser.info), as the
 * `/json/stations/...` endpoints return it. Only the fields the app reads are declared.
 *
 * `hls` and `lastcheckok` arrive as 0 or 1 from the server and as booleans from some mirrors, so
 * they are read as raw primitives and interpreted in [flag].
 */
@Serializable
data class RadioBrowserStation(
    @SerialName("stationuuid") val stationUuid: String = "",
    val name: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    val favicon: String = "",
    val homepage: String = "",
    val tags: String = "",
    @SerialName("countrycode") val countryCode: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val hls: JsonPrimitive = JsonPrimitive(0),
    @SerialName("lastcheckok") val lastCheckOk: JsonPrimitive = JsonPrimitive(0),
    val votes: Int = 0,
    @SerialName("clickcount") val clickCount: Int = 0,
)

internal fun JsonPrimitive.flag(): Boolean = booleanOrNull ?: ((intOrNull ?: 0) != 0)

object RadioBrowserJson {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun decodeStations(body: String): List<RadioBrowserStation> =
        json.decodeFromString<List<RadioBrowserStation>>(body)
}

/**
 * Paths for the Radio Browser API. Broken stations are always hidden server-side, and results come
 * back most-clicked first. The app filters again in [StationMapper], because "hidden broken" is
 * the server's last check, not a guarantee.
 */
object RadioBrowserQuery {
    const val PAGE_SIZE = 60

    fun byGenre(genre: Genre, limit: Int = PAGE_SIZE): String =
        "/json/stations/search?" + params(
            "tag" to genre.tag,
            "tagExact" to "true",
            "is_https" to "true",
            "hidebroken" to "true",
            "order" to "clickcount",
            "reverse" to "true",
            "limit" to limit.toString(),
        )

    fun byName(query: String, limit: Int = PAGE_SIZE): String? {
        val trimmed = query.trim()
        if (trimmed.length < 2) return null
        return "/json/stations/search?" + params(
            "name" to trimmed,
            "is_https" to "true",
            "hidebroken" to "true",
            "order" to "clickcount",
            "reverse" to "true",
            "limit" to limit.toString(),
        )
    }

    fun popularIn(countryCode: String, limit: Int = PAGE_SIZE): String? {
        val code = countryCode.trim().uppercase()
        if (!Regex("[A-Z]{2}").matches(code)) return null
        return "/json/stations/search?" + params(
            "countrycode" to code,
            "is_https" to "true",
            "hidebroken" to "true",
            "order" to "clickcount",
            "reverse" to "true",
            "limit" to limit.toString(),
        )
    }

    /** Counts a play, as the directory asks every client to do when a user starts a station. */
    fun click(stationId: String): String = "/json/url/" + encode(stationId)

    private fun params(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) -> "$k=${encode(v)}" }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}
