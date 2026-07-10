package com.lunaiptv.features.shell.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lunaiptv.R
import com.lunaiptv.features.customize.CustomizeScreen
import com.lunaiptv.features.settings.HomeSettingsScreen
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.features.update.UpdateDialog
import com.lunaiptv.features.settings.BackupScreen
import com.lunaiptv.features.settings.ManageProfilesScreen
import com.lunaiptv.features.settings.ManageSourcesScreen
import com.lunaiptv.features.settings.SettingsViewModel
import com.lunaiptv.features.settings.VideoPlayerSettingsScreen
import com.lunaiptv.features.shell.MainSection
import com.lunaiptv.ui.components.BrowseMode
import com.lunaiptv.ui.components.FocusableSurface
import com.lunaiptv.ui.components.LunaIPtvTextField
import com.lunaiptv.ui.components.LunaIPtvButton
import com.lunaiptv.ui.components.LunaIPtvButtonStyle
import com.lunaiptv.ui.components.LunaIPtvIcon
import com.lunaiptv.ui.components.ContentPanelFill
import com.lunaiptv.ui.components.roundedPanel
import com.lunaiptv.ui.components.StorageBrowser
import com.lunaiptv.ui.theme.Dimens
import com.lunaiptv.ui.theme.LunaIPtvTheme
import com.lunaiptv.ui.theme.ThemeMode
import com.lunaiptv.ui.theme.UiZoom

private enum class TileTone { PRIMARY, SECONDARY, TERTIARY }

private enum class SettingsTab { ROOT, SOURCES, EPG, PROFILES, BACKUP, VIDEO, CUSTOMIZE, HOME, NETWORK, METADATA, WEATHER }

