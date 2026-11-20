package com.example.opendial.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.opendial.OpenDialApp
import com.example.opendial.core.ads.AdFreePass
import com.example.opendial.core.ads.InterstitialPolicy
import com.example.opendial.core.analytics.AnalyticsEvent
import com.example.opendial.core.analytics.ListenMilestones
import com.example.opendial.core.stations.Genre
import com.example.opendial.core.stations.Station
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The app's single screen-level state holder. Pacing decisions (when an interstitial may show,
 * what the ad-free pass allows, which milestones to report) come from :core; this class only wires
 * them to playback, the ad SDK and storage.
 */
class OpenDialViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = (app as OpenDialApp).graph
    private val clock: () -> Long = System::currentTimeMillis

    private val _state = MutableStateFlow(
        AppUiState(
            home = HomeUiState(greeting = greeting(LocalTime.now().hour), localTitle = localTitle()),
            ads = AdUiState(bannerUnit = graph.ads.units.bannerUnit),
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val policy = InterstitialPolicy()
    private var pacing = policy.start(clock())
    private val milestones = ListenMilestones()
    private var searchJob: Job? = null

    init {
        graph.player.connect()
        loadGenre(Genre.Classical)
        loadLocal()
        viewModelScope.launch { graph.player.state.collect { p -> _state.update { it.copy(player = p) } } }
        viewModelScope.launch { graph.preferences.favorites.collect { f -> _state.update { it.copy(favorites = f) } } }
        viewModelScope.launch { graph.preferences.adFreePass.collect { pass -> updateAds { copy(pass = pass) } } }
        viewModelScope.launch { graph.ads.ready.collect { r -> updateAds { copy(sdkReady = r) } } }
        viewModelScope.launch { graph.ads.rewardedReady.collect { r -> updateAds { copy(rewardedReady = r) } } }
        // One tick drives both the "min left" label and listening milestones.
        viewModelScope.launch {
            while (isActive) {
                updateAds { copy(nowMs = clock()) }
                if (_state.value.player.isPlaying) {
                    milestones.add(TICK_MS).forEach { graph.analytics.log(AnalyticsEvent.ListenMilestone(it)) }
                }
                delay(TICK_MS)
            }
        }
    }

    fun onConsentUpdated() = updateAds { copy(privacyOptionsRequired = graph.consent.privacyOptionsRequired) }

    fun selectGenre(genre: Genre) {
        if (genre == _state.value.home.genre && _state.value.home.genreStations is Load.Ready) return
        loadGenre(genre)
    }

    fun retry() {
        loadGenre(_state.value.home.genre)
        loadLocal()
        val q = _state.value.search.query
        if (q.isNotBlank()) search(q)
    }

    fun search(query: String) {
        _state.update { it.copy(search = it.search.copy(query = query)) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(search = it.search.copy(results = null)) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // wait for typing to pause
            _state.update { it.copy(search = it.search.copy(results = Load.Loading)) }
            val result = runCatching { graph.repository.search(query) }
            _state.update { s -> s.copy(search = s.search.copy(results = result.fold({ Load.Ready(it) }, { Load.Failed }))) }
        }
    }

    /**
     * Plays [station]. A switch to a different station is the only point where an interstitial may
     * show, and only when [InterstitialPolicy] allows it; the stream pauses during the ad and the
     * new station starts when it closes.
     */
    fun play(activity: Activity, station: Station, genre: Genre?) {
        val current = _state.value.player.station
        if (current?.id == station.id) {
            if (!_state.value.player.isPlaying) graph.player.resume()
            return
        }
        val now = clock()
        val (next, decision) = policy.onStationSwitch(pacing, now, _state.value.ads.pass)
        pacing = next
        val start = {
            graph.player.play(station)
            graph.analytics.log(AnalyticsEvent.StationPlayed(station.id, genre?.tag))
            viewModelScope.launch { graph.repository.countPlay(station) }
            Unit
        }
        if (decision == InterstitialPolicy.Decision.Show && graph.ads.ready.value) {
            graph.player.pause()
            graph.ads.showInterstitial(
                activity,
                onShown = {
                    pacing = policy.onShown(pacing, clock())
                    graph.analytics.log(AnalyticsEvent.AdShown("interstitial"))
                },
                onClosed = start,
            )
        } else {
            start()
        }
    }

    fun togglePause() = graph.player.togglePause()

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            if (graph.preferences.toggleFavorite(station)) graph.analytics.log(AnalyticsEvent.FavoriteAdded(station.id))
        }
    }

    /** Rewarded ad: the pass is granted only when the SDK reports the reward, never on a mere close. */
    fun watchRewarded(activity: Activity) {
        val wasPlaying = _state.value.player.isPlaying
        if (wasPlaying) graph.player.pause()
        graph.ads.showRewarded(
            activity,
            onShown = { graph.analytics.log(AnalyticsEvent.AdShown("rewarded")) },
            onReward = {
                viewModelScope.launch {
                    val granted = _state.value.ads.pass.grant(clock())
                    graph.preferences.savePass(granted)
                    graph.analytics.log(AnalyticsEvent.AdFreePassGranted((AdFreePass.REWARD_MS / 60_000L).toInt()))
                }
            },
            onClosed = { if (wasPlaying) graph.player.resume() },
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        graph.consent.showPrivacyOptions(activity) { onConsentUpdated() }
    }

    fun onBannerImpression() = graph.analytics.log(AnalyticsEvent.AdShown("banner"))

    private fun loadGenre(genre: Genre) {
        _state.update { it.copy(home = it.home.copy(genre = genre, genreStations = Load.Loading)) }
        viewModelScope.launch {
            val result = runCatching { graph.repository.genre(genre) }
            _state.update { s ->
                if (s.home.genre != genre) s
                else s.copy(home = s.home.copy(genreStations = result.fold({ Load.Ready(it) }, { Load.Failed })))
            }
        }
    }

    private fun loadLocal() {
        val country = Locale.getDefault().country
        viewModelScope.launch {
            val result = runCatching { graph.repository.popularIn(country) }
            _state.update { s -> s.copy(home = s.home.copy(localStations = result.fold({ Load.Ready(it) }, { Load.Failed }))) }
        }
    }

    private fun updateAds(change: AdUiState.() -> AdUiState) = _state.update { it.copy(ads = it.ads.change()) }

    override fun onCleared() {
        graph.player.release()
    }

    companion object {
        private const val TICK_MS = 15_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { OpenDialViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application) }
        }

        fun greeting(hour: Int): String = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late-night listening"
        }

        private fun localTitle(): String? {
            val locale = Locale.getDefault()
            val name = locale.displayCountry
            return if (locale.country.length == 2 && name.isNotBlank()) "Popular in $name" else null
        }
    }
}
