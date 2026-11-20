package com.example.opendial.data

import com.example.opendial.core.stations.RadioBrowserJson
import com.example.opendial.core.stations.ServerRotation
import com.example.opendial.core.stations.Station
import com.example.opendial.core.stations.StationMapper
import java.io.IOException
import java.net.InetAddress
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Talks to the Radio Browser directory. Mirrors are found the way the directory asks: look up
 * `all.api.radio-browser.info`, reverse-resolve each address to its mirror name, try them in a
 * random order, and fall through to the next on failure. Every request carries a descriptive
 * User-Agent, also as asked.
 */
class RadioBrowserClient(
    private val http: OkHttpClient,
    private val userAgent: String,
    private val resolveMirrors: () -> List<String> = ::lookupMirrors,
    private val random: Random = Random.Default,
) {
    @Volatile private var mirrors: List<String> = emptyList()

    suspend fun stations(path: String): List<Station> = withContext(Dispatchers.IO) {
        StationMapper.map(RadioBrowserJson.decodeStations(get(path)))
    }

    /** Fire and forget: counting a click must never block or break playback. */
    suspend fun countClick(path: String) {
        withContext(Dispatchers.IO) { runCatching { get(path) } }
    }

    private fun get(path: String): String {
        val hosts = mirrors.ifEmpty { ServerRotation.order(resolveMirrors(), random).also { mirrors = it } }
        if (hosts.isEmpty()) throw IOException("No Radio Browser mirror found")
        var last: Exception? = null
        for (host in hosts) {
            try {
                val request = Request.Builder()
                    .url("https://$host$path")
                    .header("User-Agent", userAgent)
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code} from $host")
                    return response.body.string()
                }
            } catch (e: IOException) {
                last = e
            }
        }
        mirrors = emptyList() // look the mirrors up again next time
        throw last ?: IOException("All mirrors failed")
    }

    companion object {
        fun lookupMirrors(): List<String> =
            runCatching { InetAddress.getAllByName("all.api.radio-browser.info").map { it.canonicalHostName } }
                .getOrDefault(emptyList())
    }
}
