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
import com.lunaiptv.core.launcher.LauncherIntegrationRepository
import com.lunaiptv.core.launcher.LauncherLaunchResolver
import com.lunaiptv.core.launcher.LauncherRecommendationPlanner
import com.lunaiptv.core.repository.EpgRepository
import com.lunaiptv.core.repository.SeriesRepository
import com.lunaiptv.core.repository.SourceRepository
import com.lunaiptv.core.tv.TvHomeRepository
import com.lunaiptv.core.update.UpdateManager
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
            .connectTimeout(15, TimeUnit.SECONDS)  // fast fail on dead host
            .readTimeout(20, TimeUnit.SECONDS)    // detect mid-sync disconnect quickly
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)       // let SyncManager handle retries, not OkHttp
            // Global proxy (Approach 1): a ProxySelector/Authenticator that read the live snapshot, so
            // enabling/disabling the proxy takes effect immediately. Proxy off = DIRECT = exact prior
            // behavior. Credentials are never logged.
            .proxySelector(proxyHolder.proxySelector)
            .proxyAuthenticator(proxyHolder.proxyAuthenticator)
            // Force HTTP/1.1. Several IPTV panels / EPG hosts (and their CDNs) have flaky HTTP/2 stacks
            // that send RST_STREAM(PROTOCOL_ERROR) on large/slow responses — e.g. big EPG XML downloads
            // (#17) — which OkHttp surfaces as "stream was reset: PROTOCOL_ERROR". HTTP/1.1 sidesteps it
            // with no real downside for our mostly-single-stream downloads.
            .protocols(listOf(Protocol.HTTP_1_1))
            // Default a player-style UA for any request that didn't set one (e.g. Coil image loads),
            // since some IPTV panels reject the stock OkHttp UA. Per-source UAs still override this.
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
    // store, sourceDao, epgRepository
    single { com.lunaiptv.core.epg.EpgMigration(get(), get(), get()) }
    single { M3uParser() }
    single { XtreamClient(get()) }
    // TMDB metadata enrichment (plan §4): one provider, three tiers resolved from SettingsRepository.
    single<com.lunaiptv.core.metadata.MetadataProvider> {
        com.lunaiptv.core.metadata.TmdbProvider(get(), get())
    }
    // provider, metadataDao, settings, overrideStore — the on-demand resolve + cache orchestrator (plan §7, §11.2 U5b).
    single { com.lunaiptv.core.metadata.MetadataRepository(get(), get(), get(), get()) }
    // Per-content TMDB name overrides (plan §11.2 U5b): DataStore side-store, no Room schema change.
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
    // context, channelDao, movieDao, seriesDao, profileDao, favoriteDao, historyDao, progressDao, contentOrderDao
    single { UserDataResolver(androidContext(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // sourceDao, syncManager, userDataResolver
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
    // epgDao, httpClient, xtreamClient, channelDao, customize, settings, context, db, bulkInsertHelper
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
    // seriesDao, sourceDao, xtreamClient, userDataResolver
    single { SeriesRepository(get(), get(), get(), get()) }
    // sourceDao, movieDao, seriesDao, progressDao
    single { LauncherRecommendationPlanner(get(), get(), get(), get()) }
    // sourceDao, channelDao, movieDao, seriesDao, progressDao
    single { LauncherLaunchResolver(get(), get(), get(), get(), get()) }
    // context, sourceDao, channelDao, movieDao, seriesDao, progressDao, tvProviderProgramDao, customize, settings
    single { TvHomeRepository(androidContext(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // planner, resolver, tvHomeRepository
    single { LauncherIntegrationRepository(get(), get(), get()) }
    // context, downloadDao, okHttpClient, settings
    single { DownloadManager(androidContext(), get(), get(), get()) }
    // profileDao, sourceDao, settings, customizationStore, userDataResolver, epgSourceStore,
    // launcherIntegrationRepository, forceMpvStore, vodEngineStore
    single { BackupManager(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // context, okHttpClient — in-app updates from GitHub Releases
    single { UpdateManager(androidContext(), get()) }
    single { CatalogSyncScheduler(androidContext()) }
    single { EpgSyncScheduler(androidContext()) }
}
