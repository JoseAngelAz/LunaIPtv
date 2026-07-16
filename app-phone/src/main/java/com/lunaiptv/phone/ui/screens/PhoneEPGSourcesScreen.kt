package com.lunaiptv.phone.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.core.epg.EpgSource
import com.lunaiptv.core.sync.work.EpgSyncState
import com.lunaiptv.features.settings.data.EpgAutoRefresh
import com.lunaiptv.phone.R

@Composable
private fun epgAutoRefreshLabel(mode: EpgAutoRefresh): String = stringResource(when (mode) {
    EpgAutoRefresh.OFF -> R.string.auto_refresh_off
    EpgAutoRefresh.STARTUP -> R.string.auto_refresh_startup
    EpgAutoRefresh.HOURS_1 -> R.string.auto_refresh_1_hour
    EpgAutoRefresh.HOURS_3 -> R.string.auto_refresh_3_hours
    EpgAutoRefresh.HOURS_6 -> R.string.auto_refresh_6_hours
    EpgAutoRefresh.HOURS_12 -> R.string.auto_refresh_12_hours
    EpgAutoRefresh.HOURS_24 -> R.string.auto_refresh_24_hours
    EpgAutoRefresh.HOURS_48 -> R.string.auto_refresh_48_hours
})

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEPGSourcesScreen(
    vm: com.lunaiptv.phone.di.PhoneEPGSourcesViewModel,
    onBack: () -> Unit,
) {
    val sources by vm.sources.collectAsStateWithLifecycle()
    val autoRefreshMap by vm.autoRefresh.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<EpgSource?>(null) }
    var adding by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<EpgSource?>(null) }

    if (adding || editing != null) {
        PhoneEpgSourceForm(
            initial = editing,
            initialAutoRefresh = editing?.let { autoRefreshMap[it.id] } ?: EpgAutoRefresh.OFF,
            onSave = { name, url, ua, autoRefresh ->
                val e = editing
                if (e == null) vm.add(name, url, ua, autoRefresh)
                else vm.update(e, name, url, ua)
                adding = false; editing = null
            },
            onCancel = { adding = false; editing = null },
        )
        return
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.epg_sources_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_epg))
                    }
                },
                scrollBehavior = scrollBehavior,
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
                    Text(
                        stringResource(R.string.no_epg_sources),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.add_xmltv_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { adding = true }) { Text(stringResource(R.string.add_epg)) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            ) {
                items(sources, key = { it.id }) { source ->
                    val syncState by remember(source.id) { vm.observeSync(source.id) }
                        .collectAsStateWithLifecycle(EpgSyncState.Idle)
                    val autoRefresh = autoRefreshMap[source.id] ?: EpgAutoRefresh.OFF
                    PhoneEpgSourceCard(
                        source = source,
                        autoRefresh = autoRefresh,
                        counts = { vm.counts(source.id) },
                        syncState = syncState,
                        onResync = { vm.resync(source) },
                        onCancelSync = { vm.cancelSync(source) },
                        onEdit = { editing = source },
                        onDelete = { confirmDelete = source },
                    )
                }
            }
        }
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_epg_source_title, s.name)) },
            text = {
                Text(stringResource(R.string.delete_epg_source_message))
            },
            confirmButton = {
                Button(
                    onClick = { vm.delete(s); confirmDelete = null },
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
private fun PhoneEpgSourceCard(
    source: EpgSource,
    autoRefresh: EpgAutoRefresh,
    counts: suspend () -> Triple<Int, Int, Int>,
    syncState: EpgSyncState,
    onResync: () -> Unit,
    onCancelSync: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val count by produceState<Triple<Int, Int, Int>?>(initialValue = null, source.id, source.lastSyncAt, source.lastError) {
        value = runCatching { counts() }.getOrNull()
    }
    val activeSync = syncState as? EpgSyncState.Syncing
    val syncPercent = activeSync?.let {
        if (it.baseProgrammes > 0 && it.programmes > 0) {
            ((it.programmes.toLong() * 100) / it.baseProgrammes).toInt().coerceAtMost(99)
        } else null
    }

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
                    Text(
                        source.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (activeSync != null) {
                        Spacer(Modifier.width(8.dp))
                        val syncBadge = syncPercent?.let { "$it%" } ?: stringResource(R.string.syncing_badge)
                        Text(
                            syncBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    if (autoRefresh != EpgAutoRefresh.OFF) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            epgAutoRefreshLabel(autoRefresh),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                val status: String = when {
                    activeSync != null -> when {
                        activeSync.programmes > 0 -> stringResource(R.string.channels_programmes, activeSync.channels, activeSync.programmes)
                        activeSync.channels > 0 -> stringResource(R.string.channels_count, activeSync.channels)
                        else -> stringResource(R.string.connecting)
                    }
                    source.lastError != null -> source.lastError ?: stringResource(R.string.error)
                    count != null && count!!.second > 0 -> {
                        val base = stringResource(R.string.channels_programmes, count!!.first, count!!.second)
                        val catchup = count?.third?.takeIf { it > 0 }?.let { stringResource(R.string.with_catchup, it) } ?: ""
                        base + catchup
                    }
                    source.lastSyncAt != null -> {
                        val catchup = count?.third?.takeIf { it > 0 }?.let { stringResource(R.string.with_catchup, it) } ?: ""
                        stringResource(R.string.synced_no_programmes) + catchup
                    }
                    else -> stringResource(R.string.not_synced)
                }
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (source.lastError != null && activeSync == null) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (syncState.isActive) {
                OutlinedButton(onClick = onCancelSync, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            } else {
                OutlinedButton(onClick = onResync, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.re_sync)) }
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.edit)) }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneEpgSourceForm(
    initial: EpgSource?,
    initialAutoRefresh: EpgAutoRefresh,
    onSave: (name: String, url: String, userAgent: String?, autoRefresh: EpgAutoRefresh) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var ua by remember { mutableStateOf(initial?.userAgent ?: "") }
    var autoRefresh by remember { mutableStateOf(initialAutoRefresh) }
    var showAutoRefreshPicker by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(if (initial == null) R.string.add_epg_source else R.string.edit_epg_source)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.xmltv_guide_feeds_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                placeholder = { Text(stringResource(R.string.name_example)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.xmltv_url)) },
                placeholder = { Text(stringResource(R.string.xmltv_url_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = ua,
                onValueChange = { ua = it },
                label = { Text(stringResource(R.string.user_agent_optional)) },
                placeholder = { Text(stringResource(R.string.leave_blank_default)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showAutoRefreshPicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.auto_refresh), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.auto_refresh_desc_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    epgAutoRefreshLabel(autoRefresh),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { onSave(name, url, ua, autoRefresh) },
                enabled = url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (initial == null) R.string.add_sync else R.string.save_sync))
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAutoRefreshPicker) {
        AlertDialog(
            onDismissRequest = { showAutoRefreshPicker = false },
            title = { Text(stringResource(R.string.auto_refresh)) },
            text = {
                Column {
                    EpgAutoRefresh.entries.forEach { mode ->
                        TextButton(onClick = {
                            autoRefresh = mode
                            showAutoRefreshPicker = false
                        }) {
                            Text(
                                epgAutoRefreshLabel(mode),
                                color = if (autoRefresh == mode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}
