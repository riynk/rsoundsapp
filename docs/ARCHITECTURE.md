# RSounds Architecture

## Goals

RSounds is planned as a modular Android application. Core playback and data capabilities should remain independent from individual screens and online providers.

## Proposed layers

### app
Application entry point, dependency wiring, navigation host, and lifecycle integration.

### core/player
Playback abstractions, queue management, audio focus, media session, and AndroidX Media3 integration.

### core/lyrics
Lyric models, parsing, time synchronization, offset adjustment, and source interfaces.

### core/network
Shared networking primitives, error handling, and provider-facing infrastructure. Provider-specific terms and authentication requirements must be respected.

### core/design
Typography, color tokens, shapes, reusable components, motion guidelines, and the circular RSounds identity.

### features
User-facing capabilities such as home, library, search, lyrics, and settings. Feature modules should depend on core abstractions rather than each other where practical.

### automotive/android-auto
Car-compatible media browsing and playback integration using supported Android for Cars APIs and templates. The implementation must comply with platform driver-distraction requirements.

## Provider integration

Online music and lyric services should be isolated behind interfaces. Each provider module must document:

- Its official API or SDK and permitted use.
- Authentication and token-storage requirements.
- Rate limits and attribution requirements.
- Content availability and regional restrictions.
- Applicable license and terms of service.

No provider integration should scrape private endpoints, bypass access controls, or redistribute protected content without authorization.

## Initial implementation sequence

1. Establish the Gradle Android project and application module.
2. Add a minimal Compose shell and design tokens.
3. Integrate Media3 for local playback.
4. Add media-library indexing and permissions.
5. Introduce lyric source abstractions and synchronized display.
6. Add authorized online-provider integrations.
7. Implement Android Auto support and validate it against platform requirements.
