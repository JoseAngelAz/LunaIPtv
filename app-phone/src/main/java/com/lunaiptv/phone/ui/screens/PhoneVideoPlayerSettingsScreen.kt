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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.phone.di.PhoneSettingsViewModel

private enum class Dialog {
    NONE, ZOOM, SUB_SIZE, SUB_LANG, AUDIO_LANG, AUDIO_SYNC, RESUME
}

private data class ZoomModeOption(val name: String, val label: String)

private val ZOOM_MODES = listOf(
    ZoomModeOption("FIT", "Fit Screen"),
    ZoomModeOption("FILL", "Fill / Crop"),
    ZoomModeOption("STRETCH", "Stretch"),
    ZoomModeOption("ORIGINAL", "Original (1:1)"),
    ZoomModeOption("FORCE_16_9", "Force 16:9"),
    ZoomModeOption("FORCE_4_3", "Force 4:3"),
)

private val SUB_SIZES = listOf(0.8f to "Small", 1.0f to "Normal", 1.3f to "Large", 1.6f to "Extra Large")

private val LANGUAGES = listOf(
    "" to "None / Auto",
    "eng" to "English",
    "spa" to "Spanish",
    "fra" to "French",
    "deu" to "German",
    "ita" to "Italian",
    "por" to "Portuguese",
    "nld" to "Dutch",
    "rus" to "Russian",
    "ara" to "Arabic",
    "hin" to "Hindi",
    "zho" to "Chinese",
    "jpn" to "Japanese",
    "kor" to "Korean",
    "tur" to "Turkish",
)

private fun langName(code: String) =
    LANGUAGES.firstOrNull { it.first == code }?.second ?: code.ifBlank { "None / Auto" }

private fun subSizeName(scale: Float) =
    SUB_SIZES.minByOrNull { kotlin.math.abs(it.first - scale) }?.second ?: "Normal"

