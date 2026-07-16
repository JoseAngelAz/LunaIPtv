package com.lunaiptv.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneSeriesViewModel
import com.lunaiptv.core.database.entity.DownloadEntity
import com.lunaiptv.core.database.entity.EpisodeEntity
import com.lunaiptv.core.database.entity.SeriesEntity
import com.lunaiptv.core.model.DownloadStatus
import com.lunaiptv.core.model.MediaType

@Composable
fun PhoneSeriesDetailScreen(
    series: SeriesEntity,
    vm: PhoneSeriesViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (EpisodeEntity) -> Unit,
    onDownloadEpisode: ((EpisodeEntity) -> Unit)? = null,
    episodeDownloads: List<DownloadEntity> = emptyList(),
) {
    val allFavorites by vm.favoriteIds.collectAsState()
    val openedSeries by vm.openedSeries.collectAsState()
    val seasons by vm.seasons.collectAsState()
    val selectedSeason by vm.selectedSeason.collectAsState()
    val filteredEps by vm.filteredEpisodes.collectAsState()
    val episodesLoading by vm.episodesLoading.collectAsState()
    val episodeProgress by vm.episodeProgress.collectAsState()
    val seriesMeta by vm.currentSeriesMeta.collectAsState()

    LaunchedEffect(series.id) {
        vm.loadSeriesMeta(series)
        if (openedSeries?.id != series.id) {
            vm.openSeries(series)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                vm.closeSeries()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = series.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { vm.toggleFavorite(series) }) {
                Icon(
                    imageVector = if (series.id in allFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (series.id in allFavorites) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Hero image
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp),
        ) {
            AsyncImage(
                model = series.backdropUrl ?: series.posterUrl,
                contentDescription = series.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.6f to Color.Black.copy(alpha = 0.7f),
                            1f to Color.Black,
                        )
                    ),
            )
            Text(
                text = series.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        // Info + synopsis (scrollable, capped height)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                series.year?.let {
                    Text("$it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                series.rating?.let {
                    Text("\u2605 %.1f".format(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            val synopsisPlot = series.plot?.takeIf { it.isNotBlank() } ?: seriesMeta?.overview
            synopsisPlot?.let { plot ->
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Season chips (fixed height, no expanding)
        if (seasons.size > 1) {
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(seasons) { sNum ->
                    FilterChip(
                        selected = sNum == selectedSeason,
                        onClick = { vm.selectSeason(sNum) },
                        label = { Text(stringResource(R.string.season_number, sNum)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }

        // Episodes list — fills ALL remaining space
        if (episodesLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredEps) { ep ->
                    val epDownload = episodeDownloads.find { it.itemId == ep.id && it.mediaType == MediaType.EPISODE }
                    EpisodeRow(
                        episode = ep,
                        progress = episodeProgress[ep.id],
                        onClick = {
                            val saved = vm.savedPositionMs(ep.id)
                            vm.playEpisode(ep, saved)
                            onPlayEpisode(ep)
                        },
                        onDownload = if (onDownloadEpisode != null) { { onDownloadEpisode(ep) } } else null,
                        downloadStatus = epDownload?.status,
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeEntity,
    progress: com.lunaiptv.core.database.entity.PlaybackProgressEntity?,
    onClick: () -> Unit,
    onDownload: (() -> Unit)? = null,
    downloadStatus: DownloadStatus? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "E${episode.episodeNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(40.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = episode.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    episode.plot?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onDownload != null) {
                    val downloading = downloadStatus != null && downloadStatus != DownloadStatus.COMPLETED && downloadStatus != DownloadStatus.FAILED
                    val completed = downloadStatus == DownloadStatus.COMPLETED
                    IconButton(onClick = { if (!downloading && !completed) onDownload() }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(R.string.download),
                            modifier = Modifier.size(20.dp),
                            tint = when {
                                completed -> MaterialTheme.colorScheme.primary
                                downloading -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            progress?.let { p ->
                if (p.durationMs > 0) {
                    val fraction = (p.positionMs.toFloat() / p.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}
