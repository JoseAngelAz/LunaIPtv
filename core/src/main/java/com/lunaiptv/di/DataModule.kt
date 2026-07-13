package com.lunaiptv.di

import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.lunaiptv.core.backup.BackupManager
import com.lunaiptv.core.database.BulkInsertHelper
import com.lunaiptv.core.backup.UserDataResolver
import com.lunaiptv.core.customize.CustomizationStore
import com.lunaiptv.core.download.DownloadManager
import com.lunaiptv.core.network.ConnectivityObserver
import com.lunaiptv.core.network.HttpClient
import com.lunaiptv.core.parser.M3uParser
import com.lunaiptv.core.parser.XtreamClient
import com.lunaiptv.core.launcher.LauncherLaunchResolver
import com.lunaiptv.core.launcher.LauncherRecommendationPlanner
import com.lunaiptv.core.launcher.LauncherProfilePublisher
import com.lunaiptv.core.repository.EpgRepository
import com.lunaiptv.core.repository.SeriesRepository
import com.lunaiptv.core.repository.SourceRepository
import com.lunaiptv.core.sync.SyncManager
import com.lunaiptv.core.sync.work.CatalogSyncScheduler
import com.lunaiptv.core.sync.work.EpgSyncScheduler
import com.lunaiptv.core.weather.WeatherRepository
import java.util.concurrent.TimeUnit

/** Networking, parsers, sync engine, and repositories (Phase 5). */
val dataModule = module {
    // Live snapshot of the global proxy. Backs OkHttp's ProxySelector/Authenticator AND mpv's http-proxy,
    // so the proxy can be toggled at runtime without rebuilding the singleton OkHttpClient below.
    single { com.lunaiptv.core.network.ProxyConfigHolder(get<com.lunaiptv.features.settings.data.SettingsRepository>().proxyConfig) }
    single {
        val proxyHolder = get<com.lunaiptv.core.network.ProxyConfigHolder>()
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .proxySelector(proxyHolder.proxySelector)
            .proxyAuthenticator(proxyHolder.proxyAuthenticator)
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val req = chain.request()
                val out = if (req.header("User-Agent").isNullOrBlank()) {
                    req.newBuilder().header("User-Agent", HttpClient.DEFAULT_USER_AGENT).build()
                } else {
                    req
                }
                chain.proceed(out)
            }
            .build()
    }
    single { HttpClient(get()) }
    single { ConnectivityObserver(androidContext()) }
    single { CustomizationStore(androidContext()) }
    single { com.lunaiptv.core.epg.EpgSourceStore(androidContext()) }
    single { com.lunaiptv.core.player.ForceMpvStore(androidContext()) }
    single { com.lunaiptv.core.player.ExternalPlayerLauncher(androidContext()) }
    single { com.lunaiptv.core.epg.EpgMigration(get(), get(), get()) }
    single { M3uParser() }
    single { XtreamClient(get()) }
    single<com.lunaiptv.core.metadata.MetadataProvider> {
        com.lunaiptv.core.metadata.TmdbProvider(get(), get())
    }
    single { com.lunaiptv.core.metadata.MetadataRepository(get(), get(), get(), get()) }
    single { com.lunaiptv.core.metadata.MetadataOverrideStore(androidContext()) }
    single { WeatherRepository(get(), get()) }
    single { BulkInsertHelper(get()) }
    single {
        com.lunaiptv.core.sync.ImportFinalizer(
            channelDao = get(),
            movieDao = get(),
            seriesDao = get(),
            db = get(),
            bulkInsertHelper = get(),
        )
    }
    single { UserDataResolver(androidContext(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SourceRepository(get(), get(), get()) }
    single {
        SyncManager(
            context = androidContext(),
            sourceDao = get(),
            categoryDao = get(),
            channelDao = get(),
            movieDao = get(),
            seriesDao = get(),
            xtream = get(),
            m3u = get(),
            http = get(),
            bulkInsertHelper = get(),
        )
    }
    single {
        EpgRepository(
            epgDao = get(),
            http = get(),
            xtream = get(),
            channelDao = get(),
            customize = get(),
            settings = get(),
            context = androidContext(),
            db = get(),
            bulkInsertHelper = get(),
        )
    }
    single { SeriesRepository(get(), get(), get(), get()) }
    single { LauncherRecommendationPlanner(get(), get(), get(), get()) }
    single { LauncherLaunchResolver(get(), get(), get(), get(), get()) }
    single<LauncherProfilePublisher> { com.lunaiptv.core.launcher.NoOpLauncherProfilePublisher() }
    single { DownloadManager(androidContext(), get(), get(), get()) }
    single { BackupManager(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { CatalogSyncScheduler(androidContext()) }
    single { EpgSyncScheduler(androidContext()) }
}
