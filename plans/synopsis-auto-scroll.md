# Auto-Scroll Synopsis for Series — Implementation Plan

## Goal
Add auto-scrolling text to the synopsis/plot displayed in the TV version's series detail views (grid preview pane and episode detail pane). The text scrolls down slowly, pauses, then scrolls back up, allowing the user to read long synopses without manual D-pad scrolling. Speed is configurable in Settings.

## Scope
- **Scope IN**: Series grid preview pane synopsis, episode detail pane synopsis
- **Scope OUT**: Movie synopsis (same pattern exists in MoviesScreen — can be added later), phone version

## Files to Modify (9 files)

### 1. `core/src/main/java/com/lunaiptv/ui/theme/SynopsisScrollSpeed.kt` (NEW)
Create enum in `:core` module (alongside `AnimationLevel`):
```kotlin
enum class SynopsisScrollSpeed(val label: String, val dpPerSec: Int) {
    OFF("Off", 0),
    SLOW("Slow", 15),
    MEDIUM("Medium", 30),
    FAST("Fast", 50);
}
```
- `dpPerSec` = 0 means disabled
- Default = OFF

### 2. `core/src/main/java/com/lunaiptv/features/settings/data/SettingsRepository.kt`
- Add key: `val SYNOPSIS_SCROLL_SPEED = stringPreferencesKey("synopsis_scroll_speed")`
- Add flow:
  ```kotlin
  val synopsisScrollSpeed: Flow<SynopsisScrollSpeed> = context.dataStore.data.map { prefs ->
      prefs[Keys.SYNOPSIS_SCROLL_SPEED]?.let { runCatching { SynopsisScrollSpeed.valueOf(it) }.getOrNull() }
          ?: SynopsisScrollSpeed.OFF
  }
  ```
- Add setter:
  ```kotlin
  suspend fun setSynopsisScrollSpeed(speed: SynopsisScrollSpeed) {
      context.dataStore.edit { it[Keys.SYNOPSIS_SCROLL_SPEED] = speed.name }
  }
  ```
- Add `Keys.SYNOPSIS_SCROLL_SPEED` to `backupStringKeys` list

### 3. `app/src/main/java/com/lunaiptv/features/settings/SettingsViewModel.kt`
- Expose:
  ```kotlin
  val synopsisScrollSpeed: StateFlow<SynopsisScrollSpeed> = settings.synopsisScrollSpeed
      .stateIn(viewModelScope, SharingStarted.Eagerly, SynopsisScrollSpeed.OFF)
  fun setSynopsisScrollSpeed(speed: SynopsisScrollSpeed) {
      viewModelScope.launch { settings.setSynopsisScrollSpeed(speed) }
  }
  ```

### 4. `app/src/main/java/com/lunaiptv/features/shell/components/SettingsScreen.kt`
- Add state: `val synopsisScrollSpeed by settingsVm.synopsisScrollSpeed.collectAsStateWithLifecycle()` + `var showSynopsisScroll by remember { mutableStateOf(false) }`
- Add `SettingsRow` in the **Playback** section (after Auto-play next, before Catch-up time):
  ```kotlin
  SettingsRow(
      tone = TileTone.SECONDARY, icon = LunaIPtvIcon.SORT, // or a new icon
      title = stringResource(R.string.settings_synopsis_scroll),
      desc = stringResource(R.string.settings_synopsis_scroll_desc),
      chip = stringResource(synopsisScrollSpeed.labelRes),
      chipTone = if (synopsisScrollSpeed != SynopsisScrollSpeed.OFF) TileTone.PRIMARY else TileTone.SECONDARY,
      onClick = { showSynopsisScroll = true },
  )
  ```
- Add picker dialog (following AnimationLevel/Theme picker pattern):
  ```kotlin
  if (showSynopsisScroll) {
      PickerDialog(
          title = stringResource(R.string.settings_synopsis_scroll),
          options = SynopsisScrollSpeed.entries.map { it.label },
          selectedIndex = SynopsisScrollSpeed.entries.indexOf(synopsisScrollSpeed),
          onSelect = { settingsVm.setSynopsisScrollSpeed(SynopsisScrollSpeed.entries[it]); showSynopsisScroll = false },
          onDismiss = { showSynopsisScroll = false },
      )
  }
  ```

### 5. `app/src/main/res/values/strings.xml` (English)
Add:
```xml
<string name="settings_synopsis_scroll">Synopsis auto-scroll</string>
<string name="settings_synopsis_scroll_desc">Slowly scroll synopsis text so you can read it without D-pad scrolling</string>
<string name="synopsis_scroll_off">Off</string>
<string name="synopsis_scroll_slow">Slow</string>
<string name="synopsis_scroll_medium">Medium</string>
<string name="synopsis_scroll_fast">Fast</string>
```

### 6. `app/src/main/res/values-es/strings.xml` (Spanish)
Add:
```xml
<string name="settings_synopsis_scroll">Desplazamiento automático de sinopsis</string>
<string name="settings_synopsis_scroll_desc">Desplaza lentamente el texto de la sinopsis para poder leerlo sin usar el D-pad</string>
<string name="synopsis_scroll_off">Desactivado</string>
<string name="synopsis_scroll_slow">Lento</string>
<string name="synopsis_scroll_medium">Medio</string>
<string name="synopsis_scroll_fast">Rápido</string>
```

