package com.example.opendial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.opendial.core.ads.AdFreePass
import com.example.opendial.core.stations.Genre
import com.example.opendial.core.stations.Station
import com.example.opendial.playback.PlayerUiState
import com.example.opendial.ui.AdUiState
import com.example.opendial.ui.AppUiState
import com.example.opendial.ui.HomeUiState
import com.example.opendial.ui.Load
import com.example.opendial.ui.OpenDialScaffold
import com.example.opendial.ui.SearchUiState
import com.example.opendial.ui.Tab
import com.example.opendial.ui.components.AdFrame
import com.example.opendial.ui.theme.Amber
import com.example.opendial.ui.theme.Ember
import com.example.opendial.ui.theme.LocalAnimationsEnabled
import com.example.opendial.ui.theme.LocalLoadArtwork
import com.example.opendial.ui.theme.OpenDialTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A Pixel 7: 1080 x 2400 at 420 dpi. */
private const val PIXEL_7 = "w411dp-h914dp-normal-long-notround-any-420dpi-keyshidden-nonav"

/**
 * Renders the real screens with sample data and writes the PNGs used in the README
 * (docs/screenshots). Station names here are made up; the app itself shows live directory data.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PIXEL_7, application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val dir = File(System.getProperty("opendial.screenshots") ?: "build/screenshots").also { it.mkdirs() }

    private fun station(id: String, name: String, tags: List<String>, codec: String, kbps: Int, cc: String) =
        Station(id, name, "https://example.org/$id", null, null, tags, cc, codec, kbps, false, 100, 100)

    private val classical = listOf(
        station("s01", "Nocturne Classical", listOf("classical", "piano"), "AAC", 128, "US"),
        station("s02", "Baroque Hall Radio", listOf("classical", "baroque"), "MP3", 192, "DE"),
        station("s03", "Opera Tonight", listOf("opera", "vocal"), "AAC", 96, "IT"),
        station("s04", "Quiet Strings", listOf("chamber", "strings"), "OGG", 160, "AT"),
        station("s05", "Symphony 24", listOf("orchestra"), "AAC", 128, "GB"),
        station("s06", "Film Score FM", listOf("soundtrack"), "MP3", 128, "US"),
    )
    private val local = listOf(
        station("l01", "Harbor Jazz 24", listOf("jazz", "smooth"), "AAC", 128, "US"),
        station("l02", "Lo-fi Study Room", listOf("lofi", "beats"), "MP3", 128, "US"),
        station("l03", "Morning Dispatch", listOf("news", "talk"), "AAC", 64, "US"),
        station("l04", "Ambient Drift", listOf("ambient", "chill"), "OGG", 112, "US"),
    )

    private val playing = PlayerUiState(station = classical[0], isPlaying = true)

    private fun base(ads: AdUiState = AdUiState(rewardedReady = true)) = AppUiState(
        home = HomeUiState(
            greeting = "Good evening",
            genre = Genre.Classical,
            genreStations = Load.Ready(classical),
            localTitle = "Popular in United States",
            localStations = Load.Ready(local),
        ),
        search = SearchUiState(query = "jazz", results = Load.Ready(local.take(1) + classical.take(2))),
        favorites = listOf(classical[0], local[0], local[1], classical[2]),
        player = playing,
        ads = ads,
    )

    @Composable
    private fun FakeBanner() {
        AdFrame {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Brush.horizontalGradient(listOf(Ember.copy(alpha = 0.25f), Amber.copy(alpha = 0.25f)))),
                contentAlignment = Alignment.Center,
            ) { Text("Test ad · inline adaptive banner", style = MaterialTheme.typography.labelLarge) }
        }
    }

    private fun shoot(name: String, dark: Boolean, state: AppUiState, tab: Tab = Tab.Listen, nowPlaying: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalAnimationsEnabled provides false, LocalLoadArtwork provides false) {
                OpenDialTheme(darkTheme = dark) {
                    OpenDialScaffold(
                        state = state,
                        onGenre = {}, onPlay = { _, _ -> }, onRetry = {}, onQuery = {}, onToggle = {},
                        onFavorite = {}, onWatchAd = {}, onPrivacyOptions = {},
                        adSlot = { if (!state.ads.adFree) FakeBanner() },
                        initialTab = tab,
                        initialNowPlaying = nowPlaying,
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage(File(dir, "$name.png").path)
    }

    @Test
    fun home_dark() = shoot("home_dark", dark = true, state = base())

    @Test
    fun home_light() = shoot("home_light", dark = false, state = base())

    @Test
    fun now_playing_dark() = shoot("now_playing_dark", dark = true, state = base(), nowPlaying = true)

    @Test
    fun now_playing_ad_free_dark() {
        val pass = AdFreePass(untilMs = 52 * 60_000L)
        shoot("now_playing_ad_free_dark", dark = true, state = base(AdUiState(pass = pass, nowMs = 0, rewardedReady = true)), nowPlaying = true)
    }

    @Test
    fun search_dark() = shoot("search_dark", dark = true, state = base(), tab = Tab.Search)

    @Test
    fun library_light() = shoot("library_light", dark = false, state = base(), tab = Tab.Library)
}
