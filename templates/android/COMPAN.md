# Compan Android Integration

This repo uses Compandroid for private Android development builds.

An LLM agent or developer pushes source changes to GitHub. GitHub Actions builds
an APK artifact. The installed Compandroid-enabled app reads GitHub Actions,
downloads newer APK artifacts, and starts Android's user-confirmed installer.

## Before Changing Builds Or Updates

Read `compan.json` first. Keep these values aligned:

- Android package name
- Gradle app module
- build variant
- workflow path
- artifact name
- signing lineage
- versionCode

## Agent Responsibilities

- Push app code updates.
- Keep `.github/workflows/compan-android-apk.yml` passing.
- Keep `versionCode` increasing for installable APK updates.
- Do not ask the installed app for GitHub write permissions.

## Human Setup Checklist

1. Add the Compandroid SDK with `debugImplementation`.
2. Call `Compandroid.install(this)` from the main activity in debug builds.
3. Confirm `compan.json` matches this app's package, module, branch, workflow,
   and artifact name.
4. Push to the configured branch.
5. Confirm GitHub Actions uploads the APK artifact.
6. Install the development APK on device.
7. Shake the device to open Compandroid settings.
8. Use a read-only GitHub token to pull newer APK artifacts.

## Private Repo Token Scope

The installed app only needs read access:

- Metadata: read
- Contents: read
- Actions: read

Repo writes belong to the developer, LLM agent, or external automation.
