package com.lunaiptv.phone.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneDownloadsViewModel
import com.lunaiptv.core.database.entity.DownloadEntity
import com.lunaiptv.core.model.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneDownloadsScreen(
    vm: PhoneDownloadsViewModel,
    onBack: () -> Unit,
) {
    val downloads by vm.downloads.collectAsState()
    val storageInfo by vm.storageInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (downloads.any { it.status == DownloadStatus.COMPLETED }) {
                        TextButton(onClick = { vm.clearCompleted() }) {
                            Text("Clear done", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Storage bar
            storageInfo?.let { info ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val usedGb = info.usedBytes / (1024.0 * 1024 * 1024)
                    val totalGb = info.totalBytes / (1024.0 * 1024 * 1024)
                    Text(
                        stringResource(R.string.storage_used, "%.1f".format(usedGb), "%.1f".format(totalGb)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { info.usedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    )
                }
            }

            if (downloads.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_downloads),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(downloads, key = { it.id }) { dl ->
                        DownloadItem(
                            download = dl,
                            onPause = { vm.pause(dl) },
                            onResume = { vm.resume(dl) },
                            onRetry = { vm.retry(dl) },
                            onDelete = { vm.delete(dl) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    download: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (download.posterUrl != null) {
                AsyncImage(
                    model = download.posterUrl,
                    contentDescription = download.title,
                    modifier = Modifier
                        .width(48.dp)
                        .aspectRatio(0.67f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .aspectRatio(0.67f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        download.title.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    download.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))

                val statusText = when (download.status) {
                    DownloadStatus.QUEUED -> stringResource(R.string.status_queued)
                    DownloadStatus.RUNNING -> {
                        if (download.totalBytes > 0) {
                            val pct = (download.downloadedBytes * 100 / download.totalBytes).toInt()
                            "${stringResource(R.string.downloading)} $pct%"
                        } else stringResource(R.string.downloading)
                    }
                    DownloadStatus.PAUSED -> stringResource(R.string.status_paused)
                    DownloadStatus.COMPLETED -> stringResource(R.string.status_completed)
                    DownloadStatus.FAILED -> stringResource(R.string.status_failed)
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (download.status) {
                        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                if (download.status == DownloadStatus.RUNNING && download.totalBytes > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
            }

            // Action buttons
            when (download.status) {
                DownloadStatus.RUNNING -> {
                    IconButton(onClick = onPause) {
                        Text(
                            "\u23F8",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                DownloadStatus.PAUSED, DownloadStatus.QUEUED -> {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.resume), modifier = Modifier.size(20.dp))
                    }
                }
                DownloadStatus.FAILED -> {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.retry), modifier = Modifier.size(20.dp))
                    }
                }
                DownloadStatus.COMPLETED -> { }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
