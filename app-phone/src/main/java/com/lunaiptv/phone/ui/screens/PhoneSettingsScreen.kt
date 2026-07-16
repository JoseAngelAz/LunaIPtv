package com.lunaiptv.phone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.phone.R
import com.lunaiptv.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSettingsScreen(
    vm: com.lunaiptv.phone.di.PhoneSettingsViewModel,
    onBack: () -> Unit,
    onProfiles: () -> Unit = {},
    onManageSources: () -> Unit = {},
    onEpgSources: () -> Unit = {},
    onNetworkSettings: () -> Unit = {},
    onVideoPlayerSettings: () -> Unit = {},
    onBackup: () -> Unit = {},
    onDownloads: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val accent by vm.accent.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val livePreview by vm.livePreviewEnabled.collectAsStateWithLifecycle()
    val livePreviewAudio by vm.livePreviewAudio.collectAsStateWithLifecycle()
    val weatherEnabled by vm.weatherEnabled.collectAsStateWithLifecycle()
    val metadataMode by vm.metadataMode.collectAsStateWithLifecycle()
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Appearance ──────────────────────────────────
            SectionHeader(stringResource(R.string.appearance))

            SettingOption(
                title = stringResource(R.string.theme),
                subtitle = when (themeMode) {
                    ThemeMode.DARK -> stringResource(R.string.dark)
                    ThemeMode.LIGHT -> stringResource(R.string.light)
                    ThemeMode.SYSTEM -> stringResource(R.string.system_default)
                },
                onClick = {
                    val next = when (themeMode) {
                        ThemeMode.SYSTEM -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.SYSTEM
                    }
                    vm.setThemeMode(next)
                },
            )

            SettingOption(
                title = stringResource(R.string.accent),
                subtitle = accent.label,
                onClick = {
                    val colors = com.lunaiptv.ui.theme.AccentColor.entries
                    val idx = colors.indexOf(accent)
                    vm.setAccent(colors[(idx + 1) % colors.size])
                },
            )

            SettingOption(
                title = stringResource(R.string.language_setting),
                subtitle = when (language) {
                    "es" -> stringResource(R.string.language_es)
                    else -> stringResource(R.string.language_en)
                },
                onClick = {
                    vm.setLanguage(if (language == "en") "es" else "en")
                },
            )

            // ── Account ────────────────────────────────────
            SectionHeader(stringResource(R.string.account))

            SettingOption(
                title = stringResource(R.string.profiles),
                subtitle = stringResource(R.string.switch_manage_profiles),
                icon = Icons.Filled.Person,
                onClick = onProfiles,
            )

            // ── Content ────────────────────────────────────
            SectionHeader(stringResource(R.string.content))

            SettingOption(
                title = stringResource(R.string.manage_sources),
                subtitle = stringResource(R.string.add_edit_remove_sources),
                onClick = onManageSources,
            )

            SettingOption(
                title = stringResource(R.string.epg_sources),
                subtitle = stringResource(R.string.manage_epg_feeds),
                onClick = onEpgSources,
            )

            // ── Playback ────────────────────────────────────
            SectionHeader(stringResource(R.string.live_tv_section))

            SettingToggle(
                title = stringResource(R.string.live_preview),
                subtitle = stringResource(R.string.show_live_preview),
                checked = livePreview,
                onCheckedChange = vm::setLivePreview,
            )

            SettingToggle(
                title = stringResource(R.string.preview_audio),
                subtitle = stringResource(R.string.play_audio_preview),
                checked = livePreviewAudio,
                onCheckedChange = vm::setLivePreviewAudio,
            )

            // ── Video Player ────────────────────────────────
            SectionHeader(stringResource(R.string.video_player))

            SettingOption(
                title = stringResource(R.string.video_player_settings),
                subtitle = stringResource(R.string.decoder_zoom_subtitles_audio_resume),
                onClick = onVideoPlayerSettings,
            )

            // ── Network ─────────────────────────────────────
            SectionHeader(stringResource(R.string.network))

            SettingOption(
                title = stringResource(R.string.proxy),
                subtitle = stringResource(R.string.configure_http_proxy),
                onClick = onNetworkSettings,
            )

            // ── Weather ─────────────────────────────────────
            SectionHeader(stringResource(R.string.weather))

            SettingToggle(
                title = stringResource(R.string.weather_chip),
                subtitle = stringResource(R.string.show_weather_top_bar),
                checked = weatherEnabled,
                onCheckedChange = vm::setWeatherEnabled,
            )

            // ── Metadata ────────────────────────────────────
            SectionHeader(stringResource(R.string.metadata))

            SettingOption(
                title = stringResource(R.string.tmdb_enrichment),
                subtitle = when (metadataMode) {
                    com.lunaiptv.core.metadata.MetadataMode.PROVIDER -> stringResource(R.string.provider_only)
                    com.lunaiptv.core.metadata.MetadataMode.PROVIDER_PLUS_TMDB -> stringResource(R.string.provider_tmdb)
                    com.lunaiptv.core.metadata.MetadataMode.TMDB_ONLY -> stringResource(R.string.tmdb_only)
                },
                onClick = {
                    val next = when (metadataMode) {
                        com.lunaiptv.core.metadata.MetadataMode.PROVIDER -> com.lunaiptv.core.metadata.MetadataMode.PROVIDER_PLUS_TMDB
                        com.lunaiptv.core.metadata.MetadataMode.PROVIDER_PLUS_TMDB -> com.lunaiptv.core.metadata.MetadataMode.TMDB_ONLY
                        com.lunaiptv.core.metadata.MetadataMode.TMDB_ONLY -> com.lunaiptv.core.metadata.MetadataMode.PROVIDER
                    }
                    vm.setMetadataMode(next)
                },
            )

            // ── Backup ──────────────────────────────────────
            SectionHeader(stringResource(R.string.backup_restore))

            SettingOption(
                title = stringResource(R.string.backup_restore),
                subtitle = stringResource(R.string.export_import_settings),
                onClick = onBackup,
            )

            // ── Downloads ───────────────────────────────────
            SectionHeader(stringResource(R.string.downloads))

            SettingOption(
                title = stringResource(R.string.downloads),
                subtitle = stringResource(R.string.manage_downloaded_content),
                onClick = onDownloads,
            )

            // ── About ───────────────────────────────────────
            SectionHeader(stringResource(R.string.about))

            SettingOption(
                title = "LunaIPtv",
                subtitle = stringResource(R.string.version_info),
                onClick = { showAboutDialog = true },
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("LunaIPtv") },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.version_info),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lunaiptv_fork_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
