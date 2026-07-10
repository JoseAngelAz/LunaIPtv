# Contributing to LunaIPtv

Thanks for your interest in improving LunaIPtv! Contributions of all sizes are welcome — bug fixes,
features, docs, or ideas.

## Ground rules

- LunaIPtv is a **player only**. Please keep its bring-your-own-source positioning — no bundled channels,
  playlists, or content, and nothing that facilitates access to unauthorized streams.
- It targets **Android TV** (D-pad / leanback first). Keep navigation remote-friendly.
- Match the existing code style and structure (Kotlin, Jetpack Compose for TV, Koin, Room).

## Project setup

1. Install **Android Studio** (a version that supports **AGP 9.x**).
2. Clone your fork and open the project; let Gradle sync.
3. Run the `app` config on an **Android TV emulator or device**, or build from the CLI:
   ```bash
   ./gradlew assembleDebug
   ```

A quick tour of the codebase lives in the [README](README.md), and the
full design/architecture is in [`extras/`](extras/).

## Workflow (branch → Pull Request)

We use a simple fork-and-PR flow:

1. **Fork** the repo (or, if you're a collaborator, create a branch directly).
2. Create a branch off `main`:
   ```bash
   git checkout -b feature/my-improvement main
   ```
3. Make your changes, commit, and push.
4. Open a Pull Request against `main` with a clear description of what and why.

## Code style

- **Kotlin** with Compose idioms (no imperative `findViewById`).
- **Koin** for dependency injection.
- **Room** for persistence.
- Prefer **named parameters** in function calls for readability.
- Keep functions focused; extract composables when a block grows.

## Reporting bugs

Open an issue with:
- Device model and Android version
- Steps to reproduce
- Expected vs. actual behaviour
- Screenshots or logs if possible

## License

By contributing, you agree that your contributions will be licensed under the **GPLv3**.
