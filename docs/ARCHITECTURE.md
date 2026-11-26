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
 └── :core:lyrics

Future modules:
 :core:model
 :core:database
 :core:network
 :core:metadata
 :core:designsystem
 :provider:local
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

### Playback
- AndroidX Media3 ExoPlayer is the initial playback engine.
- `RsoundsPlaybackService` owns ExoPlayer and MediaSession lifecycle.
- Queue policy, source resolution, persistence, effects and UI controllers are separate integration tasks.
- Local files and permitted music-server sources share one internal playback model.

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
- `provider:*`: translate provider data into RSounds domain models; provider code does not leak into UI.
- `feature:*`: UI and user interaction; no direct HTTP or database calls.
- `app`: composition root and navigation.

## Planned data model
- `TrackId`: internal stable identifier, independent of provider IDs.
- `Track`: title, artists, album, duration, recording IDs and version tags.
- `TrackSource`: source, URI, availability and permitted actions.
- `LyricsDocument`: language, source, sync level, metadata, offset and revision.
- `LyricLine` / `LyricWord`: exact text and start/end milliseconds.
- `Playlist`, `QueueItem`, `PlaybackHistory`, `ArtworkRef`.

Track matching must consider title, primary artist, duration tolerance, album/version and recording ID when available. Title-only matching is insufficient for remixes, live versions or same-name songs.

## Android platform
- Background playback uses a foreground media-playback service and MediaSession.
- Request media and notification permissions only when the corresponding feature is first used.
- Android Auto / Android for Cars is a later feature and must follow current platform requirements.

## Testing strategy
- Unit tests: timestamps, word/line fallback, offsets and invalid YAML.
- Provider contract tests with fixtures; not only live services.
- Playback tests: queue transitions, audio focus, service recreation and noisy-output events.
- Instrumentation tests: permissions, MediaStore scanning, notifications and car integration.
- CI must run unit tests and build a debug APK before merge.
