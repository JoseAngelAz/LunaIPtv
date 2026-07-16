package com.lunaiptv.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import com.lunaiptv.phone.di.PhoneHomeState
import com.lunaiptv.phone.di.PhoneHomeViewModel
import com.lunaiptv.phone.R
import com.lunaiptv.core.database.entity.ChannelEntity
import com.lunaiptv.core.database.entity.MovieEntity
import com.lunaiptv.core.database.entity.SeriesEntity
import com.lunaiptv.core.launcher.LauncherContinuationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneHomeScreen(
    vm: PhoneHomeViewModel,
    onPlayContinueMovie: (LauncherContinuationItem) -> Unit,
    onPlayContinueSeries: (LauncherContinuationItem) -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onFavMovie: (MovieEntity) -> Unit,
    onFavSeries: (SeriesEntity) -> Unit,
) {
    val state by vm.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()

    if (state.isLoading && !isRefreshing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!state.hasAnyContent) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                vm.refresh()
                scope.launch {
                    kotlinx.coroutines.delay(800)
                    isRefreshing = false
                }
            },
            state = refreshState,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.welcome_lunaiptv), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    if (state.profileName.isNotBlank()) {
                        Text(
                            text = state.profileName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.pull_to_refresh),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            vm.refresh()
            scope.launch {
                kotlinx.coroutines.delay(800)
                isRefreshing = false
            }
        },
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
        // Weather chip
        state.weatherText?.let { weather ->
            item {
                OutlinedCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("🌤", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = weather,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        // Profile header
        if (state.profileName.isNotBlank()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.profileName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
            // ── Hero: Continue Watching (big featured card) ──────
        if (state.heroItems.isNotEmpty()) {
            item {
                HeroSection(
                    items = state.heroItems,
                    heroMeta = state.heroMeta,
                    onPlayMovie = onPlayContinueMovie,
                    onPlaySeries = onPlayContinueSeries,
                )
            }
        }

        // ── Continue Watching Movies ────────────────────────
        if (state.continueMovies.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.continue_watching_movies))
                ContinueMoviesRow(
                    items = state.continueMovies,
                    onClick = onPlayContinueMovie,
                )
            }
        }

        // ── Continue Watching Series ────────────────────────
        if (state.continueSeries.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.continue_watching_series))
                ContinueSeriesRow(
                    items = state.continueSeries,
                    onClick = onPlayContinueSeries,
                )
            }
        }

        // ── Recent Channels ─────────────────────────────────
        if (state.recentChannels.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.recent_channels))
                ChannelRow(
                    channels = state.recentChannels,
                    onClick = onPlayChannel,
                )
            }
        }

        // ── Favorite Channels ───────────────────────────────
        if (state.favoriteChannels.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.favorite_channels))
                ChannelRow(
                    channels = state.favoriteChannels,
                    onClick = onPlayChannel,
                )
            }
        }

        // ── Favorite Movies ────────────────────────────────
        if (state.favoriteMovies.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.favorite_movies))
                FavoriteMovieRow(
                    movies = state.favoriteMovies,
                    onClick = onFavMovie,
                )
            }
        }

        // ── Favorite Series ────────────────────────────────
        if (state.favoriteSeries.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.favorite_series))
                FavoriteSeriesRow(
                    series = state.favoriteSeries,
                    onClick = onFavSeries,
                )
            }
        }
    }
}
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun HeroSection(
    items: List<LauncherContinuationItem>,
    heroMeta: Map<String, com.lunaiptv.phone.di.HeroMeta>,
    onPlayMovie: (LauncherContinuationItem) -> Unit,
    onPlaySeries: (LauncherContinuationItem) -> Unit,
) {
    val first = items.first()
    val meta = heroMeta[first.stableKey]
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 12.dp)
            .clickable {
                when (first.kind) {
                    com.lunaiptv.core.launcher.LauncherContinuationKind.MOVIE -> onPlayMovie(first)
                    com.lunaiptv.core.launcher.LauncherContinuationKind.EPISODE -> onPlaySeries(first)
                    else -> {}
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Backdrop (TMDB) or poster fallback
            val imageUrl = meta?.backdropUrl ?: first.posterUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = first.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            // Gradient overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 60f,
                        )
                    ),
            )
            // Info
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                if (first.watchNextType == com.lunaiptv.core.launcher.LauncherWatchNextType.NEXT) {
                    Text(
                        text = stringResource(R.string.next_up),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = first.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                first.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                // Progress bar
                if (first.durationMs > 0 && first.positionMs > 0) {
                    val fraction = (first.positionMs.toFloat() / first.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                    val remaining = first.durationMs - first.positionMs
                    val mins = (remaining / 60_000).toInt()
                    Text(
                        text = "${mins}m remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                // TMDB plot (if available)
                meta?.plot?.let { plot ->
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            // Play icon
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun ContinueMoviesRow(
    items: List<LauncherContinuationItem>,
    onClick: (LauncherContinuationItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.stableKey }) { item ->
            ContinueMovieCard(item = item, onClick = { onClick(item) })
        }
    }
}

@Composable
private fun ContinueMovieCard(
    item: LauncherContinuationItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .aspectRatio(0.67f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("🎬")
                }
            }
            // Play icon overlay (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            // Progress + title overlay
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .padding(4.dp),
            ) {
                if (item.durationMs > 0 && item.positionMs > 0) {
                    val fraction = (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ContinueSeriesRow(
    items: List<LauncherContinuationItem>,
    onClick: (LauncherContinuationItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.stableKey }) { item ->
            ContinueSeriesCard(item = item, onClick = { onClick(item) })
        }
    }
}

@Composable
private fun ContinueSeriesCard(
    item: LauncherContinuationItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .aspectRatio(16f / 9f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("📺")
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
            )
            // Play icon overlay (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            ) {
                // Season/Episode chip
                if (item.seasonNumber != null && item.episodeNumber != null) {
                    Text(
                        text = "S${item.seasonNumber} E${item.episodeNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.durationMs > 0 && item.positionMs > 0) {
                    val fraction = (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
            }
        }
    }
}
}

@Composable
private fun ChannelRow(
    channels: List<ChannelEntity>,
    onClick: (ChannelEntity) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(channels, key = { it.id }) { ch ->
            ChannelCard(channel = ch, onClick = { onClick(ch) })
        }
    }
}

@Composable
private fun ChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .aspectRatio(16f / 9f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // Channel name overlay
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun FavoriteMovieRow(
    movies: List<MovieEntity>,
    onClick: (MovieEntity) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(movies, key = { it.id }) { movie ->
            Card(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(0.67f)
                    .clickable { onClick(movie) },
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        text = movie.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteSeriesRow(
    series: List<SeriesEntity>,
    onClick: (SeriesEntity) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(series, key = { it.id }) { s ->
            Card(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(0.67f)
                    .clickable { onClick(s) },
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = s.posterUrl,
                        contentDescription = s.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        text = s.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
