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

