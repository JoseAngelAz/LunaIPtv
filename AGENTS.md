# LunaIPtv - Agent Instructions

## Project Overview
Android TV IPTV player forked from OwnTV by Jose Angel Azucena Mendez for personal learning.
- **Package**: `com.lunaiptv`
- **ApplicationId**: `com.lunaiptv`
- **Namespace**: `com.lunaiptv`
- **Min SDK**: 26 | **Target SDK**: 36 | **Compile SDK**: 36
- **Version**: 1.0.0-beta
- **Language**: Kotlin + Jetpack Compose for TV
- **License**: GPLv3

## Tech Stack
- **UI**: Jetpack Compose for TV (tv-material3)
- **Video**: Dual engine — libmpv (default) + ExoPlayer/Media3
- **Database**: Room (schema version 13, class `OwnTVDatabase` — DO NOT rename, breaks schema chain)
- **DI**: Koin
- **Networking**: OkHttp
- **Images**: Coil (50MB disk cache, 15% memory cache)
- **DataStore**: Preferences for settings

## Build Command
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat compileDebugKotlin
```

## Critical: Persistent Identifiers (DO NOT CHANGE)
These store user data. Changing them loses all user settings/data:
- **Database name**: `owntv.db` (in `OwnTVDatabase.kt`)
- **DataStore names**: `owntv_settings`, `owntv_epg_sources`, `owntv_vod_engine`, `owntv_force_mpv`, `owntv_tmdb_overrides`, `owntv_customizations`, `owntv_pending_userdata`
- **Storage folder**: `OwnTV` (in `StorageAccess.kt`)
- **URI scheme**: `owntv://` (in `LauncherDeepLink.kt` + `AndroidManifest.xml`)
- **Backup file**: `owntv-backup.json`
- **TV provider key prefix**: `owntv:` (in `TvHomeRepository.kt`)
- **Room schema export class**: `OwnTVDatabase` (schemas under `app/schemas/com.lunaiptv.core.database.OwnTVDatabase/`)
- **TMDB Worker URL**: `owntv-tmdb-meta.xiannero.workers.dev`

## Critical: Room Schema Chain
The database class is `OwnTVDatabase` (NOT `LunaIPtvDatabase`). Renaming it breaks the Room schema export directory structure (schemas 1-13 live under `OwnTVDatabase/`). Never rename this class.

## Project Structure
```
app/src/main/java/com/lunaiptv/
├── core/
│   ├── backup/        — BackupManager, BackupCrypto, UserDataResolver
│   ├── customize/     — CustomizationStore (home row order)
│   ├── database/      — OwnTVDatabase, DAOs, entities, migrations
│   ├── epg/           — EpgSourceStore, EPG parsing
│   ├── launcher/      — Deep link handling (owntv:// scheme)
│   ├── metadata/      — TMDB integration (TmdbProvider, MetadataRepository)
│   ├── model/         — Domain models (Channel, Movie, Series, etc.)
│   ├── network/       — HttpClient wrapper
│   ├── player/        — VodEngineStore, ForceMpvStore
│   ├── repository/    — EpgRepository
│   ├── storage/       — StorageAccess (OwnTV folder)
│   ├── sync/          — M3U/Xtream import, SyncManager, ImportFinalizer
│   ├── tv/            — TvHomeRepository (Android TV Watch Next)
│   ├── update/        — UpdateManager (checks disabled for fork)
│   └── weather/       — WeatherRepository
├── di/                — Koin modules (AppModule, DatabaseModule, PlayerModule)
├── features/
│   ├── customize/     — Home row customization screen
│   ├── downloads/     — Download manager UI
│   ├── epg/           — EPG guide UI + EpgViewModel
│   ├── home/          — HomeScreen, HomeViewModel (parallelized loading)
│   ├── live/          — Live TV channel list + LiveViewModel
│   ├── movies/        — Movies list/grid + MovieViewModel
│   ├── search/        — Search UI + SearchViewModel
│   ├── series/        — Series browser (4-level) + SeriesViewModel
│   ├── settings/      — SettingsScreen (50+ options), all sub-screens
│   ├── setup/         — First-run wizard
│   ├── shell/         — Main shell (sidebar, top bar, navigation)
│   └── update/        — Update dialog (disabled, shows LunaIPtv branding)
├── player/            — OwnTVPlayer (mpv), LivePreviewEngine (Exo), PlayerHud
├── ui/
│   ├── components/    — Reusable UI (LunaIPtvButton, LunaIPtvIcon, etc.)
│   └── theme/         — Theme system (AccentColor, LunaIPtvColors, Animations)
└── OwnTVApp.kt        — Application class (Koin, Coil, WorkManager)
```

## Theme/Accent System
- **10 accent presets** + custom hex: TEAL, BLUE, VIOLET, GREEN, AMBER, ROSE, CRIMSON, INDIGO, LIME, ORANGE
- **3 theme modes**: SYSTEM, DARK, LIGHT
- **Accent picker**: Settings → Appearance → Accent — shows all presets as swatches + custom hex input
- **Cycle shortcut**: Long-press cycles through presets

## Localization
- **2 languages**: English (`values/strings.xml`) + Spanish (`values-es/strings.xml`)
- **~450+ string resources** in each language
- **Auto-detection**: App detects device locale on first launch
- **Language picker**: Settings → Appearance → Language

## Key Patterns
- **ViewModels**: Use `StateFlow` + `stateIn()` for UI state
- **TMDB enrichment**: Lazy/on-demand when detail screen opens, never bulk
- **Merge rule**: `providerField ?: tmdbField` (or reversed if TMDB_ONLY mode)
- **Audio**: mpv `dynaudnorm` filter for consistent volume, AudioFocus integration
- **Channel switch**: `clearVideoSurface()` only — no `stop()`/`lockCanvas()` (causes ANR)
- **Home loading**: 4 phases run in parallel with `coroutineScope { async {} }`

## Git Info
- **Remote**: `origin https://github.com/ahXN00/OwnTV.git`
- **User**: Jose Angel Azucena Mendez
- **Email**: lunaipTV@users.noreply.github.com

## Known Issues
- PiP/channel-switch visual bug: old channel frame can linger in corner (investigated, `clearVideoSurface()` approach — needs device testing)
- `OwnTVDatabase` class name kept as-is to preserve Room schema chain (cosmetic only, never user-visible)
