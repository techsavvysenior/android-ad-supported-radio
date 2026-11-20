package com.example.opendial.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.opendial.R
import com.example.opendial.core.stations.Station
import com.example.opendial.playback.PlayerUiState
import com.example.opendial.ui.Load
import com.example.opendial.ui.SearchUiState
import com.example.opendial.ui.components.StationCard
import com.example.opendial.ui.components.StationRow

@Composable
fun SearchScreen(
    state: SearchUiState,
    player: PlayerUiState,
    onQuery: (String) -> Unit,
    onPlay: (Station) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        item(key = "title") {
            Text(
                stringResource(R.string.tab_search),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 16.dp),
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.close)) }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
        when (val results = state.results) {
            null -> item(key = "hint") { Hint(stringResource(R.string.search_empty)) }
            Load.Loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            Load.Failed -> item(key = "error") { ErrorCard(onRetry) }
            is Load.Ready -> if (results.value.isEmpty()) {
                item(key = "none") { Hint(stringResource(R.string.search_no_results)) }
            } else {
                items(results.value, key = { it.id }) { station ->
                    StationRow(
                        station,
                        station.id == player.station?.id,
                        player.isPlaying,
                        onClick = { onPlay(station) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 32.dp),
    )
}

@Composable
fun LibraryScreen(
    favorites: List<Station>,
    player: PlayerUiState,
    onPlay: (Station) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(152.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "title") {
            Text(
                stringResource(R.string.tab_library),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (favorites.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                Column(Modifier.fillMaxSize().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.library_empty_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.library_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(favorites, key = { it.id }) { station ->
                StationCard(station, station.id == player.station?.id, player.isPlaying, onClick = { onPlay(station) })
            }
        }
    }
}
