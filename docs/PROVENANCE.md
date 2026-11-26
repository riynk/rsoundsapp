# RSounds Third-Party Provenance

Record every copied/adapted source file and distributed asset here.

## Adapted source: LyricsfileParser.kt
- Upstream project: PixelMusic
- Repository: https://github.com/ianshulyadav/PixelMusic
- Source path: `app/src/main/java/com/unshoo/pixelmusic/data/network/lyrics/LyricsfileParser.kt`
- Reviewed upstream commit: `ca0efb08707d1ecc532f32190629c5e70d4e558c` (2026-11-19)
- Upstream license declared by project: GNU GPL-3.0-or-later
- RSounds destination: `core/lyrics/src/main/java/com/rsounds/core/lyrics/LyricsfileParser.kt`
- Changes: package/API adaptation; timestamps use three decimal places for millisecond precision; documentation and tests adapted for RSounds.
- The source file carries this provenance notice. Upstream project and contributors retain their copyrights; consult upstream for full license and contributor history.

## Imported build infrastructure
- `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` are generic Gradle Wrapper files fetched from PixelMusic's repository.
- Wrapper scripts retain the Gradle Apache-2.0 header; wrapper JAR embeds Gradle license metadata.
- Wrapper distribution is pinned by URL and SHA-256 in `gradle/wrapper/gradle-wrapper.properties`.

## Evaluated but not imported
| Project | Decision | Reason |
|---|---|---|
| PixelMusic | Selective reuse only | Large app/dependency graph; InnerTube streaming path not adopted |
| Sona | Reference only | Offline-only; multiple explicit upstream credits |
| Trident | Hold | Multi-hop fork/vendor provenance requires review |

## Required process for future imports
Record upstream repository and immutable commit, source/destination path, copyright holder, SPDX license, license text/NOTICE location, changes, dependencies/assets, and verification date. Never remove upstream source headers or present imported code as originally authored by RSounds.
