<p align="center">
  <img src="extras/logo.png" alt="LunaIPtv" width="360">
</p>

<p align="center">
  <b>Your own IPTV player for Android TV</b><br>
  <sub>Fast · modern · remote-first — bring your own M3U or Xtream sources</sub>
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%20TV-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose for TV" src="https://img.shields.io/badge/Jetpack%20Compose-for%20TV-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Player" src="https://img.shields.io/badge/engines-libmpv%20%2B%20ExoPlayer-FB8C00">
  <img alt="License" src="https://img.shields.io/badge/license-GPLv3-blue">
  <img alt="Built with the help of AI" src="https://img.shields.io/badge/built%20with-the%20help%20of%20AI-8A2BE2">
</p>

<p align="center">
  <a href="https://github.com/ahXN00/OwnTV/actions/workflows/android.yml">
    <img alt="Android CI" src="https://github.com/ahXN00/OwnTV/actions/workflows/android.yml/badge.svg">
  </a>
</p>

---

LunaIPtv is a native **Android TV** IPTV **player** built with Kotlin, Jetpack Compose for TV, and a
**dual playback engine** — **libmpv (FFmpeg)** for movies/series and maximum compatibility, **ExoPlayer
(Media3)** for near-instant Live TV. It's a *player only* — you bring your own Xtream login or M3U playlist
(by **URL or a local `.m3u`/`.m3u8` file** on the device), and LunaIPtv gives you a fast, modern, remote-first
way to browse and watch them.

> **Disclaimer:** LunaIPtv does **not** provide any channels, playlists, subscriptions, streams, or media content.
> You are responsible for adding your own legally accessible sources.

> **Note:** This project was originally named **OwnTV**. It has been forked and rebranded as **LunaIPtv** for personal learning about IPTV technology. **It is not intended for selling IPTV services.**

---

## Credits

**Original project by:** [ahXN00](https://github.com/ahXN00/OwnTV)

**Fork & rebrand by:** **Jose Angel Azucena Mendez** — Forked for personal learning and educational purposes about IPTV, Android TV development, and media playback technologies.

This project is **open source** under the **GNU General Public License v3 (GPLv3)**. Anyone is free to:
- **Use** the software for any purpose
- **Copy** and distribute the software
- **Modify** and create derivative works
- **Sell** or gift the software (under the same license terms)

See the [LICENSE](LICENSE) file for the full GPLv3 text.

---

## Community

Questions, ideas, bug reports — or just want to follow along? **Join the LunaIPtv Telegram group:**

### [t.me/owntvplayer](https://t.me/owntvplayer)

Scan to join from your phone:

<a href="https://t.me/owntvplayer"><img src="extras/telegram_qr_code.jpg" alt="Scan to join the LunaIPtv Telegram group" width="170"></a>

---

## Features

- **Dual playback engine** — libmpv (FFmpeg) for maximum compatibility, ExoPlayer (Media3) for near-instant Live TV
- **Live TV** — full-screen playback with channel list sidebar, EPG, catch-up, and time-shift
- **Movies & Series** — browse and play VOD content with metadata, trailers, and subtitle support
- **Profiles** — multiple user profiles with optional PIN protection
- **Backup & Restore** — full app state backup including settings, sources, and watch history
- **EPG (Electronic Program Guide)** — grid-based TV guide with programme details
- **Search** — full-text search across movies, series, and live channels
- **Watch Next** — Android TV homescreen integration for continuing playback
- **Multi-language** — English and Spanish with in-app language switching
- **Theme engine** — multiple color themes with custom accent color picker
- **UI Zoom** — adjustable UI scaling for accessibility
- **Catch-up & Time-shift** — rewind live TV and jump to past programmes
- **Subtitle support** — SRT, SSA/ASS, PGS, VOBSUB, and DVB bitmap subtitles
- **Audio track selection** — choose between multiple audio tracks per stream

---

## Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.3.10 |
| **UI** | Jetpack Compose for TV (tv-material3) |
| **Navigation** | Compose Navigation (navController) |
| **DI** | Koin (BOM-managed) |
| **Database** | Room + KSP |
| **Networking** | OkHttp |
| **Image loading** | Coil (Compose) |
| **Media playback** | libmpv (FFmpeg) + ExoPlayer (Media3) |
| **YouTube trailers** | YouTube IFrame Player (WebView) |
| **Preferences** | DataStore Preferences |
| **Background sync** | WorkManager |
| **Architecture** | MVVM with Repository pattern |
| **Target** | Android TV (API 26+) |

---

## Architecture

```
app/src/main/java/com/lunaiptv/
├── core/                    # Shared infrastructure
│   ├── data/                # Database (Room), DataStore
│   ├── sync/                # WorkManager sync workers
│   ├── tv/                  # Android TV homescreen (Watch Next)
│   ├── update/              # In-app update checker
│   └── util/                # Utilities (LocaleHelper, etc.)
├── features/                # Feature modules (screen + ViewModel)
│   ├── home/                # Home screen (On Now, Continue Watching)
│   ├── live/                # Live TV playback
│   ├── movies/              # Movies browse + player
│   ├── series/              # Series browse + player
│   ├── settings/            # Settings screens
│   ├── setup/               # Onboarding wizard
│   ├── profiles/            # Multi-profile support
│   └── shell/               # Main shell (sidebar, top bar)
├── player/                  # Playback engines (mpv + ExoPlayer)
├── ui/                      # Theme, colors, typography
└── MainActivity.kt          # Entry point
```

---

## Building

```bash
# Clone the repository
git clone https://github.com/ahXN00/OwnTV.git
cd OwnTV

# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

**Requirements:**
- Android Studio Ladybug or later
- JDK 17
- Android SDK 36
- Kotlin 2.3.10+

> **New to Android development?** See the [**Installation Guide (INSTALL.md)**](INSTALL.md) for step-by-step instructions with screenshots.

> **Need deployment help?** See the [**Deployment Guide (DEPLOY.md)**](DEPLOY.md) for CI/CD, keystore setup, and distribution options.

---

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

```
Copyright (C) 2024 ahXN00
Copyright (C) 2025 Jose Angel Azucena Mendez (fork)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```

---

## Disclaimer

This software is provided "as is" without warranty of any kind. The developers are not responsible for:
- The legality of IPTV sources used with this player
- Any content accessed through third-party playlists
- Any damages arising from the use of this software

Users are solely responsible for ensuring they have the legal right to access any content played through LunaIPtv.
