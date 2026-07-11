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
- **Database**: Room (schema version 13, class `LunaIPtvDatabase`)
- **DI**: Koin
- **Networking**: OkHttp
- **Images**: Coil (50MB disk cache, 15% memory cache)
- **DataStore**: Preferences for settings

## Build Command
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat compileDebugKotlin
```

## Build Configuration
- **R8/ProGuard**: Enabled for release builds (`isMinifyEnabled = true`, `isShrinkResources = true`)
- **ProGuard rules**: `app/proguard-rules.pro` (Room, Koin, libmpv, Media3, OkHttp, Coil, YouTube Player)
- **Network security**: `network_security_config.xml` — cleartext (HTTP) allowed only in debug builds
- **Signing**: Release signing via env vars (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)

## Critical: Persistent Identifiers
These are the canonical identifiers for the LunaIPtv fork:
- **Database name**: `lunaiptv.db` (in `LunaIPtvDatabase.kt`)
- **DataStore names**: `lunaiptv_settings`, `lunaiptv_epg_sources`, `lunaiptv_vod_engine`, `lunaiptv_force_mpv`, `lunaiptv_tmdb_overrides`, `lunaiptv_customizations`, `lunaiptv_pending_userdata`
- **Storage folder**: `LunaIPtv` (in `StorageAccess.kt`)
- **URI scheme**: `lunaiptv://` (in `LauncherDeepLink.kt` + `AndroidManifest.xml`)
- **Backup file**: `lunaiptv-backup.json`
- **TV provider key prefix**: `lunaiptv:` (in `TvHomeRepository.kt`)
- **Room schema export class**: `LunaIPtvDatabase` (schemas under `app/schemas/com.lunaiptv.core.database.LunaIPtvDatabase/`)
- **TMDB Worker URL**: `lunaiptv-tmdb-meta.xiannero.workers.dev`

## Critical: Room Schema Chain
The database class is `LunaIPtvDatabase`. The schema export directory must match: `app/schemas/com.lunaiptv.core.database.LunaIPtvDatabase/`. If you rename this class, also rename the schema directory to match.

## Project Structure
```
app/src/main/java/com/lunaiptv/
├── core/
│   ├── backup/        — BackupManager, BackupCrypto, UserDataResolver
│   ├── customize/     — CustomizationStore (home row order)
│   ├── database/      — LunaIPtvDatabase, DAOs, entities, migrations
│   ├── epg/           — EpgSourceStore, EPG parsing
│   ├── launcher/      — Deep link handling (lunaiptv:// scheme)
│   ├── metadata/      — TMDB integration (TmdbProvider, MetadataRepository)
│   ├── model/         — Domain models (Channel, Movie, Series, etc.)
│   ├── network/       — HttpClient wrapper
│   ├── player/        — VodEngineStore, ForceMpvStore
│   ├── repository/    — EpgRepository
│   ├── storage/       — StorageAccess (LunaIPtv folder)
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
├── player/            — LunaIPtvPlayer (mpv), LivePreviewEngine (Exo), PlayerHud
├── ui/
│   ├── components/    — Reusable UI (LunaIPtvButton, LunaIPtvIcon, etc.)
│   └── theme/         — Theme system (AccentColor, LunaIPtvColors, Animations)
└── LunaIPtvApp.kt    — Application class (Koin, Coil, WorkManager)
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
- `LunaIPtvDatabase` class name and schema directory must stay in sync (renaming the class requires renaming the schema export directory)
