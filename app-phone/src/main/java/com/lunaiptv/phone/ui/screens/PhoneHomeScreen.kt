package com.lunaiptv.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lunaiptv.phone.di.PhoneHomeState
import com.lunaiptv.phone.di.PhoneHomeViewModel
import com.lunaiptv.core.database.entity.ChannelEntity
import com.lunaiptv.core.launcher.LauncherContinuationItem

@Composable
fun PhoneHomeScreen(
    vm: PhoneHomeViewModel,
    onPlayContinueMovie: (LauncherContinuationItem) -> Unit,
    onPlayContinueSeries: (LauncherContinuationItem) -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
) {
    val state by vm.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!state.hasAnyContent) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎬", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(16.dp))
                Text("Welcome to LunaIPtv", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Start watching to see your content here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ── Hero: Continue Watching (big featured card) ──────
        if (state.heroItems.isNotEmpty()) {
            item {
                HeroSection(
                    items = state.heroItems,
                    onPlayMovie = onPlayContinueMovie,
                    onPlaySeries = onPlayContinueSeries,
                )
            }
        }

        // ── Continue Watching Movies ────────────────────────
        if (state.continueMovies.isNotEmpty()) {
            item {
                SectionHeader("Continue Watching Movies")
                ContinueMoviesRow(
                    items = state.continueMovies,
                    onClick = onPlayContinueMovie,
                )
            }
        }

        // ── Continue Watching Series ────────────────────────
        if (state.continueSeries.isNotEmpty()) {
            item {
                SectionHeader("Continue Watching Series")
                ContinueSeriesRow(
                    items = state.continueSeries,
                    onClick = onPlayContinueSeries,
                )
            }
        }

        // ── Recent Channels ─────────────────────────────────
        if (state.recentChannels.isNotEmpty()) {
            item {
                SectionHeader("Recent Channels")
                ChannelRow(
                    channels = state.recentChannels,
                    onClick = onPlayChannel,
                )
            }
        }

        // ── Favorite Channels ───────────────────────────────
        if (state.favoriteChannels.isNotEmpty()) {
            item {
                SectionHeader("Favorite Channels")
                ChannelRow(
                    channels = state.favoriteChannels,
                    onClick = onPlayChannel,
                )
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
    onPlayMovie: (LauncherContinuationItem) -> Unit,
    onPlaySeries: (LauncherContinuationItem) -> Unit,
) {
    val first = items.first()
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
            // Backdrop / poster
            if (first.posterUrl != null) {
                AsyncImage(
                    model = first.posterUrl,
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
                        text = "NEXT UP",
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
                    contentDescription = "Play",
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
