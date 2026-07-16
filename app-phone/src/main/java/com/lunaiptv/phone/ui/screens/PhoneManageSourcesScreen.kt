package com.lunaiptv.phone.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.core.database.entity.SourceEntity
import com.lunaiptv.core.model.SourceType
import com.lunaiptv.core.sync.SyncCounts
import com.lunaiptv.core.sync.work.CatalogSyncState
import com.lunaiptv.features.settings.data.PlaylistAutoRefresh
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneSourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneManageSourcesScreen(
    vm: PhoneSourceViewModel,
    onBack: () -> Unit,
    onAddSource: () -> Unit,
    onEditSource: (SourceEntity) -> Unit,
) {
    val sources by vm.sources.collectAsStateWithLifecycle()
    val defaultId by vm.defaultSourceId.collectAsStateWithLifecycle()
    val playlistAutoRefresh by vm.playlistAutoRefresh.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<SourceEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_sources_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddSource) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_source))
                    }
                },
            )
        },
    ) { padding ->
        if (sources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.no_sources), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.add_first_source), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onAddSource) { Text(stringResource(R.string.add_source)) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            ) {
                items(sources, key = { it.id }) { source ->
                    val isDefault = source.id == defaultId
                    val counts by remember(source.id) { vm.contentCounts(source.id) }.collectAsStateWithLifecycle(null)
                    val syncState by remember(source.id) { vm.syncState(source.id) }.collectAsStateWithLifecycle(CatalogSyncState.Idle)
                    SourceCard(
                        source = source,
                        autoRefresh = playlistAutoRefresh[source.id] ?: PlaylistAutoRefresh.OFF,
                        isDefault = isDefault,
                        counts = counts,
                        syncState = syncState,
                        onEdit = { onEditSource(source) },
                        onResync = { vm.resync(source) },
                        onCancelSync = { vm.cancelResync(source) },
                        onDelete = { confirmDelete = source },
                    )
                }
            }
        }
    }

    confirmDelete?.let { src ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_source_title_format, src.name)) },
            text = { Text(stringResource(R.string.delete_source_detail)) },
            confirmButton = {
                Button(
                    onClick = { vm.delete(src); confirmDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SourceCard(
    source: SourceEntity,
    autoRefresh: PlaylistAutoRefresh,
    isDefault: Boolean,
    counts: SyncCounts?,
    syncState: CatalogSyncState,
    onEdit: () -> Unit,
    onResync: () -> Unit,
    onCancelSync: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val activeSync = syncState as? CatalogSyncState.Syncing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(source.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (isDefault) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.source_default_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    activeSync?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            resyncBadgeText(context, it.baseItemCount, it.totalProcessed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    buildString {
                        append(when (source.type) {
                            SourceType.XTREAM -> "${context.getString(R.string.xtream_source)} \u00b7 ${source.url}"
                            SourceType.M3U -> "${context.getString(R.string.m3u_source)} \u00b7 ${source.url}"
                            SourceType.LOCAL_BACKUP -> context.getString(R.string.backup_source)
                        })
                        if (autoRefresh != PlaylistAutoRefresh.OFF) append(" \u00b7 ${autoRefresh.label}")
                        val label = countsLabel(context, source.type, activeSync, counts)
                        if (!label.isNullOrBlank()) {
                            append(" \u00b7 $label")
                        } else if (activeSync != null) {
                            append(" \u00b7 ${context.getString(R.string.syncing_source)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.edit)) }
            if (syncState.isActive) {
                OutlinedButton(onClick = onCancelSync, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            } else {
                OutlinedButton(onClick = onResync, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.resync)) }
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete)) }
        }
    }
}

private fun countsLabel(context: Context, sourceType: SourceType, activeSync: CatalogSyncState.Syncing?, stored: SyncCounts?): String? {
    fun visibleCount(active: Boolean, processed: Int, storedCount: Int): Int =
        if (active) processed else storedCount

    val live = activeSync?.let { visibleCount(it.liveActive, it.liveProcessed, stored?.channels ?: 0) } ?: (stored?.channels ?: 0)
    val movies = activeSync?.let { visibleCount(it.moviesActive, it.moviesProcessed, stored?.movies ?: 0) } ?: (stored?.movies ?: 0)
    val series = activeSync?.let { visibleCount(it.seriesActive, it.seriesProcessed, stored?.series ?: 0) } ?: (stored?.series ?: 0)

    val parts = buildList {
        if (live > 0) add(context.getString(R.string.count_channels_format, humanCount(live)))
        if (movies > 0) add(context.getString(R.string.count_movies_format, humanCount(movies)))
        if (series > 0) add(context.getString(R.string.count_series_format, humanCount(series)))
    }
    return parts.joinToString(" \u00b7 ").ifBlank { null }
}

private fun resyncBadgeText(context: Context, baseItemCount: Int, totalProcessed: Int): String =
    if (baseItemCount > 0 && totalProcessed > 0) {
        "${((totalProcessed * 100L) / baseItemCount).coerceIn(1, 99).toInt()}%"
    } else {
        context.getString(R.string.syncing_source)
    }

private fun humanCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).removeSuffix(".0M").let { if (it.endsWith("M")) it else "${it}M" }
    n >= 1_000 -> "%.1fK".format(n / 1_000.0).removeSuffix(".0K").let { if (it.endsWith("K")) it else "${it}K" }
    else -> n.toString()
}