/**
 * The MD3 Settings screen (shown when [MainSection.SETTINGS] is active): grouped sections, each row
 * a tonal icon tile + title/description + a trailing chip or chevron. Theme / UI Zoom are live;
 * unfinished features show a "Soon" chip.
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    uiZoomPercent: Int,
    onSetZoom: (Int) -> Unit,
    onOpenPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    openEpgAdd: Boolean = false,
    onEpgAddConsumed: () -> Unit = {},
    language: String = "en",
    onSetLanguage: (String) -> Unit = {},
) {
    var tab by remember { mutableStateOf(SettingsTab.ROOT) }
    // Deep-link from the Guide's "Add EPG" button: jump straight to EPG Sources in add mode.
    var consumeEpgAdd by remember { mutableStateOf(false) }
    var showZoom by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showAccent by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showCatchupTime by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    var showAnimations by remember { mutableStateOf(false) }
    var showStartup by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }

    // Batch 4 · Settings search + quick toggles. Empty query = normal grouped list; a non-blank
    // query swaps the list for flat results that carry their group context ("Playback › HDR").
    var searchQuery by remember { mutableStateOf("") }
    val searchFieldFocus = remember { FocusRequester() }
    // While searching, Back clears the query (and returns focus to the field) instead of leaving Settings.
    BackHandler(enabled = tab == SettingsTab.ROOT && searchQuery.isNotBlank()) {
        searchQuery = ""
        runCatching { searchFieldFocus.requestFocus() }
    }

    // Dialog-close focus return: closing a dialog/picker refocuses the row that opened it (focus
    // would otherwise fall spatially back to the sidebar).
    val folderRowFocus = remember { FocusRequester() }
    val themeRowFocus = remember { FocusRequester() }
    val accentRowFocus = remember { FocusRequester() }
    val zoomRowFocus = remember { FocusRequester() }
    val updateRowFocus = remember { FocusRequester() }
    val aboutRowFocus = remember { FocusRequester() }
    val catchupRowFocus = remember { FocusRequester() }
    val clearHistoryRowFocus = remember { FocusRequester() }
    val animationsRowFocus = remember { FocusRequester() }
    val startupRowFocus = remember { FocusRequester() }
    val languageRowFocus = remember { FocusRequester() }
    // NOTE: this restore request crosses INTO the root focus group from outside (the dialog), so
    // the group's onEnter intercepts it — onEnter must consult dialogReturn first (it does, below)
    // or it would hijack the restore to its own default target. dialogReturn is cleared by onEnter.
    var dialogReturn by remember { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(showZoom, showTheme, showAccent, showFolderPicker, showUpdate, showAbout, showCatchupTime, showClearHistory, showAnimations, showStartup, showLanguage) {
        if (!showZoom && !showTheme && !showAccent && !showFolderPicker && !showUpdate && !showAbout && !showCatchupTime && !showClearHistory && !showAnimations && !showStartup && !showLanguage) {
            dialogReturn?.let { row ->
                kotlinx.coroutines.delay(80)
                runCatching { row.requestFocus() }
            }
        }
    }
    val settingsVm: SettingsViewModel = koinViewModel()
    val downloadRoot by settingsVm.downloadRoot.collectAsStateWithLifecycle()
    val livePreview by settingsVm.livePreviewEnabled.collectAsStateWithLifecycle()
    val previewAudio by settingsVm.livePreviewAudio.collectAsStateWithLifecycle()
    val hdr by settingsVm.hdrEnabled.collectAsStateWithLifecycle()
    val surroundSound by settingsVm.surroundSound.collectAsStateWithLifecycle()
    val autoPlayNext by settingsVm.autoPlayNext.collectAsStateWithLifecycle()
    val updateCheckOnStart by settingsVm.updateCheckOnStart.collectAsStateWithLifecycle()
    val catchupTz by settingsVm.catchupTimezone.collectAsStateWithLifecycle()
    val catchupOffset by settingsVm.catchupOffsetMinutes.collectAsStateWithLifecycle()
    val catchupChannels by settingsVm.catchupChannelCount.collectAsStateWithLifecycle()
    val accent by settingsVm.accent.collectAsStateWithLifecycle()
    val customAccent by settingsVm.customAccent.collectAsStateWithLifecycle()
    val animationLevel by settingsVm.animationLevel.collectAsStateWithLifecycle()
    val weatherEnabled by settingsVm.weatherEnabled.collectAsStateWithLifecycle()
    val startupMode by settingsVm.startupMode.collectAsStateWithLifecycle()

    // Restore focus to the row a sub-screen was opened from when the user navigates back.
    var lastTab by remember { mutableStateOf<SettingsTab?>(null) }
    val rowFocus = remember { mapOf(
        SettingsTab.SOURCES to FocusRequester(),
        SettingsTab.EPG to FocusRequester(),
        SettingsTab.PROFILES to FocusRequester(),
        SettingsTab.BACKUP to FocusRequester(),
        SettingsTab.VIDEO to FocusRequester(),
        SettingsTab.CUSTOMIZE to FocusRequester(),
        SettingsTab.HOME to FocusRequester(),
        SettingsTab.NETWORK to FocusRequester(),
        SettingsTab.METADATA to FocusRequester(),
        SettingsTab.WEATHER to FocusRequester(),
    ) }
    val open: (SettingsTab) -> Unit = { lastTab = it; tab = it }
    LaunchedEffect(tab) {
        if (tab == SettingsTab.ROOT && lastTab != null) {
            kotlinx.coroutines.delay(60)
            runCatching { rowFocus[lastTab]?.requestFocus() }
        }
    }
    LaunchedEffect(openEpgAdd) {
        if (openEpgAdd) { consumeEpgAdd = true; open(SettingsTab.EPG); onEpgAddConsumed() }
    }

    when (tab) {
        SettingsTab.SOURCES -> { ManageSourcesScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.EPG -> { com.lunaiptv.features.settings.EpgSourcesScreen(onBack = { tab = SettingsTab.ROOT; consumeEpgAdd = false }, modifier = modifier, startOnAdd = consumeEpgAdd); return }
        SettingsTab.PROFILES -> { ManageProfilesScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.BACKUP -> { BackupScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.VIDEO -> { VideoPlayerSettingsScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.CUSTOMIZE -> { CustomizeScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.HOME -> { HomeSettingsScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.NETWORK -> { com.lunaiptv.features.settings.NetworkSettingsScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.METADATA -> { com.lunaiptv.features.settings.MetadataSettingsScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.WEATHER -> { com.lunaiptv.features.settings.WeatherSettingsScreen(onBack = { tab = SettingsTab.ROOT }, modifier = modifier); return }
        SettingsTab.ROOT -> Unit
    }

    val colors = LunaIPtvTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedPanel(fillColor = ContentPanelFill)
            // onEnter fires for ANY focus entry from outside the group: D-pad entry from the
            // sidebar, but ALSO our own programmatic dialog-close restores (the dialogs live
            // outside this group). So it must route to the pending dialog-return row first, then
            // the last-opened sub-menu's row, then the first row.
            .focusProperties {
                onEnter = {
                    val target = dialogReturn ?: rowFocus[lastTab] ?: rowFocus.getValue(SettingsTab.PROFILES)
                    dialogReturn = null
                    runCatching { target.requestFocus() }
                }
            }
            .focusGroup()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        // --- Batch 4: quick toggles (most-used settings, one-press) ---
        // NOTE: the four here are the current "most-used" set; making this list user-configurable
        // is deferred (see DESIGN_PLAN_v4.0.3 Batch 4 · B).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickToggleChip(stringResource(R.string.settings_live_preview), livePreview, LunaIPtvIcon.LIVE_TV) { settingsVm.setLivePreviewEnabled(!livePreview) }
            QuickToggleChip(stringResource(R.string.settings_preview_audio), previewAudio, LunaIPtvIcon.AUDIO) { settingsVm.setLivePreviewAudio(!previewAudio) }
            QuickToggleChip(stringResource(R.string.settings_hdr), hdr, LunaIPtvIcon.VIDEO) { settingsVm.setHdrEnabled(!hdr) }
            QuickToggleChip(stringResource(R.string.settings_autoplay), autoPlayNext, LunaIPtvIcon.SKIP_NEXT) { settingsVm.setAutoPlayNext(!autoPlayNext) }
            QuickToggleChip(stringResource(R.string.settings_updates), updateCheckOnStart, LunaIPtvIcon.DOWNLOADS) { settingsVm.setUpdateCheckOnStart(!updateCheckOnStart) }
        }
        Spacer(Modifier.height(8.dp))

        // --- Batch 4: settings search ---
        LunaIPtvTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = stringResource(R.string.settings_search),
            placeholder = stringResource(R.string.settings_search_placeholder),
            focusRequester = searchFieldFocus,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))

        if (searchQuery.isBlank()) {
        GroupLabel(stringResource(R.string.settings_group_profile))
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.PERSON,
            title = stringResource(R.string.settings_profiles), desc = stringResource(R.string.settings_profiles_desc),
            onClick = { open(SettingsTab.PROFILES) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.PROFILES)),
        )
        SectionDivider()
        GroupLabel(stringResource(R.string.settings_group_content))
        SettingsRow(
            tone = TileTone.PRIMARY, icon = LunaIPtvIcon.PLAYLIST,
            title = stringResource(R.string.settings_playlists), desc = stringResource(R.string.settings_playlists_desc),
            onClick = { open(SettingsTab.SOURCES) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.SOURCES)),
        )
        SettingsRow(
            tone = TileTone.PRIMARY, icon = LunaIPtvIcon.EPG,
            title = stringResource(R.string.settings_epg_sources), desc = stringResource(R.string.settings_epg_sources_desc),
            onClick = { open(SettingsTab.EPG) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.EPG)),
        )
        SettingsRow(
            tone = TileTone.PRIMARY, icon = LunaIPtvIcon.SORT,
            title = stringResource(R.string.settings_customize), desc = stringResource(R.string.settings_customize_desc),
            onClick = { open(SettingsTab.CUSTOMIZE) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.CUSTOMIZE)),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.HOME,
            title = stringResource(R.string.settings_home_screen), desc = stringResource(R.string.settings_home_screen_desc),
            onClick = { open(SettingsTab.HOME) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.HOME)),
        )
        SettingsRow(
            tone = TileTone.PRIMARY, icon = LunaIPtvIcon.VIDEO,
            title = stringResource(R.string.settings_metadata), desc = stringResource(R.string.settings_metadata_desc),
            onClick = { open(SettingsTab.METADATA) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.METADATA)),
        )
        SettingsRow(
            tone = TileTone.TERTIARY, icon = LunaIPtvIcon.DOWNLOADS,
            title = stringResource(R.string.settings_download_folder),
            chip = downloadRoot.ifBlank { stringResource(R.string.settings_download_folder_default) }.let { java.io.File(it).name.ifBlank { it } },
            chipTone = TileTone.TERTIARY,
            onClick = { dialogReturn = folderRowFocus; showFolderPicker = true }, showChevron = true,
            modifier = Modifier.focusRequester(folderRowFocus),
        )
        SettingsRow(
            tone = TileTone.TERTIARY, icon = LunaIPtvIcon.DOWNLOADS,
            title = stringResource(R.string.settings_backup), desc = stringResource(R.string.settings_backup_desc),
            onClick = { open(SettingsTab.BACKUP) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.BACKUP)),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.HISTORY,
            title = stringResource(R.string.settings_clear_history), desc = stringResource(R.string.settings_clear_history_desc),
            onClick = { dialogReturn = clearHistoryRowFocus; showClearHistory = true }, showChevron = true,
            modifier = Modifier.focusRequester(clearHistoryRowFocus),
        )
        SectionDivider()
        GroupLabel(stringResource(R.string.settings_group_appearance))
        SettingsRow(
            tone = TileTone.PRIMARY, icon = LunaIPtvIcon.THEME,
            title = stringResource(R.string.settings_theme), desc = stringResource(R.string.settings_theme_desc),
            chip = themeLabel(themeMode), chipTone = TileTone.PRIMARY,
            onClick = { dialogReturn = themeRowFocus; showTheme = true }, showChevron = true,
            modifier = Modifier.focusRequester(themeRowFocus),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.PALETTE,
            title = stringResource(R.string.settings_accent), desc = stringResource(R.string.settings_accent_desc),
            chip = if (customAccent.isNotBlank()) customAccent.uppercase() else accent.label,
            chipTone = TileTone.SECONDARY,
            onClick = { dialogReturn = accentRowFocus; showAccent = true }, showChevron = true,
            modifier = Modifier.focusRequester(accentRowFocus),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.ZOOM,
            title = stringResource(R.string.settings_ui_zoom), desc = stringResource(R.string.settings_ui_zoom_desc),
            chip = UiZoom.label(uiZoomPercent), chipTone = TileTone.SECONDARY,
            onClick = { dialogReturn = zoomRowFocus; showZoom = true }, showChevron = true,
            modifier = Modifier.focusRequester(zoomRowFocus),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.THEME,
            title = stringResource(R.string.settings_animations), desc = stringResource(R.string.settings_animations_desc),
            chip = animationLevel.label, chipTone = TileTone.SECONDARY,
            onClick = { dialogReturn = animationsRowFocus; showAnimations = true }, showChevron = true,
            modifier = Modifier.focusRequester(animationsRowFocus),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.THEME,
            title = stringResource(R.string.settings_language), desc = stringResource(R.string.settings_language_desc),
            chip = if (language == "es") "Español" else "English", chipTone = TileTone.SECONDARY,
            onClick = { dialogReturn = languageRowFocus; showLanguage = true }, showChevron = true,
            modifier = Modifier.focusRequester(languageRowFocus),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.EPG,
            title = stringResource(R.string.settings_weather),
            desc = stringResource(R.string.settings_weather_desc),
            chip = if (weatherEnabled) stringResource(R.string.on) else stringResource(R.string.off),
            chipTone = if (weatherEnabled) TileTone.PRIMARY else TileTone.SECONDARY,
            onClick = { open(SettingsTab.WEATHER) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.WEATHER)),
        )

        SectionDivider()
        GroupLabel(stringResource(R.string.settings_group_playback))
        SettingsRow(
            tone = TileTone.TERTIARY, icon = LunaIPtvIcon.LIVE_TV,
            title = stringResource(R.string.settings_live_preview), desc = stringResource(R.string.settings_live_preview_desc),
            chip = if (livePreview) stringResource(R.string.on) else stringResource(R.string.off),
            chipTone = if (livePreview) TileTone.PRIMARY else TileTone.SECONDARY,
            onClick = { settingsVm.setLivePreviewEnabled(!livePreview) },
        )
        if (livePreview) {
            SettingsRow(
                tone = TileTone.SECONDARY, icon = LunaIPtvIcon.AUDIO,
                title = stringResource(R.string.settings_preview_audio), desc = stringResource(R.string.settings_preview_audio_desc),
                chip = if (previewAudio) stringResource(R.string.on) else stringResource(R.string.off),
                chipTone = if (previewAudio) TileTone.PRIMARY else TileTone.SECONDARY,
                onClick = { settingsVm.setLivePreviewAudio(!previewAudio) },
            )
        }
        SettingsRow(
            tone = TileTone.PRIMARY, icon = LunaIPtvIcon.VIDEO,
            title = stringResource(R.string.settings_hdr), desc = stringResource(R.string.settings_hdr_desc),
            chip = if (hdr) stringResource(R.string.on) else stringResource(R.string.off),
            chipTone = if (hdr) TileTone.PRIMARY else TileTone.SECONDARY,
            onClick = { settingsVm.setHdrEnabled(!hdr) },
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.AUDIO,
            title = stringResource(R.string.settings_surround),
            desc = stringResource(R.string.settings_surround_desc),
            chip = if (surroundSound) stringResource(R.string.on) else stringResource(R.string.off),
            chipTone = if (surroundSound) TileTone.PRIMARY else TileTone.SECONDARY,
            onClick = { settingsVm.setSurroundSound(!surroundSound) },
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.SKIP_NEXT,
            title = stringResource(R.string.settings_auto_next),
            desc = stringResource(R.string.settings_auto_next_desc),
            chip = if (autoPlayNext) stringResource(R.string.on) else stringResource(R.string.off),
            chipTone = if (autoPlayNext) TileTone.PRIMARY else TileTone.SECONDARY,
            onClick = { settingsVm.setAutoPlayNext(!autoPlayNext) },
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.EPG,
            title = stringResource(R.string.settings_catchup_time),
            desc = if (catchupChannels > 0) stringResource(R.string.settings_catchup_channels, catchupChannels)
                else stringResource(R.string.settings_catchup_no_channels),
            chip = when (catchupTz) {
                SettingsRepository.CatchupTimezone.DEVICE -> stringResource(R.string.settings_catchup_device)
                SettingsRepository.CatchupTimezone.MANUAL -> utcOffsetLabel(catchupOffset)
            },
            chipTone = TileTone.PRIMARY,
            onClick = { dialogReturn = catchupRowFocus; showCatchupTime = true }, showChevron = true,
            modifier = Modifier.focusRequester(catchupRowFocus),
        )
        SettingsRow(
            tone = TileTone.TERTIARY, icon = LunaIPtvIcon.VIDEO,
            title = stringResource(R.string.settings_video_player), desc = stringResource(R.string.settings_video_player_desc),
            onClick = { open(SettingsTab.VIDEO) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.VIDEO)),
        )

        SectionDivider()
        GroupLabel(stringResource(R.string.settings_group_network))
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.SHARE,
            title = stringResource(R.string.settings_proxy), desc = stringResource(R.string.settings_proxy_desc),
            onClick = { open(SettingsTab.NETWORK) }, showChevron = true,
            modifier = Modifier.focusRequester(rowFocus.getValue(SettingsTab.NETWORK)),
        )

        SectionDivider()
        GroupLabel(stringResource(R.string.settings_group_app))
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.HOME,
            title = stringResource(R.string.settings_app_startup), desc = stringResource(R.string.settings_app_startup_desc),
            chip = startupMode.label, chipTone = TileTone.PRIMARY,
            onClick = { dialogReturn = startupRowFocus; showStartup = true }, showChevron = true,
            modifier = Modifier.focusRequester(startupRowFocus),
        )
        SettingsRow(
            tone = TileTone.PRIMARY, icon = LunaIPtvIcon.DOWNLOADS,
            title = stringResource(R.string.settings_updates), desc = stringResource(R.string.settings_updates_desc),
            chip = "v${com.lunaiptv.BuildConfig.VERSION_NAME}",
            onClick = { dialogReturn = updateRowFocus; showUpdate = true }, showChevron = true,
            modifier = Modifier.focusRequester(updateRowFocus),
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.HISTORY,
            title = stringResource(R.string.settings_auto_update), desc = stringResource(R.string.settings_auto_update_desc),
            chip = if (updateCheckOnStart) stringResource(R.string.on) else stringResource(R.string.off),
            chipTone = if (updateCheckOnStart) TileTone.PRIMARY else TileTone.SECONDARY,
            onClick = { settingsVm.setUpdateCheckOnStart(!updateCheckOnStart) },
        )
        SettingsRow(
            tone = TileTone.SECONDARY, icon = LunaIPtvIcon.MENU,
            title = stringResource(R.string.settings_about), desc = stringResource(R.string.settings_about_desc),
            onClick = { dialogReturn = aboutRowFocus; showAbout = true }, showChevron = true,
            modifier = Modifier.focusRequester(aboutRowFocus),
        )
        } else {
            // Batch 4 · search results — flat, group-context-prefixed rows ("Playback › HDR").
            // Dialog-opening entries return focus to the search field on close (their normal row
            // isn't composed while searching). Toggle entries keep the results visible so the chip
            // updates live.
            val entries = listOf(
                SettingsSearchEntry(stringResource(R.string.settings_group_profile), stringResource(R.string.settings_profiles), "viewers kids mode pin lock account espectadores niños modo pin", LunaIPtvIcon.PERSON, TileTone.SECONDARY) { open(SettingsTab.PROFILES) },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_playlists), "m3u xtream source sync add remove listas fuentes", LunaIPtvIcon.PLAYLIST, TileTone.PRIMARY) { open(SettingsTab.SOURCES) },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_epg_sources), "xmltv guide feed program guía", LunaIPtvIcon.EPG, TileTone.PRIMARY) { open(SettingsTab.EPG) },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_customize), "hide unhide rename reorder categories ocultar mostrar renombrar categorías", LunaIPtvIcon.SORT, TileTone.PRIMARY) { open(SettingsTab.CUSTOMIZE) },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_home_screen), "rows hero reorder filter filas inicio", LunaIPtvIcon.HOME, TileTone.SECONDARY) { open(SettingsTab.HOME) },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_metadata), "posters plots cast ratings pósters sinopsis reparto", LunaIPtvIcon.VIDEO, TileTone.PRIMARY) { open(SettingsTab.METADATA) },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_download_folder), "storage path directory almacenamiento ruta", LunaIPtvIcon.DOWNLOADS, TileTone.TERTIARY,
                    chip = downloadRoot.ifBlank { stringResource(R.string.settings_download_folder_default) }.let { java.io.File(it).name.ifBlank { it } }, chipTone = TileTone.TERTIARY) { dialogReturn = searchFieldFocus; showFolderPicker = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_backup), "export import profiles sources exportar importar", LunaIPtvIcon.DOWNLOADS, TileTone.TERTIARY) { open(SettingsTab.BACKUP) },
                SettingsSearchEntry(stringResource(R.string.settings_group_content), stringResource(R.string.settings_clear_history), "recently watched continue remove historial reciente continuar", LunaIPtvIcon.HISTORY, TileTone.SECONDARY) { dialogReturn = searchFieldFocus; showClearHistory = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_appearance), stringResource(R.string.settings_theme), "light dark system claro oscuro", LunaIPtvIcon.THEME, TileTone.PRIMARY,
                    chip = themeLabel(themeMode)) { dialogReturn = searchFieldFocus; showTheme = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_appearance), stringResource(R.string.settings_accent), "tint palette hex preset color acento paleta", LunaIPtvIcon.PALETTE, TileTone.SECONDARY,
                    chip = if (customAccent.isNotBlank()) customAccent.uppercase() else accent.label, chipTone = TileTone.SECONDARY) { dialogReturn = searchFieldFocus; showAccent = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_appearance), stringResource(R.string.settings_ui_zoom), "scale interface size escalar interfaz", LunaIPtvIcon.ZOOM, TileTone.SECONDARY,
                    chip = UiZoom.label(uiZoomPercent), chipTone = TileTone.SECONDARY) { dialogReturn = searchFieldFocus; showZoom = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_appearance), stringResource(R.string.settings_animations), "motion snappier performance movimiento animaciones", LunaIPtvIcon.THEME, TileTone.SECONDARY,
                    chip = animationLevel.label, chipTone = TileTone.SECONDARY) { dialogReturn = searchFieldFocus; showAnimations = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_appearance), stringResource(R.string.settings_weather), "top bar chip location celsius fahrenheit clima ubicación", LunaIPtvIcon.EPG, TileTone.SECONDARY,
                    chip = if (weatherEnabled) stringResource(R.string.on) else stringResource(R.string.off), chipTone = if (weatherEnabled) TileTone.PRIMARY else TileTone.SECONDARY) { open(SettingsTab.WEATHER) },
                SettingsSearchEntry(stringResource(R.string.settings_group_playback), stringResource(R.string.settings_live_preview), "auto play focus channel reproducción enfocar canal", LunaIPtvIcon.LIVE_TV, TileTone.TERTIARY,
                    chip = if (livePreview) stringResource(R.string.on) else stringResource(R.string.off), chipTone = if (livePreview) TileTone.PRIMARY else TileTone.SECONDARY, showChevron = false) { settingsVm.setLivePreviewEnabled(!livePreview) },
                SettingsSearchEntry(stringResource(R.string.settings_group_playback), stringResource(R.string.settings_preview_audio), "sound live preview sonido vista previa", LunaIPtvIcon.AUDIO, TileTone.SECONDARY,
                    chip = if (previewAudio) stringResource(R.string.on) else stringResource(R.string.off), chipTone = if (previewAudio) TileTone.PRIMARY else TileTone.SECONDARY, showChevron = false) { settingsVm.setLivePreviewAudio(!previewAudio) },
                SettingsSearchEntry(stringResource(R.string.settings_group_playback), stringResource(R.string.settings_hdr), "high dynamic range output rango dinámico", LunaIPtvIcon.VIDEO, TileTone.PRIMARY,
                    chip = if (hdr) stringResource(R.string.on) else stringResource(R.string.off), chipTone = if (hdr) TileTone.PRIMARY else TileTone.SECONDARY, showChevron = false) { settingsVm.setHdrEnabled(!hdr) },
                SettingsSearchEntry(stringResource(R.string.settings_group_playback), stringResource(R.string.settings_surround), "dolby dts 5.1 7.1 receiver audio envolvente", LunaIPtvIcon.AUDIO, TileTone.SECONDARY,
                    chip = if (surroundSound) stringResource(R.string.on) else stringResource(R.string.off), chipTone = if (surroundSound) TileTone.PRIMARY else TileTone.SECONDARY, showChevron = false) { settingsVm.setSurroundSound(!surroundSound) },
                SettingsSearchEntry(stringResource(R.string.settings_group_playback), stringResource(R.string.settings_auto_next), "autoplay series season episodio siguiente", LunaIPtvIcon.SKIP_NEXT, TileTone.SECONDARY,
                    chip = if (autoPlayNext) stringResource(R.string.on) else stringResource(R.string.off), chipTone = if (autoPlayNext) TileTone.PRIMARY else TileTone.SECONDARY, showChevron = false) { settingsVm.setAutoPlayNext(!autoPlayNext) },
                SettingsSearchEntry(stringResource(R.string.settings_group_playback), stringResource(R.string.settings_catchup_time), "archive timezone offset repetición zona horaria", LunaIPtvIcon.EPG, TileTone.SECONDARY,
                    chip = when (catchupTz) {
                        SettingsRepository.CatchupTimezone.DEVICE -> stringResource(R.string.settings_catchup_device)
                        SettingsRepository.CatchupTimezone.MANUAL -> utcOffsetLabel(catchupOffset)
                    }) { dialogReturn = searchFieldFocus; showCatchupTime = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_playback), stringResource(R.string.settings_video_player), "decoder subtitles sync decodificador subtítulos", LunaIPtvIcon.VIDEO, TileTone.TERTIARY) { open(SettingsTab.VIDEO) },
                SettingsSearchEntry(stringResource(R.string.settings_group_network), stringResource(R.string.settings_proxy), "http traffic route tráfico", LunaIPtvIcon.SHARE, TileTone.SECONDARY) { open(SettingsTab.NETWORK) },
                SettingsSearchEntry(stringResource(R.string.settings_group_app), stringResource(R.string.settings_app_startup), "launch open landing iniciar abrir", LunaIPtvIcon.HOME, TileTone.SECONDARY,
                    chip = startupMode.label) { dialogReturn = searchFieldFocus; showStartup = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_app), stringResource(R.string.settings_updates), "github release version actualización", LunaIPtvIcon.DOWNLOADS, TileTone.PRIMARY,
                    chip = "v${com.lunaiptv.BuildConfig.VERSION_NAME}") { dialogReturn = searchFieldFocus; showUpdate = true },
                SettingsSearchEntry(stringResource(R.string.settings_group_app), stringResource(R.string.settings_auto_update), "auto update new version actualizar iniciar", LunaIPtvIcon.HISTORY, TileTone.SECONDARY,
                    chip = if (updateCheckOnStart) stringResource(R.string.on) else stringResource(R.string.off), chipTone = if (updateCheckOnStart) TileTone.PRIMARY else TileTone.SECONDARY, showChevron = false) { settingsVm.setUpdateCheckOnStart(!updateCheckOnStart) },
                SettingsSearchEntry(stringResource(R.string.settings_group_app), stringResource(R.string.settings_about), "version license project info versión licencia", LunaIPtvIcon.MENU, TileTone.SECONDARY) { dialogReturn = searchFieldFocus; showAbout = true },
            )
            val tokens = searchQuery.trim().lowercase().split(" ").filter { it.isNotBlank() }
            val results = entries.filter { e -> tokens.all { t -> e.haystack.contains(t) } }
            if (results.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_match, searchQuery.trim()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                results.forEach { e ->
                    SettingsRow(
                        tone = e.tone, icon = e.icon,
                        title = "${e.group} › ${e.title}",
                        chip = e.chip, chipTone = e.chipTone,
                        showChevron = e.showChevron,
                        onClick = e.onClick,
                    )
                }
            }
        }
    }

    if (showUpdate) {
        UpdateDialog(onDismiss = { showUpdate = false }, checkOnOpen = true)
    }
    if (showCatchupTime) {
        CatchupTimeDialog(
            mode = catchupTz,
            offsetMinutes = catchupOffset,
            offsetRange = settingsVm.catchupOffsetRangeMinutes,
            onSetMode = settingsVm::setCatchupTimezone,
            onAdjustOffset = settingsVm::adjustCatchupOffset,
            onDismiss = { showCatchupTime = false },
        )
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
    if (showClearHistory) {
        ClearHistoryDialog(
            onClear = { type -> settingsVm.clearWatchHistory(type); showClearHistory = false },
            onDismiss = { showClearHistory = false },
        )
    }
    if (showTheme) {
        com.lunaiptv.features.settings.PickerDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it.name to themeLabel(it) },
            selected = themeMode.name,
            onSelect = { settingsVm.setThemeMode(ThemeMode.valueOf(it)); showTheme = false },
            onDismiss = { showTheme = false },
        )
    }
    if (showStartup) {
        com.lunaiptv.features.settings.PickerDialog(
            title = stringResource(R.string.settings_app_startup),
            options = com.lunaiptv.features.settings.data.StartupMode.entries.map { it.name to it.label },
            selected = startupMode.name,
            onSelect = { settingsVm.setStartupMode(com.lunaiptv.features.settings.data.StartupMode.valueOf(it)); showStartup = false },
            onDismiss = { showStartup = false },
        )
    }
    if (showAnimations) {
        com.lunaiptv.features.settings.PickerDialog(
            title = stringResource(R.string.settings_animations),
            options = com.lunaiptv.ui.theme.AnimationLevel.entries.map { it.name to it.label },
            selected = animationLevel.name,
            onSelect = { settingsVm.setAnimationLevel(com.lunaiptv.ui.theme.AnimationLevel.valueOf(it)); showAnimations = false },
            onDismiss = { showAnimations = false },
        )
    }
    if (showLanguage) {
        com.lunaiptv.features.settings.PickerDialog(
            title = stringResource(R.string.settings_language),
            options = listOf("en" to stringResource(R.string.language_english), "es" to stringResource(R.string.language_spanish)),
            selected = language,
            onSelect = { onSetLanguage(it); showLanguage = false },
            onDismiss = { showLanguage = false },
        )
    }
    if (showAccent) {
        AccentPaletteDialog(
            accent = accent,
            customAccent = customAccent,
            onPickPreset = { settingsVm.setAccent(it) },
            onPickCustom = { settingsVm.setCustomAccent(it) },
            onDismiss = { showAccent = false },
        )
    }
    if (showZoom) {
        ZoomDialog(current = uiZoomPercent, onSet = onSetZoom, onDismiss = { showZoom = false })
    }
    if (showFolderPicker) {
        StorageBrowser(
            title = stringResource(R.string.settings_choose_download),
            mode = BrowseMode.FOLDER,
            onPick = { settingsVm.setDownloadRoot(it.absolutePath); showFolderPicker = false },
            onDismiss = { showFolderPicker = false },
        )
    }
}

@Composable
private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
}

/**
 * Accent picker: preset swatches, a hue/shade palette, and a hex-code field for an exact color.
 * Presets clear the custom color; palette/hex set it (custom overrides the preset in the theme).
 */