### 7. `app/src/main/java/com/lunaiptv/features/series/SeriesScreen.kt`
**The core UI change.** Add a `@Composable` `AutoScrollText` and replace plain `Text(plot, ...)` in two places.

#### a) Add `AutoScrollText` private composable (top of file or bottom):
```kotlin
@Composable
private fun AutoScrollText(
    text: String,
    speed: SynopsisScrollSpeed,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LunaIPtvTheme.colors.onSurfaceVariant,
) {
    if (speed == SynopsisScrollSpeed.OFF || text.isBlank()) {
        Text(text, style = style, color = color, modifier = modifier)
        return
    }

    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    // Auto-scroll loop
    LaunchedEffect(text, speed) {
        // Wait for layout to measure max scroll
        snapshotFlow { scrollState.maxValue }.first { it > 0 }
        val maxScroll = scrollState.maxValue
        val durationMs = with(density) {
            (maxScroll.toFloat() / speed.dpPerSec * 160).toInt() // dpPerSec → ms
        }.coerceAtLeast(2000)

        while (isActive) {
            // Scroll to bottom
            scrollState.animateScrollTo(maxScroll, tween(durationMs, easing = LinearEasing))
            delay(2500) // pause at bottom
            // Scroll back to top
            scrollState.animateScrollTo(0, tween(durationMs, easing = LinearEasing))
            delay(2500) // pause at top
        }
    }

    Box(modifier) {
        Column(Modifier.verticalScroll(scrollState)) {
            Text(text, style = style, color = color)
        }
    }
}
```

#### b) Replace in **series grid preview pane** (line ~545-549):
Before:
```kotlin
if (!plot.isNullOrBlank()) {
    item("plot") {
        Spacer(Modifier.height(8.dp))
        Text(plot, style = MaterialTheme.typography.bodyMedium, color = LunaIPtvTheme.colors.onSurfaceVariant)
    }
}
```
After:
```kotlin
if (!plot.isNullOrBlank()) {
    item("plot") {
        Spacer(Modifier.height(8.dp))
        AutoScrollText(plot, synopsisScrollSpeed, modifier = Modifier.heightIn(max = 150.dp))
    }
}
```

#### c) Replace in **episode detail pane** (line ~755-757):
Before:
```kotlin
if (!plot.isNullOrBlank()) {
    Spacer(Modifier.height(8.dp))
    Text(plot, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
}
```
After:
```kotlin
if (!plot.isNullOrBlank()) {
    Spacer(Modifier.height(8.dp))
    AutoScrollText(plot, synopsisScrollSpeed, modifier = Modifier.heightIn(max = 150.dp))
}
```

#### d) Pass `synopsisScrollSpeed` into `EpisodeDetailPane`:
Add `synopsisScrollSpeed: SynopsisScrollSpeed` parameter to `EpisodeDetailPane` and `SeriesGrid`.

#### e) Read setting at `SeriesScreen` top level:
```kotlin
val synopsisScrollSpeed by settingsRepository.synopsisScrollSpeed.collectAsStateWithLifecycle(SynopsisScrollSpeed.OFF)
```
Pass down to `SeriesGrid` and `EpisodeView` → `EpisodeDetailPane`.

### 8. `AGENTS.md`
Add to **Key Patterns** section:
```
- **Synopsis auto-scroll**: `AutoScrollText` composable in `SeriesScreen.kt` scrolls synopsis text down/up at configurable speed (Off/Slow/Medium/Fast). Setting stored in `SettingsRepository` as `SYNOPSIS_SCROLL_SPEED` string key. `heightIn(max=150.dp)` caps the scrollable area. 2.5s pause at each end.
```

### 9. Git Commit
Commit all changes with message: `feat: auto-scroll series synopsis with configurable speed`

## Implementation Order
1. Create `SynopsisScrollSpeed` enum (`:core`)
2. Add setting to `SettingsRepository` + backup key
3. Add to `SettingsViewModel`
4. Add English + Spanish strings
5. Add settings row + dialog in `SettingsScreen`
6. Add `AutoScrollText` composable + wire into `SeriesScreen`
7. Update `AGENTS.md`
8. Compile check: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat compileDebugKotlin`
9. Git commit + push

## Key Design Decisions
- **Enum in `:core`** (not `:app`) — follows `AnimationLevel` pattern, keeps `:core` free of TV-specific APIs
- **`tween` + `LinearEasing`** — smooth constant-speed scroll, no acceleration/deceleration
- **2.5s pause at each end** — enough time to read the last/first lines before reversing
- **`heightIn(max = 150.dp)`** — caps the synopsis box height so it doesn't push episodes off screen; the auto-scroll reveals the rest
- **`snapshotFlow { scrollState.maxValue }.first { it > 0 }`** — waits for layout measurement before starting animation, avoids 0-length scroll edge case
- **`animateScrollTo` not `scrollTo`** — frame-by-frame `scrollTo` in a while loop would fight Compose layout; `animateScrollTo` uses the built-in animation system for buttery smoothness
- **Setting default: OFF** — opt-in, since not everyone wants auto-scroll. Once enabled, it's persistent.
