# RSounds Roadmap

Sequenced by dependency and risk, not visual feature count.

## P0 — Build foundation
- [x] Android Gradle project and Compose shell
- [x] Media3 ExoPlayer + MediaSessionService lifecycle boundary
- [x] Lyrics models and parsers
- [x] Bounded safe Lyricsfile YAML parser
- [x] Playback-position lyric timeline and correction calculation
- [x] GitHub Actions hosted build/test verification and APK artifact

## P1 — Local listening
- [x] MediaStore audio scanner and runtime permission flow
- [x] MediaController connection and local playback controls
- [ ] Room library index and stable track identity
- [ ] Queue persistence, shuffle, repeat, history and favorites
- [ ] Notification permission prompt and full audio focus/device matrix
- [ ] Embedded LRC/TTML/QRC reader and local lyric editor

## P2 — Deep lyrics
- [x] Plain, line and word sync levels with honest fallbacks
- [x] Playback-position-driven line/word timeline and correction math
- [x] LRCLIB signature lookup with User-Agent identification
- [x] Request serialization, 300 ms pacing and Retry-After cooldown
- [x] Word-sync → line-sync → plain-text fallback
- [x] Now Playing lyrics panel driven by Media3 playback position
- [ ] Persist manual offset correction per track
- [ ] Translation and romanization provider contracts
- [ ] Floating lyrics with explicit overlay permission
- [ ] Song identification integration and privacy controls

## P3 — Hybrid sources
- [ ] Provider capability contract and common search model
- [ ] Navidrome and Jellyfin adapters for user-owned libraries
- [ ] Official YouTube discovery/player integration within policy
- [ ] Cross-provider search with source-specific result grouping
- [ ] Artwork, metadata and playlist import where permitted

## P4 — Premium experience
- [ ] Now Playing redesign and animated lyrics
- [ ] Equalizer/audio effects where supported
- [ ] Android Auto / Android for Cars
- [ ] Personalized discovery and smart playlists
- [ ] Backup, restore, diagnostics and accessibility pass
- [ ] Release signing, privacy notice, security and distribution review
