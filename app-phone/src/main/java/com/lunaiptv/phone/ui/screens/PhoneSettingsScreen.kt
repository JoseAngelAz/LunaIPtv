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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val accent by vm.accent.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val livePreview by vm.livePreviewEnabled.collectAsStateWithLifecycle()
    val livePreviewAudio by vm.livePreviewAudio.collectAsStateWithLifecycle()
    val weatherEnabled by vm.weatherEnabled.collectAsStateWithLifecycle()
    val metadataMode by vm.metadataMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Appearance ──────────────────────────────────
            SectionHeader("Appearance")

            SettingOption(
                title = "Theme",
                subtitle = when (themeMode) {
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.SYSTEM -> "System default"
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
                title = "Accent",
                subtitle = accent.label,
                onClick = {
                    val colors = com.lunaiptv.ui.theme.AccentColor.entries
                    val idx = colors.indexOf(accent)
                    vm.setAccent(colors[(idx + 1) % colors.size])
                },
            )

            SettingOption(
                title = "Language",
                subtitle = when (language) {
                    "es" -> "Español"
                    else -> "English"
                },
                onClick = {
                    vm.setLanguage(if (language == "en") "es" else "en")
                },
            )

            // ── Account ────────────────────────────────────
            SectionHeader("Account")

            SettingOption(
                title = "Profiles",
                subtitle = "Switch or manage profiles",
                icon = Icons.Filled.Person,
                onClick = onProfiles,
            )

            // ── Content ────────────────────────────────────
            SectionHeader("Content")

            SettingOption(
                title = "Manage Sources",
                subtitle = "Add, edit or remove IPTV sources",
                onClick = onManageSources,
            )

            SettingOption(
                title = "EPG Sources",
                subtitle = "Manage electronic program guide feeds",
                onClick = onEpgSources,
            )

            // ── Playback ────────────────────────────────────
            SectionHeader("Live TV")

            SettingToggle(
                title = "Live preview",
                subtitle = "Show live TV preview in channel list",
                checked = livePreview,
                onCheckedChange = vm::setLivePreview,
            )

            SettingToggle(
                title = "Preview audio",
                subtitle = "Play audio during live preview",
                checked = livePreviewAudio,
                onCheckedChange = vm::setLivePreviewAudio,
            )

            // ── Video Player ────────────────────────────────
            SectionHeader("Video Player")

            SettingOption(
                title = "Video Player Settings",
                subtitle = "Decoder, zoom, subtitles, audio, resume",
                onClick = onVideoPlayerSettings,
            )

            // ── Network ─────────────────────────────────────
            SectionHeader("Network")

            SettingOption(
                title = "Proxy",
                subtitle = "Configure HTTP proxy for all traffic",
                onClick = onNetworkSettings,
            )

            // ── Weather ─────────────────────────────────────
            SectionHeader("Weather")

            SettingToggle(
                title = "Weather chip",
                subtitle = "Show weather in the top bar",
                checked = weatherEnabled,
                onCheckedChange = vm::setWeatherEnabled,
            )

            // ── Metadata ────────────────────────────────────
            SectionHeader("Metadata")

            SettingOption(
                title = "TMDB enrichment",
                subtitle = when (metadataMode) {
                    com.lunaiptv.core.metadata.MetadataMode.PROVIDER -> "Provider only (no TMDB)"
                    com.lunaiptv.core.metadata.MetadataMode.PROVIDER_PLUS_TMDB -> "Provider + TMDB"
                    com.lunaiptv.core.metadata.MetadataMode.TMDB_ONLY -> "TMDB only"
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
            SectionHeader("Backup & Restore")

            SettingOption(
                title = "Backup / Restore",
                subtitle = "Export or import app settings",
                onClick = onBackup,
            )

            // ── About ───────────────────────────────────────
            SectionHeader("About")

            SettingOption(
                title = "LunaIPtv",
                subtitle = "Version 1.0.0 · Forked from OwnTV",
                onClick = { },
            )

            Spacer(Modifier.height(32.dp))
        }
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
