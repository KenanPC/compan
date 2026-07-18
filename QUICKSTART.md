# Compandroid Quickstart

Use this guide to add Compandroid to an Android app repo.

Compandroid is intended for private GitHub-backed development builds. An LLM
agent or developer pushes app updates to GitHub, GitHub Actions builds an APK,
and the installed development app pulls the latest APK artifact with explicit
user approval.

## 1. Add Compandroid To The App

No packaged SDK has been published yet. For now, vendor this repo or add it as a
source dependency, then include the SDK module only in development builds:

```kotlin
// settings.gradle.kts
include(":compandroid-sdk")
project(":compandroid-sdk").projectDir = file("vendor/compan/compandroid-sdk")
```

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation(project(":compandroid-sdk"))
}
```

After a packaged release exists, prefer a versioned dependency:

```kotlin
dependencies {
    debugImplementation("dev.compan:compandroid-sdk:0.1.0")
}
```

## 2. Install The Shake Settings Hook

In the app's main `Activity`, install Compandroid for debug builds:

```kotlin
import dev.compan.compandroid.Compandroid

override fun onResume() {
    super.onResume()
    if (BuildConfig.DEBUG) {
        Compandroid.install(this)
    }
}

override fun onPause() {
    Compandroid.uninstall()
    super.onPause()
}
```

When the app is running, shake the device to open Compandroid settings.

## 3. Copy Repo Protocol Files

Copy these files from this repo into the Android app repo:

```text
templates/android/compan.json                      -> compan.json
templates/android/COMPAN.md                        -> COMPAN.md
templates/android/AGENTS.md                        -> AGENTS.md
templates/android/CLAUDE.md                        -> CLAUDE.md
templates/android/.github/workflows/compan-android-apk.yml
                                                   -> .github/workflows/compan-android-apk.yml
templates/android/compan.json                      -> app/src/debug/assets/compan.json
```

Update `compan.json` so it matches the app:

```json
{
  "app": {
    "packageName": "com.example.app.dev",
    "module": ":app",
    "buildVariant": "debug"
  },
  "github": {
    "branch": "compan-android",
    "workflow": ".github/workflows/compan-android-apk.yml",
    "artifactName": "compan-android-debug-apk"
  }
}
```

## 4. Configure GitHub Actions

The default workflow builds:

```bash
./gradlew :app:assembleDebug
```

If the app uses another module or variant, update both:

- `.github/workflows/compan-android-apk.yml`
- `compan.json`

The workflow must upload an artifact whose name matches
`github.artifactName` in `compan.json`.

## 5. Bundle Runtime Defaults

Copy `compan.json` into the debug assets directory:

```text
app/src/debug/assets/compan.json
```

Compandroid reads this bundled asset at runtime to prefill owner, repo, branch,
workflow, artifact name, and package name. The user can still override these in
the settings panel.

## 6. Private Repo Access

For private repos, the installed app needs read-only GitHub access:

- Metadata: read
- Contents: read
- Actions: read

It does not need write access. Repo writes should be handled by the developer,
an LLM agent, or GitHub automation outside the installed app.

## 7. Pull An Update On Device

After GitHub Actions produces a successful APK artifact:

1. Open the development app.
2. Shake the device.
3. Enter the GitHub owner, repo, branch, artifact name, and read token.
4. Tap **Pull Latest APK**.
5. Android will show the system install/update confirmation UI.
6. Confirm the install.

Compandroid cannot silently install APKs.

## 8. Keep Updates Installable

Every APK update must keep:

- the same package name
- compatible signing certificate lineage
- a higher `versionCode`

If any of those change, Android may reject the update.