private fun zoomLabel(name: String) =
    ZOOM_MODES.firstOrNull { it.name == name }?.label ?: "Fit Screen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneVideoPlayerSettingsScreen(
    vm: PhoneSettingsViewModel,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hw by vm.hwDecoding.collectAsStateWithLifecycle()
    val vodExo by vm.vodPreferExo.collectAsStateWithLifecycle()
    val externalPlayer by vm.externalPlayer.collectAsStateWithLifecycle()
    val zoom by vm.defaultZoom.collectAsStateWithLifecycle()
    val resumeMode by vm.resumeMode.collectAsStateWithLifecycle()
    val subScale by vm.subtitleScale.collectAsStateWithLifecycle()
    val subLang by vm.preferredSubLang.collectAsStateWithLifecycle()
    val audioLang by vm.preferredAudioLang.collectAsStateWithLifecycle()
    val audioDelay by vm.audioDelayMs.collectAsStateWithLifecycle()
    val surroundSound by vm.surroundSound.collectAsStateWithLifecycle()
    val autoPlayNext by vm.autoPlayNext.collectAsStateWithLifecycle()
    val hdrEnabled by vm.hdrEnabled.collectAsStateWithLifecycle()

    var dialog by remember { mutableStateOf(Dialog.NONE) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Video Player") },
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
            // ── Decoding ──────────────────────────────────────
            SectionHeader("Decoding")

            SettingToggle(
                title = "HW Decoding",
                subtitle = "Use hardware decoding when available",
                checked = hw,
                onCheckedChange = vm::setHwDecoding,
            )

            SettingToggle(
                title = "VOD Player",
                subtitle = if (vodExo) "ExoPlayer" else "mpv",
                checked = vodExo,
                onCheckedChange = vm::setVodPreferExo,
            )

            SettingToggle(
                title = "External Player",
                subtitle = "Open videos in external player app",
                checked = externalPlayer,
                onCheckedChange = vm::setExternalPlayer,
            )

            SettingOption(
                title = "Default Zoom",
                subtitle = zoomLabel(zoom),
                onClick = { dialog = Dialog.ZOOM },
            )

            SettingOption(
                title = "Resume Mode",
                subtitle = runCatching { SettingsRepository.ResumeMode.valueOf(resumeMode.name).label }
                    .getOrDefault(resumeMode.name),
                onClick = { dialog = Dialog.RESUME },
            )

            // ── Subtitles ─────────────────────────────────────
            SectionHeader("Subtitles")

            SettingOption(
                title = "Subtitle Size",
                subtitle = subSizeName(subScale),
                onClick = { dialog = Dialog.SUB_SIZE },
            )

            SettingOption(
                title = "Subtitle Language",
                subtitle = langName(subLang),
                onClick = { dialog = Dialog.SUB_LANG },
            )

            // ── Audio ─────────────────────────────────────────
            SectionHeader("Audio")

            SettingOption(
                title = "Audio Language",
                subtitle = langName(audioLang),
                onClick = { dialog = Dialog.AUDIO_LANG },
            )

            SettingOption(
                title = "Audio Sync",
                subtitle = "%+d ms".format(audioDelay),
                onClick = { dialog = Dialog.AUDIO_SYNC },
            )

            // ── Other ─────────────────────────────────────────
            SectionHeader("Other")

            SettingToggle(
                title = "Surround Sound",
                subtitle = "Enable surround sound output",
                checked = surroundSound,
                onCheckedChange = vm::setSurroundSound,
            )

            SettingToggle(
                title = "Auto-play Next",
                subtitle = "Automatically play the next item",
                checked = autoPlayNext,
                onCheckedChange = vm::setAutoPlayNext,
            )

            SettingToggle(
                title = "HDR",
                subtitle = "Enable HDR rendering",
                checked = hdrEnabled,
                onCheckedChange = vm::setHdrEnabled,
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    when (dialog) {
        Dialog.ZOOM -> PickerDialog(
            title = "Default Zoom",
            options = ZOOM_MODES.map { it.name to it.label },
            selected = zoom,
            onSelect = { vm.setDefaultZoom(it); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.RESUME -> PickerDialog(
            title = "Resume Mode",
            options = SettingsRepository.ResumeMode.entries.map { it.name to it.label },
            selected = resumeMode.name,
            onSelect = { vm.setResumeMode(it); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.SUB_SIZE -> PickerDialog(
            title = "Subtitle Size",
            options = SUB_SIZES.map { it.first.toString() to it.second },
            selected = (SUB_SIZES.minByOrNull { kotlin.math.abs(it.first - subScale) }?.first ?: 1.0f).toString(),
            onSelect = { vm.setSubtitleScale(it.toFloat()); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.SUB_LANG -> LanguagePickerDialog(
            title = "Subtitle Language",
            selected = subLang,
            onSelect = { vm.setPreferredSubLang(it); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.AUDIO_LANG -> LanguagePickerDialog(
            title = "Audio Language",
            selected = audioLang,
            onSelect = { vm.setPreferredAudioLang(it); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.AUDIO_SYNC -> AudioSyncDialog(
            value = audioDelay,
            onSet = { vm.setAudioDelayMs(it) },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.NONE -> Unit
    }
}

// ── Reusable components ───────────────────────────────────────

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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

@Composable
private fun PickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(options, key = { it.first }) { (value, label) ->
                    val isSel = value == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSel) {
                            Text(
                                text = "\u2713",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun LanguagePickerDialog(
    title: String,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.height(400.dp)) {
                items(LANGUAGES, key = { it.first }) { (code, name) ->
                    val isSel = code == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSel) {
                            Text(
                                text = "\u2713",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun AudioSyncDialog(
    value: Int,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(value.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Sync") },
        text = {
            Column {
                Text(
                    text = "Delay in milliseconds (-2000 to +2000)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue == "-" || newValue.all { it == '-' || it.isDigit() }) {
                            text = newValue
                        }
                    },
                    label = { Text("ms") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ms = text.toIntOrNull()?.coerceIn(-2000, 2000) ?: value
                onSet(ms)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
