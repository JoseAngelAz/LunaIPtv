package com.lunaiptv.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.lunaiptv.core.launcher.LauncherProfilePublisher
import com.lunaiptv.core.tv.TvHomeRepository
import com.lunaiptv.core.launcher.LauncherIntegrationRepository
import com.lunaiptv.core.update.UpdateManager

/**
 * TV-specific Koin module. Provides Android TV launcher integration, the Watch Next publisher,
 * and the (currently disabled) update checker. Not needed for a phone build.
 */
val tvDataModule = module {
    // context, sourceDao, channelDao, movieDao, seriesDao, progressDao, tvProviderProgramDao, customize, settings
    single { TvHomeRepository(androidContext(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // planner, resolver, tvHomeRepository
    single { LauncherIntegrationRepository(get(), get(), get()) }
    single<LauncherProfilePublisher> { get<LauncherIntegrationRepository>() }
    // context, okHttpClient — in-app updates from GitHub Releases (disabled in fork)
    single { UpdateManager(androidContext(), get()) }
}
