# RSounds Roadmap

Sequenced by dependency and risk, not visual feature count.

## P0 — Build foundation
- [x] Android Gradle project and Compose shell
- [x] Media3 ExoPlayer + MediaSessionService lifecycle boundary
- [x] Lyrics models
- [x] Enhanced-LRC parser with millisecond timestamps
- [x] Bounded safe Lyricsfile YAML parser
- [x] Unit-test fixtures for word/line sync, offsets and invalid documents
- [x] GitHub Actions workflow
- [ ] Confirm CI result and resolve toolchain issues

## P1 — Local listening
- [ ] MediaStore scanner and permission flow
- [ ] Room library and stable track identity
- [ ] MediaController and real playback controls
- [ ] Queue, shuffle, repeat, history and favorites
- [ ] Notification, audio focus, headset and Bluetooth behavior
- [ ] Embedded LRC/TTML/QRC reader and local lyric editor

## P2 — Deep lyrics
- [ ] LRCLIB provider adapter and polite rate-limit handling
- [ ] Track-to-lyrics matching with duration/version confidence
- [ ] Timeline state driven by actual playback position
- [ ] Word, line and plain-text fallback states
- [ ] Translation and romanization provider contracts
- [ ] Manual offset correction and local persistence
- [ ] Floating lyrics with explicit overlay permission
- [ ] Song identification integration and privacy controls

## P3 — Hybrid sources
- [ ] Provider contract and capability model
- [ ] Navidrome and Jellyfin adapters for user-owned libraries
- [ ] Official YouTube discovery/player integration within policy
- [ ] Cross-provider search with source-specific result grouping
- [ ] Artwork, metadata and playlist import where permitted

## P4 — Premium experience
- [ ] Now Playing UI and animated lyrics
- [ ] Equalizer/audio effects where supported
- [ ] Android Auto / Android for Cars
- [ ] Personalized discovery and smart playlists
- [ ] Backup, restore, diagnostics and accessibility pass
- [ ] Release signing, privacy notice, security and distribution review