@Composable
private fun AccentPaletteDialog(
    accent: com.lunaiptv.ui.theme.AccentColor,
    customAccent: String,
    onPickPreset: (com.lunaiptv.ui.theme.AccentColor) -> Unit,
    onPickCustom: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    val isDark = colors.isDark
    val firstFocus = remember { FocusRequester() }
    var hexInput by remember { mutableStateOf(customAccent.removePrefix("#")) }
    var hexError by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    BackHandler { onDismiss() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(640.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceContainerHigh)
                .padding(28.dp),
        ) {
            Text(stringResource(R.string.settings_accent_section), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.settings_presets), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.lunaiptv.ui.theme.AccentColor.entries.forEachIndexed { i, ac ->
                    val isSel = customAccent.isBlank() && ac == accent
                    Swatch(
                        color = ac.primary(isDark),
                        selected = isSel,
                        onClick = { onPickPreset(ac); onDismiss() },
                        modifier = if (i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_palette), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val hues = (0 until 360 step 30).toList()
            listOf(0.85f to 0.55f, 0.55f to 0.72f).forEach { (sat, light) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    hues.forEach { h ->
                        val c = Color.hsl(h.toFloat(), sat, light)
                        val hex = "#%06X".format(c.toArgb() and 0xFFFFFF)
                        Swatch(
                            color = c,
                            selected = customAccent.equals(hex, ignoreCase = true),
                            onClick = { onPickCustom(hex); onDismiss() },
                            sizeDp = 36,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.settings_hex_code), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("#", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                com.lunaiptv.ui.components.LunaIPtvTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it.take(6); hexError = false },
                    label = stringResource(R.string.settings_hex_field),
                    placeholder = "52DBC8",
                    modifier = Modifier.width(200.dp),
                )
                LunaIPtvButton(stringResource(R.string.settings_apply), onClick = {
                    val parsed = com.lunaiptv.ui.theme.parseAccentHex(hexInput)
                    if (parsed != null) {
                        onPickCustom("#" + hexInput.trim().removePrefix("#").uppercase())
                        onDismiss()
                    } else {
                        hexError = true
                    }
                })
                if (hexError) {
                    Text(stringResource(R.string.settings_hex_hint), style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                LunaIPtvButton(stringResource(R.string.close), onClick = onDismiss, style = LunaIPtvButtonStyle.SECONDARY)
            }
        }
    }
}

/** A focusable color swatch circle; the selected one is ringed. */
@Composable
private fun Swatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44,
) {
    val colors = LunaIPtvTheme.colors
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.size((sizeDp + 14).dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        selected = selected,
        unfocusedContainerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
        contentAlignment = Alignment.Center,
    ) { _ ->
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(3.dp, colors.onSurface, androidx.compose.foundation.shape.CircleShape)
                    else Modifier,
                ),
        )
    }
}

