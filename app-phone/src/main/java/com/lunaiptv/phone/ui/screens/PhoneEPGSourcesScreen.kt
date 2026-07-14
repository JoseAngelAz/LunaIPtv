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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.core.epg.EpgSource
import com.lunaiptv.core.sync.work.EpgSyncState
import com.lunaiptv.features.settings.data.EpgAutoRefresh

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
                title = { Text("EPG Sources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add EPG")
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
                        "No EPG sources yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add an XMLTV link to fill the TV guide with programmes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { adding = true }) { Text("Add EPG") }
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
            title = { Text("Delete \"${s.name}\"?") },
            text = {
                Text("Removes this EPG source and its guide data. Your playlists are not affected.")
            },
            confirmButton = {
                Button(
                    onClick = { vm.delete(s); confirmDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
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
                        val syncBadge = syncPercent?.let { "$it%" } ?: "Syncing"
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
                            autoRefresh.label,
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
                val catchupNote = count?.third?.takeIf { it > 0 }?.let { " \u00b7 $it with catch-up" } ?: ""
                val status: String = when {
                    activeSync != null -> when {
                        activeSync.programmes > 0 -> "${activeSync.channels} channels \u00b7 ${activeSync.programmes} programmes"
                        activeSync.channels > 0 -> "${activeSync.channels} channels"
                        else -> "Connecting\u2026"
                    }
                    source.lastError != null -> source.lastError ?: "Error"
                    count != null && count!!.second > 0 -> "${count!!.first} channels \u00b7 ${count!!.second} programmes$catchupNote"
                    source.lastSyncAt != null -> "Synced, no programmes in window$catchupNote"
                    else -> "Not synced yet"
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
                OutlinedButton(onClick = onCancelSync, modifier = Modifier.weight(1f)) { Text("Cancel") }
            } else {
                OutlinedButton(onClick = onResync, modifier = Modifier.weight(1f)) { Text("Re-sync") }
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
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
                title = { Text(if (initial == null) "Add EPG Source" else "Edit EPG Source") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                "XMLTV guide feeds fill the TV guide with programmes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. UK Guide") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("XMLTV URL") },
                placeholder = { Text("https://…/epg.xml(.gz)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = ua,
                onValueChange = { ua = it },
                label = { Text("User-Agent (optional)") },
                placeholder = { Text("Leave blank for default") },
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
                    Text("Auto refresh", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Off, on startup, or when data is stale",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    autoRefresh.label,
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
                Text(if (initial == null) "Add & Sync" else "Save & Sync")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAutoRefreshPicker) {
        AlertDialog(
            onDismissRequest = { showAutoRefreshPicker = false },
            title = { Text("Auto refresh") },
            text = {
                Column {
                    EpgAutoRefresh.entries.forEach { mode ->
                        TextButton(onClick = {
                            autoRefresh = mode
                            showAutoRefreshPicker = false
                        }) {
                            Text(
                                mode.label,
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
