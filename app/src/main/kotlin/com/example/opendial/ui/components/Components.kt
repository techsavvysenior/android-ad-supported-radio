package com.example.opendial.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.opendial.R
import com.example.opendial.core.stations.Station
import com.example.opendial.ui.theme.LocalAnimationsEnabled
import com.example.opendial.ui.theme.LocalLoadArtwork
import kotlin.math.abs

/** Two hues picked from the station id, so every station gets its own stable artwork colors. */
fun stationColors(station: Station): Pair<Color, Color> {
    val h = abs(station.id.hashCode())
    val hue1 = (h % 360).toFloat()
    val hue2 = (hue1 + 40f + (h / 360 % 60)) % 360f
    return Color.hsl(hue1, 0.62f, 0.52f) to Color.hsl(hue2, 0.70f, 0.38f)
}

private fun initials(name: String): String =
    name.split(' ', '-', '_').filter { it.firstOrNull()?.isLetterOrDigit() == true }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "♪" }

/** A gradient tile with the station's initials, with its logo on top when it has one. */
@Composable
fun StationArt(station: Station, size: Dp, modifier: Modifier = Modifier, corner: Dp = size / 5) {
    val (a, b) = stationColors(station)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(a, b))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(station.name),
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.32f).sp,
        )
        if (LocalLoadArtwork.current && station.faviconUrl != null) {
            AsyncImage(
                model = station.faviconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Four bouncing bars while playing, resting low when paused. */
@Composable
fun EqualizerBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val rest = listOf(0.35f, 0.7f, 0.5f, 0.85f)
    Row(modifier = modifier.height(16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        if (playing && LocalAnimationsEnabled.current) {
            // The infinite transition only exists while playing, so a paused player costs nothing.
            val transition = rememberInfiniteTransition(label = "eq")
            rest.indices.forEach { i ->
                val level by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(durationMillis = 380 + i * 110), RepeatMode.Reverse),
                    label = "bar$i",
                )
                Bar(level, color)
            }
        } else {
            rest.forEach { still -> Bar(if (playing) still else 0.25f, color) }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .width(3.dp)
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

@Composable
fun LivePill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.live), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** A large card for the "Trending" carousel. */
@Composable
fun StationCard(station: Station, isCurrent: Boolean, playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(152.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        Box {
            StationArt(station, 152.dp, corner = 20.dp)
            if (isCurrent) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) { EqualizerBars(playing, color = Color.White) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(station.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            station.tags.take(2).joinToString(" · ").ifEmpty { station.detail },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A list row: logo, name, format, and a play button or the live bars. */
@Composable
fun StationRow(station: Station, isCurrent: Boolean, playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArt(station, 56.dp, corner = 14.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(station.tags.firstOrNull(), station.detail.ifEmpty { null }).filterNotNull().joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            EqualizerBars(playing, modifier = Modifier.padding(horizontal = 12.dp))
        } else {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.play),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
fun PlayPauseIcon(playing: Boolean, modifier: Modifier = Modifier) {
    if (playing) {
        Icon(painterResource(R.drawable.ic_pause), contentDescription = stringResource(R.string.pause), modifier = modifier)
    } else {
        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.play), modifier = modifier)
    }
}

/** The bar above the tabs while something is loaded. Tapping it opens Now Playing. */
@Composable
fun MiniPlayer(
    station: Station,
    playing: Boolean,
    buffering: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column {
            Row(
                Modifier.clickable(onClick = onOpen).padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationArt(station, 44.dp, corner = 12.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EqualizerBars(playing, modifier = Modifier.graphicsLayer(scaleX = 0.8f, scaleY = 0.8f))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            station.detail.ifEmpty { stringResource(R.string.live) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                FilledIconButton(
                    onClick = onToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { PlayPauseIcon(playing) }
            }
            if (buffering) {
                LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(horizontal = 20.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The labelled frame an ad sits in, so it is never mistaken for a station. */
@Composable
fun AdFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.advertisement).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
