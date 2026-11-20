package com.example.opendial.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.opendial.R
import com.example.opendial.core.stations.Genre
import com.example.opendial.core.stations.Station
import com.example.opendial.playback.PlayerUiState
import com.example.opendial.ui.HomeUiState
import com.example.opendial.ui.Load
import com.example.opendial.ui.components.SectionHeader
import com.example.opendial.ui.components.StationCard
import com.example.opendial.ui.components.StationRow

@Composable
fun HomeScreen(
    state: HomeUiState,
    player: PlayerUiState,
    adFreeLabel: String?,
    onGenre: (Genre) -> Unit,
    onPlay: (Station, Genre?) -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    adSlot: @Composable () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val currentId = player.station?.id
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.greeting, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
                }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings)) }
            }
            if (adFreeLabel != null) {
                Text(
                    adFreeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        item(key = "genres") {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Genre.entries) { genre ->
                    FilterChip(
                        selected = genre == state.genre,
                        onClick = { onGenre(genre) },
                        label = { Text(genre.title) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        item(key = "trending-title") {
            SectionHeader("Trending in ${state.genre.title}", subtitle = "Most played right now")
            Spacer(Modifier.height(12.dp))
        }
        item(key = "trending") {
            when (val load = state.genreStations) {
                Load.Loading -> LoadingRow()
                Load.Failed -> ErrorCard(onRetry)
                is Load.Ready -> LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(load.value.take(10), key = { it.id }) { station ->
                        StationCard(station, station.id == currentId, player.isPlaying, onClick = { onPlay(station, state.genre) })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        item(key = "ad") {
            adSlot()
            Spacer(Modifier.height(16.dp))
        }
        val local = state.localStations
        val localTitle = state.localTitle
        if (localTitle != null && local is Load.Ready && local.value.isNotEmpty()) {
            item(key = "local-title") {
                SectionHeader(localTitle, subtitle = "Popular where you are")
                Spacer(Modifier.height(4.dp))
            }
            items(local.value.take(8), key = { "local-" + it.id }) { station ->
                StationRow(station, station.id == currentId, player.isPlaying, onClick = { onPlay(station, null) }, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
        val more = state.genreStations
        if (more is Load.Ready && more.value.size > 10) {
            item(key = "more-title") {
                Spacer(Modifier.height(16.dp))
                SectionHeader("More ${state.genre.title}")
                Spacer(Modifier.height(4.dp))
            }
            items(more.value.drop(10).take(20), key = { "more-" + it.id }) { station ->
                StationRow(station, station.id == currentId, player.isPlaying, onClick = { onPlay(station, state.genre) }, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), userScrollEnabled = false) {
        items(4) {
            Column {
                Box(Modifier.size(152.dp).padding(0.dp).then(Modifier.placeholder(RoundedCornerShape(20.dp))))
                Spacer(Modifier.height(10.dp))
                Box(Modifier.size(width = 110.dp, height = 14.dp).placeholder(RoundedCornerShape(7.dp)))
            }
        }
    }
}

@Composable
fun ErrorCard(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .placeholder(RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.error_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.retry)) }
    }
}
