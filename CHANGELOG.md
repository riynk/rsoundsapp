# Changelog

All notable changes to RSounds are recorded here.

## Unreleased — 2026-11-26

### Added
- MediaStore local library scanning and runtime audio-permission flow.
- Media3 playback service, MediaController, local queue submission and play/pause UI.
- Read-only audio metadata access through Android MediaStore content URIs.
- Deep lyrics domain with plain, line and word synchronization states.
- Playback-position-driven lyric timeline and word highlighting in the Now Playing panel.
- LRCLIB lookup adapter with metadata matching, User-Agent identification, 750 ms pacing, Retry-After cooldown, and word/line/plain fallback.
- Hosted test-report and debug APK artifact publishing.
- Initial Android Gradle project and Jetpack Compose application shell.
- Media3 ExoPlayer and MediaSessionService lifecycle boundary.
- Lyrics models and enhanced-LRC parsing with millisecond timestamps.
- Bounded safe LRCLIB Lyricsfile YAML parser adapted from PixelMusic with provenance.
- Tests for word timing, line fallback, offsets and malformed input.
- GitHub Actions workflow for Android tests and debug APK assembly.
- Technical audit, architecture, provenance and roadmap documents.

### Notes
- This establishes the build foundation. Local scanning, online providers and a complete player UI are not yet implemented.
