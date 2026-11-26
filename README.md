# RSounds

**One Player, Multiple Worlds.**

RSounds is an open-source Android music application built around a hybrid listening model and a deep synchronized-lyrics experience.

The product direction combines a capable local music player, integrations with permitted online music sources, and a lyrics system that can display word-level timing when source data provides it.

> RSounds is an independent project. It is not affiliated with, endorsed by, or sponsored by Musixmatch, YouTube, or Google.

## Current implementation
- [x] Android Gradle project and Jetpack Compose application shell
- [x] Media3 ExoPlayer and MediaSessionService playback engine
- [x] MediaController connection with lock-screen/system media-session integration
- [x] Read-only MediaStore audio scan with Android-version-aware permission flow
- [x] Local track list and play/pause controls
- [x] Lyrics document, line and word models
- [x] Enhanced-LRC parser with millisecond timestamps and offsets
- [x] Safe LRCLIB Lyricsfile YAML parser with bounded input
- [x] Unit tests for word sync, line fallback, offsets and invalid input
- [x] GitHub Actions tests, debug APK assembly and artifact upload

The current app can scan and play audio already accessible through Android MediaStore. It is still an early build: Room persistence, advanced queue controls, remote provider adapters, translation UI, floating lyrics, Android Auto and the full design system remain in the roadmap.

## Product principles
- **Hybrid:** local music first, with online providers behind isolated adapters.
- **Deep lyrics:** word sync when available, line sync as fallback, plain text when no timing exists.
- **Honest sync:** never invent per-word timing when the source only provides line timing.
- **Provider independence:** one provider failure must not break local playback.
- **Reuse responsibly:** reuse, repair, refactor and integrate; preserve upstream provenance and licenses.
- **Authorized access:** use official APIs, SDKs or explicitly permitted protocols. Do not bypass subscriptions, ads, access controls or redistribute protected catalogs without permission.

## Technology
| Area | Foundation |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 ExoPlayer / MediaSession |
| Local library | Android MediaStore |
| Lyrics | RSounds models, Enhanced-LRC and Lyricsfile parser |
| Build | Gradle Kotlin DSL |
| Minimum Android | Android 8.0 (API 26), subject to feature-specific support |
| Compile SDK | Android 17 API 37.1 preview |
| Target SDK | Android 16 API 36 until Android 17 is stable |

## Build
Requirements: JDK 17, Android SDK Platform 37.1 preview and Build Tools 37.0.0.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/`. Hosted CI builds also publish a `RSounds-debug-apk` artifact.

## Repository map
```text
app/                 Android app, permissions, player connection and Compose UI
core/player/         Media3 playback service and system media session
core/lyrics/         Lyrics models, parsers and tests
provider/local/      Read-only MediaStore audio library
docs/                Architecture, audit, provenance and roadmap
.github/workflows/   CI verification and APK artifact publishing
```

## Legal
RSounds project code is licensed under GNU GPL-3.0-only unless a file or component states otherwise. Third-party code, libraries, fonts, artwork, lyrics, recordings and online services remain subject to their own licenses, rights and terms.

See [Architecture](docs/ARCHITECTURE.md), [Technical audit](docs/TECHNICAL_AUDIT.md), [Provenance](docs/PROVENANCE.md), [Roadmap](docs/ROADMAP.md) and [Contributing](CONTRIBUTING.md).
