@file:OptIn(FlowPreview::class)

package com.lunaiptv.phone.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lunaiptv.core.metadata.MetadataMode
import com.lunaiptv.core.network.ProxyConfig
import com.lunaiptv.features.settings.data.EpgAutoRefresh
import com.lunaiptv.features.settings.data.PlaylistAutoRefresh
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.ui.theme.AccentColor
import com.lunaiptv.ui.theme.AnimationLevel
import com.lunaiptv.ui.theme.ThemeMode

/**
 * Phone-specific settings ViewModel. Exposes appearance + playback + metadata + network settings
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
    val hwDecoding = settings.hwDecoding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val vodPreferExo = settings.vodPreferExo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val surroundSound = settings.surroundSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoPlayNext = settings.autoPlayNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hdrEnabled = settings.hdrEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val defaultZoom = settings.defaultZoom
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FIT")
    val subtitleScale = settings.subtitleScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val audioDelayMs = settings.audioDelayMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val preferredAudioLang = settings.preferredAudioLang
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val preferredSubLang = settings.preferredSubLang
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val resumeMode = settings.resumeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.ResumeMode.ASK)

    // ── Weather ─────────────────────────────────────────────
    val weatherEnabled = settings.weatherEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val weatherLocation = settings.weatherLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val weatherFahrenheit = settings.weatherFahrenheit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── Metadata / TMDB ────────────────────────────────────
    val metadataMode = settings.metadataMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetadataMode.PROVIDER_PLUS_TMDB)
    val tmdbApiKey = settings.tmdbApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val metadataServerUrl = settings.metadataServerUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // ── Network / Proxy ────────────────────────────────────
    val proxyConfig = settings.proxyConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProxyConfig())

    // ── Auto-refresh ────────────────────────────────────────
    val playlistAutoRefresh = settings.playlistAutoRefresh
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val epgAutoRefresh = settings.epgAutoRefresh
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ── Catch-up ────────────────────────────────────────────
    val catchupTimezone = settings.catchupTimezone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.CatchupTimezone.DEVICE)
    val catchupOffsetMinutes = settings.catchupOffsetMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val catchupOffsetRangeMinutes: IntRange = settings.catchupOffsetRangeMinutes

    // ── Update ──────────────────────────────────────────────
    val updateCheckOnStart = settings.updateCheckOnStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // ── Appearance setters ──────────────────────────────────
    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { settings.setThemeMode(mode) } }
    fun setAccent(color: AccentColor) { viewModelScope.launch { settings.setAccent(color) } }
    fun setCustomAccent(hex: String) { viewModelScope.launch { settings.setCustomAccent(hex) } }
    fun setUiZoom(pct: Int) { viewModelScope.launch { settings.setUiZoomPercent(pct) } }
    fun setLanguage(lang: String) { viewModelScope.launch { settings.setLanguage(lang) } }
    fun setAnimationLevel(level: AnimationLevel) { viewModelScope.launch { settings.setAnimationLevel(level) } }

    // ── Playback setters ────────────────────────────────────
    fun setLivePreview(enabled: Boolean) { viewModelScope.launch { settings.setLivePreviewEnabled(enabled) } }
    fun setLivePreviewAudio(enabled: Boolean) { viewModelScope.launch { settings.setLivePreviewAudio(enabled) } }
    fun setExternalPlayer(enabled: Boolean) { viewModelScope.launch { settings.setExternalPlayer(enabled) } }
    fun setHwDecoding(enabled: Boolean) { viewModelScope.launch { settings.setHwDecoding(enabled) } }
    fun setVodPreferExo(enabled: Boolean) { viewModelScope.launch { settings.setVodPreferExo(enabled) } }
    fun setSurroundSound(enabled: Boolean) { viewModelScope.launch { settings.setSurroundSound(enabled) } }
    fun setAutoPlayNext(enabled: Boolean) { viewModelScope.launch { settings.setAutoPlayNext(enabled) } }
    fun setHdrEnabled(enabled: Boolean) { viewModelScope.launch { settings.setHdrEnabled(enabled) } }
    fun setDefaultZoom(name: String) { viewModelScope.launch { settings.setDefaultZoom(name) } }
    fun setSubtitleScale(scale: Float) { viewModelScope.launch { settings.setSubtitleScale(scale) } }
    fun setAudioDelayMs(ms: Int) { viewModelScope.launch { settings.setAudioDelayMs(ms) } }
    fun setPreferredAudioLang(lang: String) { viewModelScope.launch { settings.setPreferredAudioLang(lang) } }
    fun setPreferredSubLang(lang: String) { viewModelScope.launch { settings.setPreferredSubLang(lang) } }
    fun setResumeMode(name: String) {
        viewModelScope.launch {
            settings.setResumeMode(
                runCatching { SettingsRepository.ResumeMode.valueOf(name) }.getOrDefault(SettingsRepository.ResumeMode.ASK)
            )
        }
    }

    // ── Weather setters ─────────────────────────────────────
    fun setWeatherEnabled(enabled: Boolean) { viewModelScope.launch { settings.setWeatherEnabled(enabled) } }
    fun setWeatherLocation(location: String) { viewModelScope.launch { settings.setWeatherLocation(location) } }
    fun setWeatherFahrenheit(fahrenheit: Boolean) { viewModelScope.launch { settings.setWeatherFahrenheit(fahrenheit) } }

    // ── Metadata setters ────────────────────────────────────
    fun setMetadataMode(mode: MetadataMode) { viewModelScope.launch { settings.setMetadataMode(mode) } }
    fun setTmdbApiKey(key: String) { viewModelScope.launch { settings.setTmdbApiKey(key) } }
    fun setMetadataServerUrl(url: String) { viewModelScope.launch { settings.setMetadataServerUrl(url) } }

    // ── Proxy ───────────────────────────────────────────────
    fun saveProxy(enabled: Boolean, host: String, port: Int, username: String, password: String) {
        viewModelScope.launch { settings.saveProxy(enabled, host, port, username, password) }
    }

    // ── Auto-refresh setters ────────────────────────────────
    fun setPlaylistAutoRefresh(sourceId: Long, mode: PlaylistAutoRefresh) {
        viewModelScope.launch { settings.setPlaylistAutoRefresh(sourceId, mode) }
    }
    fun setEpgAutoRefresh(sourceId: Long, mode: EpgAutoRefresh) {
        viewModelScope.launch { settings.setEpgAutoRefresh(sourceId, mode) }
    }

    // ── Catch-up setters ────────────────────────────────────
    fun setCatchupTimezone(mode: SettingsRepository.CatchupTimezone) {
        viewModelScope.launch { settings.setCatchupTimezone(mode) }
    }
    fun adjustCatchupOffset(deltaMinutes: Int) {
        viewModelScope.launch { settings.setCatchupOffsetMinutes((catchupOffsetMinutes.value + deltaMinutes)) }
    }

    // ── Update setter ───────────────────────────────────────
    fun setUpdateCheckOnStart(enabled: Boolean) { viewModelScope.launch { settings.setUpdateCheckOnStart(enabled) } }

    // ── Backup export / import ──────────────────────────────
    suspend fun exportSettings(): org.json.JSONObject = settings.exportSettings()
    suspend fun importSettings(o: org.json.JSONObject) { settings.importSettings(o) }
}
