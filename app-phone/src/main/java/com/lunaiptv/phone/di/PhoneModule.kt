package com.lunaiptv.phone.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.lunaiptv.features.settings.data.SettingsRepository

val phoneModule = module {
    single { SettingsRepository(androidContext()) }

    // ExoPlayer (single instance, shared for live + VOD)
    single { PhoneLivePlayer(androidContext(), get()) }

    // ViewModels
    viewModel { PhoneViewModel(get()) }
    viewModel { PhoneSettingsViewModel(get()) }
    viewModel {
        PhoneSearchViewModel(
            channelDao = get(),
            categoryDao = get(),
            movieDao = get(),
            seriesDao = get(),
            historyDao = get(),
            profileDao = get(),
            sourceDao = get(),
            settings = get(),
            customize = get(),
            favoriteDao = get(),
        )
    }
    viewModel {
        PhoneLiveViewModel(
            channelDao = get(),
            categoryDao = get(),
            favoriteDao = get(),
            historyDao = get(),
            profileDao = get(),
            sourceDao = get(),
            settings = get(),
            customize = get(),
            epgDao = get(),
            player = get(),
        )
    }
    viewModel {
        PhoneMoviesViewModel(
            movieDao = get(),
            categoryDao = get(),
            favoriteDao = get(),
            historyDao = get(),
            progressDao = get(),
            sourceDao = get(),
            settings = get(),
            player = get(),
        )
    }
    viewModel {
        PhoneSeriesViewModel(
            seriesDao = get(),
            categoryDao = get(),
            favoriteDao = get(),
            historyDao = get(),
            progressDao = get(),
            sourceDao = get(),
            settings = get(),
            seriesRepository = get(),
            player = get(),
        )
    }
}
