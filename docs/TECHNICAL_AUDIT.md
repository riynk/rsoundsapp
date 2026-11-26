# RSounds Technical Audit

**Audit date:** 2026-11-26  
**Scope:** repository state, selected source projects, licensing/provenance and initial platform architecture.  
**Status:** preliminary code-structure and source-document review; initial CI build verified; not a complete security or device audit.

## RSounds baseline
At audit start, `RY-DEV` contained documentation and a GPL-3.0-only license, but no Android Gradle project, manifest, Kotlin source or automated build. The initial implementation created the Android project and two core modules.

## CI verification
- **Verified commit:** `8f47ffabf6eab6e112b55ec1d2616b0615abfc19`
- **Workflow:** Android CI
- **Run:** https://github.com/riynk/rsoundsapp/actions/runs/45831770725
- **Result:** GitHub Actions completed successfully.
- **Tasks:** `testDebugUnitTest` and `assembleDebug`.
- This verifies Gradle configuration, compilation, the current lyrics unit tests, and debug APK assembly on the hosted runner.
- It does not verify runtime playback on a physical device, permissions, audio focus, streaming-provider access, Android Auto, security posture, or production release signing.

## Candidate review

### PixelMusic — hybrid architecture reference
Repository: https://github.com/ianshulyadav/PixelMusic

Evidence reviewed: README, Gradle settings/version catalog/app build file, MediaSession playback service, LRCLIB response/model/repository, Lyricsfile parser/tests, `docs/PROVENANCE.md`, `docs/DEPENDENCY_LICENSES.md`, and `docs/THIRD_PARTY_NOTICES.md`.

Findings:
- Strong hybrid feature fit: local audio, cloud/server adapters, playback session, EQ, LRCLIB word/line lyrics, cache, translation and romanization.
- README describes YouTube Music streaming through public InnerTube streams. RSounds does not adopt this pathway because official authorization and compatibility with current YouTube terms were not established.
- Provenance states PixelPlayerOSS (GPL-3.0-or-later) and a foundational MIT PixelPlayer commit. This is self-reported upstream evidence and does not replace file-level review.
- Its dependency review reports 631 unique release runtime coordinates. Importing the whole application would transfer substantial maintenance and license-review scope.
- Decision: selective technical reference only; preserve notices for any imported component.

### Sona — local library and lyrics reference
Repository: https://github.com/LhacenMed/Sona

Findings:
- Strong offline-library foundation: MediaStore library, playlists, playback persistence, embedded LRC/TTML/QRC, word highlighting and lyric offset.
- README credits Auxio, ArchiveTune, YTDLnis, syncedlyrics, Fossify Music Player and cascade. Ported files carry original notices.
- It is explicitly an offline player rather than a hybrid streaming product.
- Decision: reference for local-library and lyrics patterns; no Sona source imported in this implementation.

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
- LRCLIB integration must implement pacing, 429/Retry-After handling, caching and source attribution.
- Navidrome/Jellyfin are candidates for user-owned libraries through supported protocols, subject to server/content permissions.
- Music recordings, lyrics, translations and artwork have rights independent of application source code.

## Initial risk register

| Risk | Severity | Mitigation / gate |
|---|---|---|
| Unofficial streaming endpoints change or are not permitted | High | Do not ship InnerTube extraction; use approved adapters |
| Upstream provenance is incomplete | High | File history, NOTICE and source mapping before reuse |
| Lyrics match wrong recording/version | High | Multi-signal matching and user correction |
| Word timings are absent/inaccurate | Medium | Honest sync-level indicator and line fallback |
| Provider rate limits or downtime | Medium | Cache, backoff, 429 handling and isolation |
| Large upstream dependency graph | Medium | Selective extraction and dependency allowlist |
| Android background restrictions | Medium | MediaSessionService and platform tests |

## Audit limitations
No full source checkout, dependency-license scan, APK inspection, SAST, secret scan, instrumentation test or physical-device playback test was performed. The successful hosted build does not substitute for those checks.

## Next audit gates
1. Inspect source histories for every proposed import.
2. Create a dependency SBOM and automated license report.
3. Add local MediaStore scanner and playback-controller tests.
4. Review provider API contracts and terms before implementation.
5. Run permissions, audio focus, notification and physical-device playback tests.
