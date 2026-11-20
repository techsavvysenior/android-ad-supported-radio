package com.example.opendial.core.stations

/**
 * Turns directory entries into stations the app will actually offer:
 * - the last check passed and the stream is HTTPS (Android blocks cleartext, and so should we);
 * - the name is not blank;
 * - one station per stream URL, keeping the one with the most votes;
 * - at most [MAX_TAGS] tags, lower-case and trimmed;
 * - a favicon only if it is HTTPS too.
 * Order is kept as the server sent it (most-clicked first).
 */
object StationMapper {
    const val MAX_TAGS = 4

    fun map(entries: List<RadioBrowserStation>): List<Station> {
        val byUrl = LinkedHashMap<String, RadioBrowserStation>()
        for (entry in entries) {
            if (!entry.lastCheckOk.flag()) continue
            if (entry.stationUuid.isBlank() || entry.name.isBlank()) continue
            val url = entry.urlResolved.trim()
            if (!url.startsWith("https://", ignoreCase = true)) continue
            val existing = byUrl[url]
            if (existing == null || entry.votes > existing.votes) byUrl[url] = entry
        }
        return byUrl.values.map { it.toStation() }
    }

    private fun RadioBrowserStation.toStation() = Station(
        id = stationUuid,
        name = name.trim().replace(Regex("\\s+"), " "),
        streamUrl = urlResolved.trim(),
        faviconUrl = favicon.trim().takeIf { it.startsWith("https://", ignoreCase = true) },
        homepage = homepage.trim().takeIf { it.startsWith("https://", ignoreCase = true) },
        tags = tags.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_TAGS),
        countryCode = countryCode.trim().uppercase().takeIf { it.length == 2 },
        codec = codec.trim().uppercase(),
        bitrateKbps = bitrate.coerceAtLeast(0),
        isHls = hls.flag(),
        votes = votes,
        clicks = clickCount,
    )
}
