package com.example.opendial.core.stations

/** A station the app can play. Built only from directory entries that passed [StationMapper]. */
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val homepage: String?,
    val tags: List<String>,
    val countryCode: String?,
    val codec: String,
    val bitrateKbps: Int,
    val isHls: Boolean,
    val votes: Int,
    val clicks: Int,
) {
    /** One line under the name, such as "AAC · 128 kbps · US". Parts that are unknown are left out. */
    val detail: String
        get() = listOfNotNull(
            codec.takeIf { it.isNotBlank() && it != "UNKNOWN" },
            bitrateKbps.takeIf { it > 0 }?.let { "$it kbps" },
            countryCode,
        ).joinToString(" · ")
}

/** The genres on the home screen. [tag] is the Radio Browser tag searched for. */
enum class Genre(val title: String, val tag: String) {
    Classical("Classical", "classical"),
    Jazz("Jazz", "jazz"),
    LoFi("Lo-fi", "lofi"),
    Ambient("Ambient", "ambient"),
    News("News", "news"),
    Pop("Pop", "pop"),
}
