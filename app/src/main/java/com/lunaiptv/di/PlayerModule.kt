package com.lunaiptv.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.lunaiptv.player.HeroPreviewEngine
import com.lunaiptv.player.LivePreviewEngine
import com.lunaiptv.player.OwnTVPlayer

/** App-wide libmpv player. */
val playerModule = module {
    // Tails own-process logcat for MediaCodec/AudioTrack errors the engines can't expose.
    single { com.lunaiptv.player.PlayerDiagnostics() }
    // Per-item VOD engine pins made with the player's gear toggle (VOD counterpart of ForceMpvStore).
    single { com.lunaiptv.core.player.VodEngineStore(androidContext()) }
    // context, settings, connectivity, okHttpClient (ExoPlayer image-sub handoff), diagnostics, proxyHolder, vodEngineStore
    single { OwnTVPlayer(androidContext(), get(), get(), get(), get(), get(), get()) }
    // ExoPlayer engine for the fast Live preview pane (mpv stays the full/fullscreen player).
    single { LivePreviewEngine(androidContext(), get(), get()) }
    // Muted ExoPlayer engine for the Home hero preview.
    single { HeroPreviewEngine(androidContext(), get()) }
}
