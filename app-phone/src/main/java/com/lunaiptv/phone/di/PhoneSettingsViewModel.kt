@file:OptIn(FlowPreview::class)

package com.lunaiptv.phone.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.ui.theme.AccentColor
import com.lunaiptv.ui.theme.AnimationLevel
import com.lunaiptv.ui.theme.ThemeMode

/**
 * Phone-specific settings ViewModel. Exposes appearance + basic playback settings
 * without TV-only deps (LauncherIntegrationRepository, TvHomeRepository, etc.).
 */
@OptIn(FlowPreview::class)
class PhoneSettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    // ── Appearance ──────────────────────────────────────────
    val themeMode = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val accent = settings.accent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentColor.TEAL)
    val customAccent = settings.customAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val uiZoomPercent = settings.uiZoomPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)
    val language = settings.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")
    val animationLevel = settings.animationLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnimationLevel.FULL)

    // ── Playback ────────────────────────────────────────────
    val livePreviewEnabled = settings.livePreviewEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val livePreviewAudio = settings.livePreviewAudio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val externalPlayer = settings.externalPlayer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── Setters ─────────────────────────────────────────────
    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { settings.setThemeMode(mode) } }
    fun setAccent(color: AccentColor) { viewModelScope.launch { settings.setAccent(color) } }
    fun setCustomAccent(hex: String) { viewModelScope.launch { settings.setCustomAccent(hex) } }
    fun setUiZoom(pct: Int) { viewModelScope.launch { settings.setUiZoomPercent(pct) } }
    fun setLanguage(lang: String) { viewModelScope.launch { settings.setLanguage(lang) } }
    fun setAnimationLevel(level: AnimationLevel) { viewModelScope.launch { settings.setAnimationLevel(level) } }
    fun setLivePreview(enabled: Boolean) { viewModelScope.launch { settings.setLivePreviewEnabled(enabled) } }
    fun setLivePreviewAudio(enabled: Boolean) { viewModelScope.launch { settings.setLivePreviewAudio(enabled) } }
    fun setExternalPlayer(enabled: Boolean) { viewModelScope.launch { settings.setExternalPlayer(enabled) } }
}
