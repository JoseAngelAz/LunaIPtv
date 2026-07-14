package com.lunaiptv.phone.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.ui.theme.AccentColor
import com.lunaiptv.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PhoneViewModel(
    settings: SettingsRepository,
) : ViewModel() {
    val themeMode = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val accent = settings.accent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentColor.TEAL)

    val customAccent = settings.customAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
}
