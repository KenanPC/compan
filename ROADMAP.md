# Compan Roadmap

This repo is in early preview. The current implementation is useful for shaping
the Compandroid protocol, but it is not yet a polished packaged SDK.

## Before First Preview Release

- Add a sample Android app that consumes Compandroid.
- Add a Gradle wrapper or verified build environment.
- Verify shake settings on an emulator or device.
- Verify GitHub Actions artifact lookup against a private test repo.
- Verify APK download and Android installer handoff.
- Add a release tag such as `v0.1.0-preview`.

## Known Gaps

- Package distribution: publish `dev.compan:compandroid-sdk` through Maven
  Central or GitHub Packages.
- Runtime config: continue expanding `assets/compan.json` support and generated
  build metadata.
- GitHub Actions lookup: add fuller pagination, workflow-run detail views,
  retry/backoff, token validation, and rate-limit UI.
- Token storage: move from plain shared preferences to encrypted storage.
- APK install flow: add richer result/status feedback after users return from
  Android's installer.
- APK validation: add signing certificate diagnostics before invoking the
  installer where Android APIs allow it.
- Debug-only safety: add Gradle checks and docs to reduce accidental production
  inclusion.
- UX: replace the minimal settings activity with a clearer debug-console UI.
- Activation: add configurable shake threshold and a fallback activation route.
- Expo/React Native: maintain dedicated adoption docs for apps whose Android
  project is generated under `android/`.

