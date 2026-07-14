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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lunaiptv.core.database.entity.SourceEntity
import com.lunaiptv.core.model.SourceType
import com.lunaiptv.core.sync.ImportStage
import com.lunaiptv.features.settings.data.PlaylistAutoRefresh
import com.lunaiptv.phone.di.PhoneSourceViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class SourceKind { XTREAM, M3U }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAddSourceScreen(
    vm: PhoneSourceViewModel,
    onBack: () -> Unit,
    editing: SourceEntity? = null,
    initialAutoRefresh: PlaylistAutoRefresh = PlaylistAutoRefresh.OFF,
    initialIsDefault: Boolean = false,
    showDefaultToggle: Boolean = true,
) {
    val importState by vm.importState.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()

    when (val s = importState) {
        is PhoneSourceViewModel.ImportState.Running -> {
            ImportProgressScreen(progress = progress, onCancel = { vm.cancelImport(); onBack() })
            return
        }
        is PhoneSourceViewModel.ImportState.Success -> {
            LaunchedEffect(s) { vm.resetImport(); onBack() }
            return
        }
        is PhoneSourceViewModel.ImportState.Failed -> {
            ImportErrorScreen(message = s.message, onRetry = { vm.resetImport() }, onBack = { vm.resetImport(); onBack() })
            return
        }
        else -> { /* Idle — show form */ }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val editingMode = editing != null
    var kind by remember { mutableStateOf(if (editing?.type == SourceType.M3U) SourceKind.M3U else SourceKind.XTREAM) }
    var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var server by remember(editing) { mutableStateOf(if (editing != null && editing.type == SourceType.XTREAM) editing.url else "") }
    var username by remember(editing) { mutableStateOf(editing?.username ?: "") }
    var password by remember(editing) { mutableStateOf(editing?.password ?: "") }
    var m3uUrl by remember(editing) { mutableStateOf(if (editing != null && editing.type == SourceType.M3U) editing.url else "") }
    var epgUrl by remember(editing) { mutableStateOf(editing?.epgUrl ?: "") }
    var userAgent by remember(editing) { mutableStateOf(editing?.userAgent ?: "") }
    var autoRefresh by remember(initialAutoRefresh) { mutableStateOf(initialAutoRefresh) }
    var isDefault by remember(initialIsDefault) { mutableStateOf(initialIsDefault) }
    var syncLive by remember { mutableStateOf(true) }
    var syncMovies by remember { mutableStateOf(true) }
    var syncSeries by remember { mutableStateOf(true) }
    var showAutoRefreshPicker by remember { mutableStateOf(false) }

    val showContentToggles = kind == SourceKind.XTREAM && !editingMode
    val canStart = when (kind) {
        SourceKind.XTREAM -> server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && (syncLive || syncMovies || syncSeries)
        SourceKind.M3U -> m3uUrl.isNotBlank()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(if (editingMode) "Edit Source" else "Add Source") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                if (editingMode) "Update your source settings" else "Connect your IPTV provider",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            SourceTypeSelector(
                selected = kind,
                enabled = !editingMode,
                onSelect = { kind = it },
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (optional)") },
                placeholder = { Text("My IPTV") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))

            when (kind) {
                SourceKind.XTREAM -> {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://host:port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    val pwLabel = if (editingMode) "Password (keep blank to keep current)" else "Password"
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(pwLabel) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SourceKind.M3U -> {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text("Playlist URL") },
                        placeholder = { Text("http://…/playlist.m3u") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = userAgent,
                onValueChange = { userAgent = it },
                label = { Text("User Agent (optional)") },
                placeholder = { Text("e.g. VLC/3.0.20") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = epgUrl,
                onValueChange = { epgUrl = it },
                label = { Text("EPG URL (optional)") },
                placeholder = { Text("http://…/xmltv.php") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            SettingToggle(
                title = "Auto-refresh playlist",
                subtitle = autoRefresh.label,
                checked = autoRefresh != PlaylistAutoRefresh.OFF,
                onCheckedChange = {
                    autoRefresh = if (it) PlaylistAutoRefresh.HOURS_12 else PlaylistAutoRefresh.OFF
                },
            )

            if (showDefaultToggle) {
                Spacer(Modifier.height(8.dp))
                SettingToggle(
                    title = "Default playlist",
                    subtitle = "Use as primary content source",
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                )
            }

            if (showContentToggles) {
                Spacer(Modifier.height(20.dp))
                Text("First sync — choose content to import", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("You can sync the rest later from the source list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                SettingToggle(title = "Live TV", subtitle = "Channels and live streams", checked = syncLive, onCheckedChange = { syncLive = it })
                Spacer(Modifier.height(4.dp))
                SettingToggle(title = "Movies", subtitle = "Video on demand library", checked = syncMovies, onCheckedChange = { syncMovies = it })
                Spacer(Modifier.height(4.dp))
                SettingToggle(title = "Series", subtitle = "TV series catalogue", checked = syncSeries, onCheckedChange = { syncSeries = it })
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    when (kind) {
                        SourceKind.XTREAM -> vm.addXtream(name, server, username, password, userAgent, epgUrl, autoRefresh, syncLive, syncMovies, syncSeries, isDefault)
                        SourceKind.M3U -> vm.addM3u(name, m3uUrl, userAgent, epgUrl, autoRefresh, isDefault)
                    }
                },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (editingMode) "Save" else "Start Import")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAutoRefreshPicker) {
        AlertDialog(
            onDismissRequest = { showAutoRefreshPicker = false },
            title = { Text("Auto-refresh playlist") },
            text = {
                Column {
                    PlaylistAutoRefresh.entries.forEach { mode ->
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

@Composable
private fun SourceTypeSelector(
    selected: SourceKind,
    enabled: Boolean,
    onSelect: (SourceKind) -> Unit,
) {
    Column {
        Text("Source type", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = selected == SourceKind.XTREAM,
                onClick = { if (enabled) onSelect(SourceKind.XTREAM) },
                label = { Text("Xtream Codes") },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
            FilterChip(
                selected = selected == SourceKind.M3U,
                onClick = { if (enabled) onSelect(SourceKind.M3U) },
                label = { Text("M3U Playlist") },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun ImportProgressScreen(progress: ImportStage?, onCancel: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 24.dp))
            Text("Importing catalog…", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            val live = progress?.liveProcessed ?: 0
            val movies = progress?.moviesProcessed ?: 0
            val series = progress?.seriesProcessed ?: 0
            val parts = buildList {
                if (live > 0) add("$live channels")
                if (movies > 0) add("$movies movies")
                if (series > 0) add("$series series")
            }
            val label = parts.joinToString(" · ").ifBlank { "Preparing catalog" }
            Text(label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun ImportErrorScreen(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Import failed", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = onRetry) { Text("Try Again") }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
