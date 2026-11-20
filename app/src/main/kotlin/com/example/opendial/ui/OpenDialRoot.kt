package com.example.opendial.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.opendial.R
import com.example.opendial.ads.BannerSlot
import com.example.opendial.core.stations.Genre
import com.example.opendial.core.stations.Station
import com.example.opendial.ui.components.MiniPlayer
import com.example.opendial.ui.screens.HomeScreen
import com.example.opendial.ui.screens.LibraryScreen
import com.example.opendial.ui.screens.NowPlayingScreen
import com.example.opendial.ui.screens.SearchScreen
import com.example.opendial.ui.screens.SettingsSheet

enum class Tab(val label: Int, val icon: ImageVector) {
    Listen(R.string.tab_listen, Icons.Filled.Home),
    Search(R.string.tab_search, Icons.Filled.Search),
    Library(R.string.tab_library, Icons.Filled.Favorite),
}

@Composable
fun OpenDialRoot(viewModel: OpenDialViewModel, activity: Activity) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OpenDialScaffold(
        state = state,
        onGenre = viewModel::selectGenre,
        onPlay = { station, genre -> viewModel.play(activity, station, genre) },
        onRetry = viewModel::retry,
        onQuery = viewModel::search,
        onToggle = viewModel::togglePause,
        onFavorite = viewModel::toggleFavorite,
        onWatchAd = { viewModel.watchRewarded(activity) },
        onPrivacyOptions = { viewModel.showPrivacyOptions(activity) },
        adSlot = {
            if (!state.ads.adFree) {
                BannerSlot(
                    adUnitId = state.ads.bannerUnit,
                    sdkReady = state.ads.sdkReady,
                    onImpression = viewModel::onBannerImpression,
                )
            }
        },
    )
}

/** Stateless: everything comes in as [state] and callbacks, which is what the screenshot tests render. */
@Composable
fun OpenDialScaffold(
    state: AppUiState,
    onGenre: (Genre) -> Unit,
    onPlay: (Station, Genre?) -> Unit,
    onRetry: () -> Unit,
    onQuery: (String) -> Unit,
    onToggle: () -> Unit,
    onFavorite: (Station) -> Unit,
    onWatchAd: () -> Unit,
    onPrivacyOptions: () -> Unit,
    adSlot: @Composable () -> Unit,
    initialTab: Tab = Tab.Listen,
    initialNowPlaying: Boolean = false,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var nowPlayingOpen by rememberSaveable { mutableStateOf(initialNowPlaying) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val station = state.player.station

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    if (station != null) {
                        MiniPlayer(
                            station = station,
                            playing = state.player.isPlaying,
                            buffering = state.player.isBuffering,
                            onOpen = { nowPlayingOpen = true },
                            onToggle = onToggle,
                        )
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = t == tab,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(stringResource(t.label)) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            val content = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            when (tab) {
                Tab.Listen -> HomeScreen(
                    state = state.home,
                    player = state.player,
                    adFreeLabel = if (state.ads.adFree) stringResource(R.string.ad_free_active, state.ads.minutesLeft) else null,
                    onGenre = onGenre,
                    onPlay = { s, g -> onPlay(s, g) },
                    onRetry = onRetry,
                    onSettings = { settingsOpen = true },
                    adSlot = adSlot,
                    contentPadding = content,
                )
                Tab.Search -> SearchScreen(state.search, state.player, onQuery, { onPlay(it, null) }, onRetry, content)
                Tab.Library -> LibraryScreen(state.favorites, state.player, { onPlay(it, null) }, content)
            }
        }

        AnimatedVisibility(
            visible = nowPlayingOpen && station != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            if (station != null) {
                NowPlayingScreen(
                    station = station,
                    playing = state.player.isPlaying,
                    buffering = state.player.isBuffering,
                    error = state.player.error,
                    isFavorite = state.currentIsFavorite,
                    ads = state.ads,
                    onToggle = onToggle,
                    onFavorite = { onFavorite(station) },
                    onWatchAd = onWatchAd,
                    onClose = { nowPlayingOpen = false },
                )
            }
        }
    }

    BackHandler(enabled = nowPlayingOpen) { nowPlayingOpen = false }

    if (settingsOpen) {
        SettingsSheet(state.ads, onPrivacyOptions = onPrivacyOptions, onDismiss = { settingsOpen = false })
    }
}
