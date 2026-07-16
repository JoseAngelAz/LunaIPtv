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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneSettingsViewModel

private enum class Dialog {
    NONE, ZOOM, SUB_SIZE, SUB_LANG, AUDIO_LANG, AUDIO_SYNC, RESUME
}

private data class ZoomModeOption(val name: String, val labelRes: Int)

private val ZOOM_MODES = listOf(
    ZoomModeOption("FIT", R.string.fit_screen),
    ZoomModeOption("FILL", R.string.fill_crop),
    ZoomModeOption("STRETCH", R.string.stretch),
    ZoomModeOption("ORIGINAL", R.string.original_1_1),
    ZoomModeOption("FORCE_16_9", R.string.force_16_9),
    ZoomModeOption("FORCE_4_3", R.string.force_4_3),
)

private data class SubSizeOption(val scale: Float, val labelRes: Int)

private val SUB_SIZES = listOf(
    SubSizeOption(0.8f, R.string.small),
    SubSizeOption(1.0f, R.string.normal),
    SubSizeOption(1.3f, R.string.large),
    SubSizeOption(1.6f, R.string.extra_large),
)

private data class LanguageOption(val code: String, val labelRes: Int)

private val LANGUAGES = listOf(
    LanguageOption("", R.string.none_auto),
    LanguageOption("eng", R.string.lang_english),
    LanguageOption("spa", R.string.lang_spanish),
    LanguageOption("fra", R.string.lang_french),
    LanguageOption("deu", R.string.lang_german),
    LanguageOption("ita", R.string.lang_italian),
    LanguageOption("por", R.string.lang_portuguese),
    LanguageOption("nld", R.string.lang_dutch),
    LanguageOption("rus", R.string.lang_russian),
    LanguageOption("ara", R.string.lang_arabic),
    LanguageOption("hin", R.string.lang_hindi),
    LanguageOption("zho", R.string.lang_chinese),
    LanguageOption("jpn", R.string.lang_japanese),
    LanguageOption("kor", R.string.lang_korean),
    LanguageOption("tur", R.string.lang_turkish),
)

@Composable
private fun langName(code: String): String =
    stringResource(LANGUAGES.firstOrNull { it.code == code }?.labelRes ?: R.string.none_auto)

@Composable
private fun subSizeName(scale: Float): String =
    stringResource(SUB_SIZES.minByOrNull { kotlin.math.abs(it.scale - scale) }?.labelRes ?: R.string.normal)

