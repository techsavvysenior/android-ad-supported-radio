package com.example.opendial.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.opendial.core.ads.AdFreePass
import com.example.opendial.core.stations.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.store by preferencesDataStore(name = "opendial")

/** Favorites (stored whole, so the library works offline) and the ad-free pass. */
class Preferences(private val context: Context) {
    private val favoritesKey = stringPreferencesKey("favorites")
    private val adFreeUntilKey = longPreferencesKey("ad_free_until")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(SavedStation.serializer())

    val favorites: Flow<List<Station>> = context.store.data.map { prefs ->
        prefs[favoritesKey]?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            .orEmpty()
            .map(SavedStation::toStation)
    }

    val adFreePass: Flow<AdFreePass> = context.store.data.map { AdFreePass(it[adFreeUntilKey] ?: 0L) }

    /** Adds the station if it isn't saved, removes it if it is. Returns true when it was added. */
    suspend fun toggleFavorite(station: Station): Boolean {
        var added = false
        context.store.edit { prefs ->
            val current = prefs[favoritesKey]?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()
            val next = if (current.any { it.id == station.id }) {
                current.filterNot { it.id == station.id }
            } else {
                added = true
                listOf(SavedStation.from(station)) + current
            }
            prefs[favoritesKey] = json.encodeToString(serializer, next)
        }
        return added
    }

    suspend fun savePass(pass: AdFreePass) {
        context.store.edit { it[adFreeUntilKey] = pass.untilMs }
    }
}

@Serializable
private data class SavedStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String? = null,
    val tags: List<String> = emptyList(),
    val countryCode: String? = null,
    val codec: String = "",
    val bitrateKbps: Int = 0,
    val isHls: Boolean = false,
) {
    fun toStation() = Station(id, name, streamUrl, faviconUrl, null, tags, countryCode, codec, bitrateKbps, isHls, 0, 0)

    companion object {
        fun from(s: Station) = SavedStation(s.id, s.name, s.streamUrl, s.faviconUrl, s.tags, s.countryCode, s.codec, s.bitrateKbps, s.isHls)
    }
}
