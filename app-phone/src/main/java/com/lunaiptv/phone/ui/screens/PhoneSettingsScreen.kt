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
import androidx.compose.material.icons.filled.Person
import com.lunaiptv.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSettingsScreen(
    vm: com.lunaiptv.phone.di.PhoneSettingsViewModel,
    onBack: () -> Unit,
    onProfiles: () -> Unit = {},
    onManageSources: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val accent by vm.accent.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val livePreview by vm.livePreviewEnabled.collectAsStateWithLifecycle()
    val livePreviewAudio by vm.livePreviewAudio.collectAsStateWithLifecycle()
    val externalPlayer by vm.externalPlayer.collectAsStateWithLifecycle()

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

            // ── Playback ────────────────────────────────────
            SectionHeader("Playback")

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

            SettingToggle(
                title = "External player",
                subtitle = "Open videos in external player app",
                checked = externalPlayer,
                onCheckedChange = vm::setExternalPlayer,
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
