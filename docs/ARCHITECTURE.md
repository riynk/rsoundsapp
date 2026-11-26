# RSounds Architecture

**Product model:** hybrid local + authorized online music, with a deep synchronized-lyrics experience.

## Engineering principles
- Reuse, repair, refactor, integrate. Evaluate upstream code as a component, not a wholesale application import.
- Keep playback, lyrics, metadata, storage and provider adapters behind stable contracts.
- A provider failure must not break local playback or the lyrics engine.
- Preserve upstream copyright, license texts and provenance for every reused component.
- Prefer official APIs, SDKs or explicitly permitted protocols.

## Current module graph
```text
:app
 ├── :core:player
 ├── :core:lyrics
 └── :provider:local

Future modules:
 :core:model
 :core:database
 :core:network
 :core:metadata
 :core:designsystem
 :provider:lrclib
 :provider:navidrome
 :provider:jellyfin
 :provider:youtube
 :feature:home
 :feature:library
 :feature:search
 :feature:player
 :feature:lyrics
 :feature:settings
 :feature:automotive
```

The initial implementation is a modular monolith. Split modules further only when build time, ownership or test boundaries justify the cost.

## Runtime boundaries

### Local library
- `:provider:local` reads audio metadata from Android MediaStore through a read-only ContentResolver query.
- Android 13+ requests `READ_MEDIA_AUDIO`; Android 8–12 requests `READ_EXTERNAL_STORAGE` with a maximum SDK of 32.
- Audio content is opened through platform content URIs. RSounds does not copy, upload or modify the source files.
- A Room index and change observer are planned; the current scanner performs an on-demand library query.

### Playback
- AndroidX Media3 ExoPlayer is the initial playback engine.
- `RsoundsPlaybackService` owns ExoPlayer and MediaSession lifecycle.
- The app connects through a MediaController and submits MediaStore content URIs as MediaItems.
- Media3 handles audio focus; the player pauses on noisy audio-output changes.
- The system media session provides integration points for lock-screen, headset and external controls.
- Advanced queue persistence, shuffle/repeat, audio effects and device tests remain separate integration tasks.

### Lyrics
- Domain timestamps are integer milliseconds.
- Enhanced-LRC parsing supports line and word timestamps, metadata, offsets and repeated line timestamps.
- LRCLIB Lyricsfile YAML uses SnakeYAML's safe constructor, schema-version checks and input/collection limits.
- Word sync is preferred; line sync is fallback; plain text is last fallback when no timing is supplied.
- Never fabricate word timing from line timing.

### Providers
Each provider must document permitted API/protocol use, authentication, attribution, rate limits, regions, cache rules, content rights and failure behavior.

Initial candidates:
- Android MediaStore / Storage Access Framework.
- LRCLIB for lyrics, subject to API usage policy and polite rate-limit handling.
- User-owned Navidrome or Jellyfin servers through supported protocols.
- YouTube official APIs/player flows for discovery and user-initiated playback within current policies.

### YouTube boundary
Do not implement audio-only extraction, background playback, offline copies, ad blocking or other behavior that bypasses YouTube restrictions. Identify YouTube content and keep search results separated where platform policies require it.

## Module contracts
- `core:player`: playback service and Media3-facing interfaces.
- `core:lyrics`: parsing, normalized models, timeline selection and offset transformations.
- `provider:local`: read-only MediaStore scan and platform content URI mapping.
- `provider:*`: translate provider data into RSounds domain models; provider code does not leak into UI.
- `feature:*`: UI and user interaction; no direct HTTP or database calls.
- `app`: composition root, permissions, controller connection and navigation.

## Planned data model
- `TrackId`: internal stable identifier, independent of provider IDs.
- `Track`: title, artists, album, duration, recording IDs and version tags.
- `TrackSource`: source, URI, availability and permitted actions.
- `LyricsDocument`: language, source, sync level, metadata, offset and revision.
- `LyricLine` / `LyricWord`: exact text and start/end milliseconds.
- `Playlist`, `QueueItem`, `PlaybackHistory`, `ArtworkRef`.

Track matching must consider title, primary artist, duration tolerance, album/version and recording ID when available. Title-only matching is insufficient for remixes, live versions or same-name songs.

## Android platform
- Compile against Android 17 API 37.1 preview to satisfy Compose 1.13.0 library metadata.
- Keep targetSdk at Android 16 API 36 until Android 17 reaches stable release.
- Background playback uses a foreground media-playback service and MediaSession.
- Request media and notification permissions only when the corresponding feature is first used.
- Android Auto / Android for Cars is a later feature and must follow current platform requirements.

## Testing strategy
- Unit tests: timestamps, word/line fallback, offsets and invalid YAML.
- Provider contract tests with fixtures; not only live services.
- Playback tests: queue transitions, audio focus, service recreation and noisy-output events.
- Instrumentation tests: permissions, MediaStore scanning, notifications and car integration.
- CI must run unit tests and build a debug APK before merge.
