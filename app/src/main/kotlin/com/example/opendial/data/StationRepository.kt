package com.example.opendial.data

import com.example.opendial.core.stations.Genre
import com.example.opendial.core.stations.RadioBrowserQuery
import com.example.opendial.core.stations.Station
import java.util.concurrent.ConcurrentHashMap

/** Station lists, cached in memory for the session so switching genre tabs is instant. */
class StationRepository(private val client: RadioBrowserClient) {
    private val cache = ConcurrentHashMap<String, List<Station>>()

    suspend fun genre(genre: Genre): List<Station> = cached(RadioBrowserQuery.byGenre(genre))

    suspend fun popularIn(countryCode: String): List<Station> =
        RadioBrowserQuery.popularIn(countryCode)?.let { cached(it) } ?: emptyList()

    suspend fun search(query: String): List<Station> =
        RadioBrowserQuery.byName(query, limit = 40)?.let { client.stations(it) } ?: emptyList()

    suspend fun countPlay(station: Station) = client.countClick(RadioBrowserQuery.click(station.id))

    private suspend fun cached(path: String): List<Station> =
        cache[path] ?: client.stations(path).also { cache[path] = it }
}
