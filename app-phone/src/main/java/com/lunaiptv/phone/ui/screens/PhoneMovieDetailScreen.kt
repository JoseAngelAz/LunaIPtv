package com.lunaiptv.phone.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneMoviesViewModel
import com.lunaiptv.core.database.entity.MovieEntity
import com.lunaiptv.core.model.DownloadStatus

@Composable
fun PhoneMovieDetailScreen(
    movie: MovieEntity,
    vm: PhoneMoviesViewModel,
    onBack: () -> Unit,
    onPlay: (MovieEntity) -> Unit,
    onDownload: ((MovieEntity) -> Unit)? = null,
    downloadStatus: DownloadStatus? = null,
) {
    val allFavorites by vm.favoriteIds.collectAsState()
    val movieMeta by vm.currentMovieMeta.collectAsState()

    LaunchedEffect(movie.id) { vm.loadMovieMeta(movie) }

    DisposableEffect(movie.id) {
        onDispose { vm.clearMovieMeta(); vm.saveProgress(movie.id) }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar (fixed)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.toggleFavorite(movie) }) {
                Icon(
                    imageVector = if (movie.id in allFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (movie.id in allFavorites) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Hero image (fixed height)
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp),
        ) {
            AsyncImage(
                model = movie.backdropUrl ?: movie.posterUrl,
                contentDescription = movie.name,
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
                text = movie.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        // Info + play + download + synopsis (all scrollable as one)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    movie.year?.let {
                        Text("$it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    movie.rating?.let {
                        Text("\u2605 %.1f".format(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    movie.durationSecs?.let { secs ->
                        val h = secs / 3600
                        val m = (secs % 3600) / 60
                        Text("${h}h ${m}m", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Play button
                Button(
                    onClick = { onPlay(movie) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play), style = MaterialTheme.typography.titleMedium)
                }

                // Download button
                if (onDownload != null) {
                    Spacer(Modifier.height(8.dp))
                    val downloading = downloadStatus != null && downloadStatus != DownloadStatus.COMPLETED && downloadStatus != DownloadStatus.FAILED
                    val completed = downloadStatus == DownloadStatus.COMPLETED
                    OutlinedButton(
                        onClick = { if (!downloading && !completed) onDownload(movie) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !downloading && !completed,
                    ) {
                        if (downloading) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.downloading), style = MaterialTheme.typography.titleMedium)
                        } else if (completed) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.status_completed), style = MaterialTheme.typography.titleMedium)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.download), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Synopsis label
                Text(
                    text = stringResource(R.string.synopsis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))

                // Synopsis text
                val synopsisText = movie.plot?.takeIf { it.isNotBlank() }
                    ?: movieMeta?.overview
                if (synopsisText != null) {
                    Text(
                        text = synopsisText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_synopsis_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { vm.retryMovieMeta(movie) },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.search_synopsis), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
