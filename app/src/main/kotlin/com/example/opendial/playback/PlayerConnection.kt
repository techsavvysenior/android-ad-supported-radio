package com.example.opendial.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.opendial.core.stations.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the UI needs to know about playback. */
data class PlayerUiState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null,
)

/**
 * The app's handle on [PlaybackService], through a Media3 [MediaController]. The UI only reads
 * [state] and calls [play], [togglePause] and [pause].
 */
class PlayerConnection(private val context: Context) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var pendingStation: Station? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying, error = null)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(isPlaying = false, isBuffering = false, error = error.errorCodeName)
        }
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                c.addListener(listener)
                controller = c
                pendingStation?.let { start(c, it) }
                pendingStation = null
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun play(station: Station) {
        _state.value = _state.value.copy(station = station, isBuffering = true, error = null)
        val c = controller
        if (c == null) {
            pendingStation = station
            connect()
        } else {
            start(c, station)
        }
    }

    fun togglePause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    private fun start(c: MediaController, station: Station) {
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.tags.take(2).joinToString(" · ").ifEmpty { station.detail })
            .setArtworkUri(station.faviconUrl?.let(Uri::parse))
            .setIsPlayable(true)
            .build()
        val item = MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)
            .apply { if (station.isHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    fun release() {
        controller?.release()
        controller = null
    }
}
