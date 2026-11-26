# RSounds

**One Player, Multiple Worlds.**

RSounds is an open-source Android music player designed to bring local music, online music services, synchronized lyrics, and in-car playback into one coherent experience.

The project aims to combine a clean listening experience with a modular foundation that can grow over time. Its interface will stay simple at the center, with a distinctive circular identity and carefully animated supporting elements.

> RSounds is an independent project. It is not affiliated with, endorsed by, or sponsored by Musixmatch, YouTube, or Google.

## Vision

- One home for music stored on your device and music available through supported online providers.
- A flexible lyrics experience, including synchronized lyrics when a lawful source is available.
- Reliable playback controls and media-session integration.
- A focused, distraction-free interface with thoughtful motion and accessibility.
- Android Auto support that follows Android for Cars requirements.

## Planned features

- [ ] Local audio library and media scanning
- [ ] Playback engine, queue, playlists, and audio focus
- [ ] Synchronized lyrics and manual lyric adjustment
- [ ] Provider integration layer for services with permitted APIs or SDKs
- [ ] Search, favorites, history, and library management
- [ ] Android Auto / compatible car display experience
- [ ] Themes, motion, and accessibility options

Features are planned, not yet implemented unless marked otherwise.

## Technology direction

| Area | Planned technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 |
| Architecture | Modular Android application |
| Car experience | Android for Cars App Library / supported media integrations |
| Build | Gradle |

Technology choices may evolve as implementation and compatibility requirements become clearer.

## Repository structure

The project will grow toward a modular structure:

```text
rsoundsapp/
├── app/
├── core/
│   ├── player/
│   ├── lyrics/
│   ├── network/
│   └── design/
├── features/
│   ├── home/
│   ├── library/
│   ├── search/
│   ├── lyrics/
│   └── settings/
├── automotive/
│   └── android-auto/
├── docs/
├── CONTRIBUTING.md
├── CHANGELOG.md
├── LICENSE
└── README.md
```

This is the intended direction; folders will be added as their modules are implemented.

## Getting started

The Android application source and build instructions are still being established. Setup instructions will be added when the first runnable project skeleton is committed.

## Contributing

Contributions, issue reports, and thoughtful feature proposals are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## Legal and third-party services

- RSounds project code is distributed under the GNU General Public License v3.0 only (GPL-3.0-only), unless a file or component states otherwise.
- Third-party libraries, APIs, artwork, and service integrations remain subject to their own licenses and terms.
- Online music and lyrics integrations must use authorized APIs, SDKs, or other permitted access methods.
- RSounds will not bundle proprietary applications, bypass subscriptions or access controls, or redistribute copyrighted catalogs without permission.

## Project status

Early development — currently establishing project documentation and architecture before the first Android implementation.