@Composable
private fun zoomLabel(name: String): String =
    stringResource(ZOOM_MODES.firstOrNull { it.name == name }?.labelRes ?: R.string.fit_screen)

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
                title = { Text(stringResource(R.string.video_player)) },
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
            // ── Decoding ──────────────────────────────────────
            SectionHeader(stringResource(R.string.decoding))

            SettingToggle(
                title = stringResource(R.string.hw_decoding),
                subtitle = stringResource(R.string.use_hw_decoding),
                checked = hw,
                onCheckedChange = vm::setHwDecoding,
            )

            SettingToggle(
                title = stringResource(R.string.vod_player),
                subtitle = if (vodExo) "ExoPlayer" else "mpv",
                checked = vodExo,
                onCheckedChange = vm::setVodPreferExo,
            )

            SettingToggle(
                title = stringResource(R.string.external_player),
                subtitle = stringResource(R.string.open_in_external),
                checked = externalPlayer,
                onCheckedChange = vm::setExternalPlayer,
            )

            SettingOption(
                title = stringResource(R.string.default_zoom),
                subtitle = zoomLabel(zoom),
                onClick = { dialog = Dialog.ZOOM },
            )

            SettingOption(
                title = stringResource(R.string.resume_mode),
                subtitle = stringResource(when (runCatching { SettingsRepository.ResumeMode.valueOf(resumeMode.name) }.getOrNull()) {
                    SettingsRepository.ResumeMode.AUTO -> R.string.resume_always
                    SettingsRepository.ResumeMode.NEVER -> R.string.resume_never
                    else -> R.string.resume_ask
                }),
                onClick = { dialog = Dialog.RESUME },
            )

            // ── Subtitles ─────────────────────────────────────
            SectionHeader(stringResource(R.string.subtitles_section))

            SettingOption(
                title = stringResource(R.string.subtitle_size),
                subtitle = subSizeName(subScale),
                onClick = { dialog = Dialog.SUB_SIZE },
            )

            SettingOption(
                title = stringResource(R.string.subtitle_language),
                subtitle = langName(subLang),
                onClick = { dialog = Dialog.SUB_LANG },
            )

            // ── Audio ─────────────────────────────────────────
            SectionHeader(stringResource(R.string.audio_section))

            SettingOption(
                title = stringResource(R.string.audio_language),
                subtitle = langName(audioLang),
                onClick = { dialog = Dialog.AUDIO_LANG },
            )

            SettingOption(
                title = stringResource(R.string.audio_sync),
                subtitle = "%+d ${stringResource(R.string.ms_label)}".format(audioDelay),
                onClick = { dialog = Dialog.AUDIO_SYNC },
            )

            // ── Other ─────────────────────────────────────────
            SectionHeader(stringResource(R.string.other))

            SettingToggle(
                title = stringResource(R.string.surround_sound),
                subtitle = stringResource(R.string.enable_surround),
                checked = surroundSound,
                onCheckedChange = vm::setSurroundSound,
            )

            SettingToggle(
                title = stringResource(R.string.auto_play_next),
                subtitle = stringResource(R.string.auto_play_next_description),
                checked = autoPlayNext,
                onCheckedChange = vm::setAutoPlayNext,
            )

            SettingToggle(
                title = stringResource(R.string.hdr),
                subtitle = stringResource(R.string.enable_hdr),
                checked = hdrEnabled,
                onCheckedChange = vm::setHdrEnabled,
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    when (dialog) {
        Dialog.ZOOM -> PickerDialog(
            title = stringResource(R.string.default_zoom),
            options = ZOOM_MODES.map { it.name to stringResource(it.labelRes) },
            selected = zoom,
            onSelect = { vm.setDefaultZoom(it); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.RESUME -> PickerDialog(
            title = stringResource(R.string.resume_mode),
            options = SettingsRepository.ResumeMode.entries.map { it.name to stringResource(when (it) {
                SettingsRepository.ResumeMode.AUTO -> R.string.resume_always
                SettingsRepository.ResumeMode.NEVER -> R.string.resume_never
                else -> R.string.resume_ask
            }) },
            selected = resumeMode.name,
            onSelect = { vm.setResumeMode(it); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.SUB_SIZE -> PickerDialog(
            title = stringResource(R.string.subtitle_size),
            options = SUB_SIZES.map { it.scale.toString() to stringResource(it.labelRes) },
            selected = (SUB_SIZES.minByOrNull { kotlin.math.abs(it.scale - subScale) }?.scale ?: 1.0f).toString(),
            onSelect = { vm.setSubtitleScale(it.toFloat()); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.SUB_LANG -> LanguagePickerDialog(
            title = stringResource(R.string.subtitle_language),
            selected = subLang,
            onSelect = { vm.setPreferredSubLang(it); dialog = Dialog.NONE },
            onDismiss = { dialog = Dialog.NONE },
        )
        Dialog.AUDIO_LANG -> LanguagePickerDialog(
            title = stringResource(R.string.audio_language),
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
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
                items(LANGUAGES, key = { it.code }) { (code, labelRes) ->
                    val isSel = code == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(labelRes),
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
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
        title = { Text(stringResource(R.string.audio_sync)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.delay_ms_description),
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
                    label = { Text(stringResource(R.string.ms_label)) },
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
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
