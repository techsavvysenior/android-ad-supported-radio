package com.example.opendial.ui

import com.example.opendial.core.ads.AdFreePass
import com.example.opendial.core.stations.Genre
import com.example.opendial.core.stations.Station
import com.example.opendial.playback.PlayerUiState

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data object Failed : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}

data class HomeUiState(
    val greeting: String = "",
    val genre: Genre = Genre.Classical,
    val genreStations: Load<List<Station>> = Load.Loading,
    val localTitle: String? = null,
    val localStations: Load<List<Station>> = Load.Loading,
)

data class SearchUiState(
    val query: String = "",
    val results: Load<List<Station>>? = null,
)

data class AdUiState(
    val pass: AdFreePass = AdFreePass(),
    val nowMs: Long = 0L,
    val rewardedReady: Boolean = false,
    val sdkReady: Boolean = false,
    val bannerUnit: String = "",
    val privacyOptionsRequired: Boolean = false,
) {
    val adFree: Boolean get() = pass.isActive(nowMs)
    val minutesLeft: Int get() = pass.remainingMinutes(nowMs)
    val canBuyMore: Boolean get() = !pass.isFull(nowMs)
}

data class AppUiState(
    val home: HomeUiState = HomeUiState(),
    val search: SearchUiState = SearchUiState(),
    val favorites: List<Station> = emptyList(),
    val player: PlayerUiState = PlayerUiState(),
    val ads: AdUiState = AdUiState(),
) {
    val currentIsFavorite: Boolean get() = player.station?.let { s -> favorites.any { it.id == s.id } } == true
}