/** About LunaIPtv: version, license and author info. */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BackHandler { onDismiss() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(520.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "LunaIPtv",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.settings_version, com.lunaiptv.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleMedium, color = colors.primary)
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.settings_copyright), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Spacer(Modifier.height(20.dp))
            LunaIPtvButton(stringResource(R.string.close), onClick = onDismiss, modifier = Modifier.focusRequester(focus))
        }
    }
}

/**
 * Pick what watch history to clear: everything, or just Live TV / Movies / Series. Over a dimmed scrim;
 * Cancel is focused first so a stray OK doesn't wipe anything. [onClear] gets null for "all".
 */
@Composable
private fun ClearHistoryDialog(
    onClear: (com.lunaiptv.core.model.MediaType?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    // null = still choosing a scope; non-null = confirming that scope (type + label; type null = everything).
    var pending by remember { mutableStateOf<Pair<com.lunaiptv.core.model.MediaType?, String>?>(null) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(pending) { runCatching { firstFocus.requestFocus() } }
    BackHandler { if (pending != null) pending = null else onDismiss() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(460.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val p = pending
            if (p == null) {
                Text(stringResource(R.string.settings_clear_history_title), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_clear_history_choice_desc),
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                LunaIPtvButton(stringResource(R.string.cancel), onClick = onDismiss, style = LunaIPtvButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth().focusRequester(firstFocus))
                Spacer(Modifier.height(10.dp))
                LunaIPtvButton(stringResource(R.string.nav_live), onClick = { pending = com.lunaiptv.core.model.MediaType.LIVE to "Live TV" }, style = LunaIPtvButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LunaIPtvButton(stringResource(R.string.nav_movies), onClick = { pending = com.lunaiptv.core.model.MediaType.MOVIE to "Movies" }, style = LunaIPtvButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LunaIPtvButton(stringResource(R.string.nav_series), onClick = { pending = com.lunaiptv.core.model.MediaType.SERIES to "Series" }, style = LunaIPtvButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LunaIPtvButton(stringResource(R.string.settings_clear_history_all), onClick = { pending = (null as com.lunaiptv.core.model.MediaType?) to "all" }, modifier = Modifier.fillMaxWidth())
            } else {
                Text(stringResource(R.string.settings_clear_history_confirm, p.second), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_clear_history_warning), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LunaIPtvButton(stringResource(R.string.no), onClick = { pending = null }, style = LunaIPtvButtonStyle.SECONDARY, modifier = Modifier.focusRequester(firstFocus))
                    LunaIPtvButton(stringResource(R.string.yes_clear), onClick = { onClear(p.first) })
                }
            }
        }
    }
}

/** A stepper for the global UI scale. Changes apply live (the whole UI re-scales as you adjust). */
@Composable
private fun ZoomDialog(current: Int, onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    // Zoom below LOW_RAM_WARN doubles the on-screen item count, which can OOM-crash 2 GB devices
    // (#51) — the first step under it is gated behind an accept-the-risk warning. Accepting once
    // arms the rest of this dialog session; if it was opened already below the line, don't nag.
    var lowZoomAccepted by remember { mutableStateOf(current < UiZoom.LOW_RAM_WARN) }
    var pendingLowZoom by remember { mutableStateOf<Int?>(null) }
    BackHandler { onDismiss() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(460.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.settings_ui_zoom_section), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.settings_ui_zoom_dialog_desc, UiZoom.MIN, UiZoom.MAX),
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // Initial focus lands on the DECREASE button: the dialog is most often opened to escape an
                // over-zoomed screen (where everything's too big to navigate), so "–" must be first under
                // the cursor. The buttons stay focusable at the limits (clamped + dimmed, never disabled)
                // so focus always lands inside the dialog — a disabled "+" at MAX zoom was leaving focus
                // stranded outside, trapping the user at high zoom.
                StepButton("–", dimmed = current <= UiZoom.MIN, modifier = Modifier.focusRequester(firstFocus)) {
                    val next = UiZoom.clamp(current - UiZoom.STEP)
                    if (next < UiZoom.LOW_RAM_WARN && !lowZoomAccepted) pendingLowZoom = next else onSet(next)
                }
                Text(
                    UiZoom.label(current),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.primary,
                    modifier = Modifier.width(120.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                StepButton("+", dimmed = current >= UiZoom.MAX) {
                    onSet(UiZoom.clamp(current + UiZoom.STEP))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LunaIPtvButton(stringResource(R.string.reset), onClick = { onSet(UiZoom.DEFAULT) }, style = LunaIPtvButtonStyle.SECONDARY)
                Spacer(Modifier.weight(1f))
                LunaIPtvButton(stringResource(R.string.done), onClick = onDismiss)
            }
        }

        // Accept-the-risk gate for zoom below LOW_RAM_WARN (#51). One button, focus locked (all
        // D-pad directions cancelled) — OK accepts and applies the pending step, Back cancels.
        pendingLowZoom?.let { target ->
            val acceptFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { acceptFocus.requestFocus() } }
            // Composed after the dialog's own BackHandler, so it wins while the warning is up.
            BackHandler {
                pendingLowZoom = null
                runCatching { firstFocus.requestFocus() }
            }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.width(460.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.settings_ui_zoom_warning), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.settings_ui_zoom_warning_desc, UiZoom.LOW_RAM_WARN),
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    LunaIPtvButton(
                        stringResource(R.string.settings_ui_zoom_warning_accept),
                        onClick = {
                            lowZoomAccepted = true
                            pendingLowZoom = null
                            onSet(target)
                            runCatching { firstFocus.requestFocus() }
                        },
                        modifier = Modifier
                            .focusRequester(acceptFocus)
                            .focusProperties {
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            },
                    )
                }
            }
        }
    }
}

