package com.lunaiptv.phone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lunaiptv.core.database.entity.MovieEntity
import com.lunaiptv.core.database.entity.SeriesEntity
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.phone.di.PhoneSearchIntent
import com.lunaiptv.phone.di.PhoneSearchResults
import com.lunaiptv.phone.di.PhoneSearchViewModel
import com.lunaiptv.phone.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSearchScreen(
    vm: PhoneSearchViewModel,
    onPlayChannel: (com.lunaiptv.core.database.entity.ChannelEntity) -> Unit,
    onPlayMovie: (MovieEntity) -> Unit,
    onOpenSeries: (SeriesEntity) -> Unit,
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val curated by vm.curatedResults.collectAsStateWithLifecycle()
    val intent by vm.intent.collectAsStateWithLifecycle()
    val recentSearches by vm.recentSearches.collectAsStateWithLifecycle()
    val favorites by vm.favoriteChannelIds.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search bar
            TextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.close))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val displayResults = when {
                query.length >= 2 -> results
                query.isEmpty() && intent != null -> curated
                else -> PhoneSearchResults()
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Recent searches (when empty query)
                if (query.isEmpty() && intent == null && recentSearches.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.recent_searches),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(recentSearches.take(5)) { term ->
                        ListItem(
                            headlineContent = { Text(term) },
                            leadingContent = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.clickable { vm.setQuery(term) },
                        )
                    }
                    item {
                        Text(
                            stringResource(R.string.clear_history),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.clearRecentSearches() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                // Empty-state intent chips
                if (query.isEmpty() && intent == null) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            PhoneSearchIntent.entries.forEach { si ->
                                androidx.compose.material3.AssistChip(
                                    onClick = { vm.setIntent(si) },
                                    label = { Text(si.label) },
                                )
                            }
                        }
                    }
                }

                // Intent back button
                if (query.isEmpty() && intent != null) {
                    item {
                        Text(
                            stringResource(R.string.back_to_search),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.setIntent(null) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                // Channel results
                if (displayResults.channels.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.channels_section)) }
                    items(displayResults.channels) { row ->
                        val ch = row.channel
                        val isFav = favorites.contains(ch.id)
                        ListItem(
                            headlineContent = { Text(ch.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                val sub = buildString {
                                    row.categoryName?.let { append(it) }
                                }
                                if (sub.isNotEmpty()) Text(sub, maxLines = 1)
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = ch.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { vm.toggleFavoriteChannel(ch) }) {
                                        Icon(
                                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = stringResource(R.string.favorite),
                                            tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    IconButton(onClick = { onPlayChannel(ch) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play))
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onPlayChannel(ch) },
                        )
                    }
                }

                // Movie results
                if (displayResults.movies.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.movies_section)) }
                    items(displayResults.movies) { movie ->
                        ListItem(
                            headlineContent = { Text(movie.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                val sub = buildString {
                                    movie.year?.let { append(it.toString()) }
                                }
                                if (sub.isNotEmpty()) Text(sub, maxLines = 1)
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = movie.posterUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onPlayMovie(movie) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play))
                                }
                            },
                            modifier = Modifier.clickable { onPlayMovie(movie) },
                        )
                    }
                }

                // Series results
                if (displayResults.series.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.series_section)) }
                    items(displayResults.series) { series ->
                        ListItem(
                            headlineContent = { Text(series.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = {
                                AsyncImage(
                                    model = series.posterUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                )
                            },
                            modifier = Modifier.clickable { onOpenSeries(series) },
                        )
                    }
                }

                // Empty state
                if (displayResults.isEmpty && query.length >= 2) {
                    item {
                        Text(
                            "No results for \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            // Big empty state when no query and no intent
            if (query.isEmpty() && intent == null && recentSearches.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.search),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.find_channels_movies_series),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
