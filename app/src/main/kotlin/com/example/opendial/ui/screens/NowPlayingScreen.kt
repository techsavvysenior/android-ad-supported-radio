package com.example.opendial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.opendial.R
import com.example.opendial.core.stations.Station
import com.example.opendial.ui.AdUiState
import com.example.opendial.ui.components.EqualizerBars
import com.example.opendial.ui.components.LivePill
import com.example.opendial.ui.components.PlayPauseIcon
import com.example.opendial.ui.components.StationArt
import com.example.opendial.ui.components.stationColors

/**
 * Full-screen player. The background takes the station's two artwork colors. The rewarded-ad
 * offer sits under the controls, phrased as what the listener gets, and disappears while the pass
 * is already full.
 */
@Composable
fun NowPlayingScreen(
    station: Station,
    playing: Boolean,
    buffering: Boolean,
    error: String?,
    isFavorite: Boolean,
    ads: AdUiState,
    onToggle: () -> Unit,
    onFavorite: () -> Unit,
    onWatchAd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (a, b) = stationColors(station)
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxSize()
            .background(bg)
            .background(Brush.verticalGradient(listOf(a.copy(alpha = 0.55f), b.copy(alpha = 0.25f), bg))),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.close)) }
                Spacer(Modifier.weight(1f))
                LivePill()
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(if (isFavorite) R.string.unfavorite else R.string.favorite),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.weight(0.6f))
            StationArt(
                station,
                size = 264.dp,
                corner = 36.dp,
                modifier = Modifier.shadow(32.dp, RoundedCornerShape(36.dp), ambientColor = a, spotColor = a),
            )
            Spacer(Modifier.height(32.dp))
            Text(
                station.name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                error?.let { "Can't play this stream ($it)" } ?: station.detail,
                style = MaterialTheme.typography.bodyLarge,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (station.tags.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    station.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag) },
                            shape = RoundedCornerShape(50),
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.06f)),
                            border = null,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(0.5f))
            Box(contentAlignment = Alignment.Center) {
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(84.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { PlayPauseIcon(playing, modifier = Modifier.size(40.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            EqualizerBars(playing && !buffering)
            Spacer(Modifier.weight(0.5f))
            AdFreeOffer(ads, onWatchAd)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdFreeOffer(ads: AdUiState, onWatchAd: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (ads.adFree) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)) {
                Text(
                    stringResource(R.string.ad_free_active, ads.minutesLeft),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        if (ads.canBuyMore) {
            FilledTonalButton(onClick = onWatchAd, enabled = ads.rewardedReady, shape = RoundedCornerShape(50)) {
                Text(
                    when {
                        !ads.rewardedReady -> stringResource(R.string.ad_free_loading)
                        ads.adFree -> stringResource(R.string.ad_free_offer_more)
                        else -> stringResource(R.string.ad_free_offer)
                    },
                )
            }
        }
    }
}