/** "UTC", "UTC+05:00", "UTC-03:30" — labels a UTC offset (in minutes) for catch-up. */
private fun utcOffsetLabel(minutes: Int): String {
    if (minutes == 0) return "UTC"
    val sign = if (minutes < 0) "-" else "+"
    val abs = kotlin.math.abs(minutes)
    return "UTC$sign%02d:%02d".format(abs / 60, abs % 60)
}

@Composable
private fun CatchupTimeDialog(
    mode: SettingsRepository.CatchupTimezone,
    offsetMinutes: Int,
    offsetRange: IntRange,
    onSetMode: (SettingsRepository.CatchupTimezone) -> Unit,
    onAdjustOffset: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    BackHandler { onDismiss() }
    val manual = mode == SettingsRepository.CatchupTimezone.MANUAL
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(480.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.settings_catchup_section), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.settings_catchup_desc),
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            // Mode toggle: Device / Manual.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LunaIPtvButton(
                    stringResource(R.string.settings_catchup_device),
                    onClick = { onSetMode(SettingsRepository.CatchupTimezone.DEVICE) },
                    style = if (!manual) LunaIPtvButtonStyle.PRIMARY else LunaIPtvButtonStyle.SECONDARY,
                    modifier = Modifier.focusRequester(firstFocus),
                )
                LunaIPtvButton(
                    stringResource(R.string.settings_catchup_manual),
                    onClick = { onSetMode(SettingsRepository.CatchupTimezone.MANUAL) },
                    style = if (manual) LunaIPtvButtonStyle.PRIMARY else LunaIPtvButtonStyle.SECONDARY,
                )
            }
            if (manual) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    StepButton("–", enabled = offsetMinutes > offsetRange.first) { onAdjustOffset(-60) }
                    Text(
                        utcOffsetLabel(offsetMinutes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.primary,
                        modifier = Modifier.width(150.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    StepButton("+", enabled = offsetMinutes < offsetRange.last) { onAdjustOffset(60) }
                }
            }
            Spacer(Modifier.height(24.dp))
            LunaIPtvButton(stringResource(R.string.done), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StepButton(
    label: String,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    FocusableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(64.dp),
        shape = RoundedCornerShape(18.dp),
        contentAlignment = Alignment.Center,
    ) { _ ->
        Text(label, style = MaterialTheme.typography.headlineMedium, color = if (enabled && !dimmed) colors.onSurface else colors.outline)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LunaIPtvTheme.colors.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(1.dp)
            .background(LunaIPtvTheme.colors.outlineVariant),
    )
}

@Composable
private fun SettingsRow(
    tone: TileTone,
    icon: LunaIPtvIcon,
    title: String,
    desc: String? = null,
    chip: String? = null,
    chipTone: TileTone = TileTone.PRIMARY,
    soon: Boolean = false,
    showChevron: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = LunaIPtvTheme.colors
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tonal icon tile
            val (tileBg, tileOn) = tone.colors()
            Box(
                modifier = Modifier
                    .size(Dimens.IconTileSize)
                    .clip(RoundedCornerShape(Dimens.IconTileCorner))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                LunaIPtvIcon(icon = icon, tint = tileOn, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                if (desc != null) {
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (soon) {
                    SoonChip()
                }
                if (chip != null) {
                    ValueChip(chip, chipTone)
                }
                if (showChevron) {
                    LunaIPtvIcon(icon = LunaIPtvIcon.CHEVRON, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SoonChip() {
    val colors = LunaIPtvTheme.colors
    Text(
        text = "SOON",
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceContainerHighest)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Batch 4 · one searchable settings row: its group breadcrumb, title, extra keywords, and action. */
private class SettingsSearchEntry(
    val group: String,
    val title: String,
    keywords: String,
    val icon: LunaIPtvIcon,
    val tone: TileTone,
    val chip: String? = null,
    val chipTone: TileTone = TileTone.PRIMARY,
    val showChevron: Boolean = true,
    val onClick: () -> Unit,
) {
    /** Lower-cased match target: group + title + keywords. */
    val haystack: String = "$group $title $keywords".lowercase()
}

/** Batch 4 · a focusable most-used toggle chip shown pinned above the settings list. */
@Composable
private fun QuickToggleChip(
    label: String,
    on: Boolean,
    icon: LunaIPtvIcon,
    onToggle: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    val onColors = TileTone.PRIMARY.colors()
    val offColors = TileTone.SECONDARY.colors()
    val (bg, fg) = if (on) onColors else offColors
    FocusableSurface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        contentAlignment = Alignment.Center,
    ) { _ ->
        Row(
            modifier = Modifier
                .background(bg, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LunaIPtvIcon(icon = icon, tint = fg, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (on) stringResource(R.string.on) else stringResource(R.string.off),
                style = MaterialTheme.typography.labelMedium,
                color = if (on) fg else colors.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ValueChip(text: String, tone: TileTone) {
    val (bg, on) = tone.colors()
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = on,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun TileTone.colors(): Pair<Color, Color> {
    val c = LunaIPtvTheme.colors
    return when (this) {
        TileTone.PRIMARY -> c.primaryContainer to c.onPrimaryContainer
        TileTone.SECONDARY -> c.secondaryContainer to c.onSecondaryContainer
        TileTone.TERTIARY -> c.tertiaryContainer to c.onTertiaryContainer
    }
}
