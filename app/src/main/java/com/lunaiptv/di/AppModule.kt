package com.lunaiptv.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.lunaiptv.features.customize.CustomizeViewModel
import com.lunaiptv.features.downloads.DownloadsViewModel
import com.lunaiptv.features.epg.EpgViewModel
import com.lunaiptv.features.home.HomeViewModel
import com.lunaiptv.features.live.LiveViewModel
import com.lunaiptv.features.movies.MovieViewModel
import com.lunaiptv.features.profiles.ProfilesViewModel
import com.lunaiptv.features.search.SearchViewModel
import com.lunaiptv.features.series.SeriesViewModel
import com.lunaiptv.features.settings.BackupViewModel
import com.lunaiptv.features.settings.HomeSettingsViewModel
import com.lunaiptv.features.settings.SettingsViewModel
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.features.setup.SetupViewModel
import com.lunaiptv.features.shell.ShellViewModel

/**
 * Root Koin module. Each feature will contribute its own bindings as the app grows;
 * for now this wires settings persistence and the shell view model.
 */
val appModule = module {
    single { SettingsRepository(androidContext()) }
    // Merged (v4.0.0 + PR#31 Home/launcher). Koin resolves each get() by type, so only the count must match
    // each ViewModel's merged constructor.
    viewModel { ShellViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SetupViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { LiveViewModel(androidContext(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { MovieViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SeriesViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ProfilesViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HomeSettingsViewModel(get()) }
    viewModel { DownloadsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { EpgViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // settings, sourceDao, categoryDao, customizationStore
    viewModel { CustomizeViewModel(get(), get(), get(), get()) }
    // backupManager
    viewModel { BackupViewModel(get()) }
    // store, epgRepository, sourceRepository, settings, epgDao, channelDao, scheduler
    viewModel { com.lunaiptv.features.settings.EpgSourcesViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
