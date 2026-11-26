# RSounds Technical Audit

**Audit date:** 2026-11-26  
**Scope:** repository baseline, selected source projects, licensing/provenance, architecture and initial CI implementation.  
**Status:** preliminary source-structure review plus passing hosted build/unit tests; not a complete security or device audit.

## Verified implementation
- Android Gradle project and Jetpack Compose app shell.
- Media3 ExoPlayer + MediaSessionService lifecycle boundary.
- MediaStore local audio scanner with version-aware permission declaration and request flow.
- MediaController connection, track list, queue submission and play/pause surface.
- Enhanced-LRC and Lyricsfile parsing, including word-level timestamp preservation.
- Plain-text lyrics parsing and playback-position-driven lyrics timeline.
- LRCLIB signature lookup adapter with client identification, serialized requests, pacing and Retry-After cooldown.
- Now Playing lyrics panel that resolves the active line/word from Media3 playback position.
- Unit tests for parsers, timeline boundaries, LRCLIB fallbacks and rate limiting.
- GitHub Actions unit tests, debug APK assembly and artifact publishing.

### CI evidence
- Foundation build: https://github.com/riynk/rsoundsapp/actions/runs/45832235322 — success.
- Local playback build: https://github.com/riynk/rsoundsapp/actions/runs/45833231266 — success.
- Lyrics timeline build: https://github.com/riynk/rsoundsapp/actions/runs/45834020814 — success.
- Current CI for the LRCLIB provider/UI integration must be checked before describing that commit as verified.
- Hosted CI verifies compilation and unit tests, not physical-device playback or external network behavior.

## Candidate review

### PixelMusic — hybrid architecture reference
Repository: https://github.com/ianshulyadav/PixelMusic

Evidence reviewed: README, Gradle settings/version catalog/app build file, MediaSession playback service, LRCLIB response/model/repository, Lyricsfile parser/tests, `docs/PROVENANCE.md`, `docs/DEPENDENCY_LICENSES.md`, and `docs/THIRD_PARTY_NOTICES.md`.

Findings:
- Strong hybrid feature fit: local audio, cloud/server adapters, playback session, EQ, LRCLIB word/line lyrics, cache, translation and romanization.
- README describes YouTube Music streaming through public InnerTube streams. RSounds does not adopt this pathway because official authorization and compatibility with current YouTube terms were not established.
- Provenance states PixelPlayerOSS (GPL-3.0-or-later) and a foundational MIT PixelPlayer commit. This is self-reported upstream evidence and does not replace file-level review.
- Its dependency review reports 631 unique release runtime coordinates. Importing the whole application would transfer substantial maintenance and license-review scope.
- Decision: selective technical reference only. The Lyricsfile parser is the only file currently adapted from this project and is recorded in `docs/PROVENANCE.md`.

### Sona — local library and lyrics reference
Repository: https://github.com/LhacenMed/Sona

Findings:
- Strong offline-library foundation: MediaStore library, playlists, playback persistence, embedded LRC/TTML/QRC, word highlighting and lyric offset.
- README credits Auxio, ArchiveTune, YTDLnis, syncedlyrics, Fossify Music Player and cascade. Ported files carry original notices.
- It is explicitly an offline player rather than a hybrid streaming product.
- Decision: reference for local-library and lyrics patterns; no Sona source imported.

### Trident — provenance hold
Repository: https://github.com/primearijit/Trident

Findings:
- Relevant features include synchronized lyrics, playback, network storage, discovery and Android Auto.
- NOTICE identifies vendored/adapted code from compose-hig (Apache-2.0), MeloX-Android (GPL-3.0), and Square (GPL-3.0).
- README identifies BeatWave/BitChord lineage and Convx architectural influence. The multi-hop history needs additional verification.
- Decision: do not import source until the provenance chain is mapped.

## Licensing
RSounds declares GPL-3.0-only. This does not replace upstream licenses or remove copyright notices.

Before importing code:
1. Verify exact upstream commit and file history.
2. Record project, copyright holders, source/destination paths, SPDX expression and modifications.
3. Preserve source headers and applicable license/NOTICE texts.
4. Review runtime, native, font, icon and generated assets separately.
5. Check combined distribution obligations.
6. Maintain an SBOM and regenerate it when dependencies change.

The adapted Lyricsfile parser is recorded in `docs/PROVENANCE.md`.

## Provider and content constraints
- YouTube integrations must follow current YouTube API/player policies; do not use the RSounds engine to extract audio, block ads or bypass playback restrictions.
- LRCLIB requires a descriptive User-Agent, sequential requests, short request spacing and honoring Retry-After; adapter behavior is isolated in `:provider:lrclib`.
- Navidrome/Jellyfin are candidates for user-owned libraries through supported protocols, subject to server/content permissions.
- Music recordings, lyrics, translations and artwork have rights independent of application source code.

## Initial risk register
| Risk | Severity | Mitigation / gate |
|---|---|---|
| Unofficial streaming endpoints change or are not permitted | High | Do not ship InnerTube extraction; use approved adapters |
| Upstream provenance is incomplete | High | File history, NOTICE and source mapping before reuse |
| Lyrics match wrong recording/version | High | Multi-signal metadata match and user verification |
| Word timings are absent/inaccurate | Medium | Honest sync-level indicator and line fallback |
| Provider rate limits or downtime | Medium | Pacing, Retry-After, cache plan and provider isolation |
| Large upstream dependency graph | Medium | Selective extraction and dependency allowlist |
| Android background restrictions | Medium | MediaSessionService and platform tests |

## Audit limitations
No full source checkout, dependency-license scanner/SBOM, APK inspection, SAST, secret scan, Android instrumentation test or physical-device playback test was performed. Current CI is hosted JVM/Android compile and unit-test verification only.

## Next audit gates
1. Inspect source histories for every proposed import.
2. Create a dependency SBOM and automated license report.
3. Add provider contract tests and local persistence.
4. Run permissions, audio focus, notification and physical-device playback tests.
5. Review provider API contracts and terms before implementation.
