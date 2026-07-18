# React Native And Expo Adoption

Compandroid is Android-native. React Native and Expo apps can still use it, but
the Android project details usually live under `android/`.

## Expo Prebuild

For Expo apps, run or maintain an Android native project with prebuild:

```bash
npx expo prebuild --platform android
```

After prebuild, apply Compandroid changes inside `android/`.

## Where Files Usually Go

```text
compan.json                                -> compan.json
templates/android/COMPAN.md                -> COMPAN.md
templates/android/AGENTS.md                -> AGENTS.md
templates/android/CLAUDE.md                -> CLAUDE.md
templates/android/.github/workflows/...    -> .github/workflows/compan-android-apk.yml
compan.json                                -> android/app/src/debug/assets/compan.json
```

Keep the root `compan.json` and bundled Android asset in sync. The asset lets
the installed Compandroid settings panel prefill repo, branch, workflow, artifact
name, and package name.

## Package Identity

Expo package identity may come from `app.json`, `app.config.js`, or generated
Android Gradle files. Make sure these all agree with `compan.json`:

- Expo Android package
- generated Android `applicationId`
- Compandroid `app.packageName`
- installed APK package name

## Build Workflow

The default Compandroid workflow assumes:

```bash
./gradlew :app:assembleDebug
```

For Expo/RN repos, the workflow may need to run from `android/` or generate the
native project first. Keep the workflow artifact name aligned with `compan.json`.

