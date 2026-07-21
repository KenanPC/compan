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

Any LLM agent expected to use this setup for repo changes needs repo-level
`content:read-write` and `workflow:read-write` permission. These scopes allow
the agent to push source updates, bump `versionCode`, and create or edit the
GitHub Actions workflow that produces APK artifacts.

## 7. Pull An Update On Device

After GitHub Actions produces a successful APK artifact:

1. Open the development app.
2. Shake the device.
3. Scan a QR code containing a read-only GitHub token, or paste the token as a
   fallback.
4. Choose or confirm the GitHub owner, repo, branch, workflow, and artifact
   name.
5. Tap **Pull Latest APK**.
6. Android will show the system install/update confirmation UI.
7. Confirm the install.

Compandroid cannot silently install APKs.

The recommended token is a fine-grained personal access token scoped to the app
repo with Metadata, Contents, and Actions read access. Do not bundle this token
into the APK.

To scan the token without using a hosted QR service, open
`tools/token-qr.html` from this repo in a browser, paste the read-only token,
generate the QR code, scan it with Compandroid, then click **Erase Session**.
The generator is a local static page with no remote scripts and clears the
token automatically after a short session.

## 8. Keep Updates Installable

Every APK update must keep:

- the same package name
- compatible signing certificate lineage
- a higher `versionCode`

If any of those change, Android may reject the update.

If a developer manually installs a local APK with the same or newer
`versionCode` than the latest GitHub Actions artifact, Compandroid will reject
the GitHub APK as not newer. The first pull may still download the artifact so
Compandroid can inspect its `versionCode`; repeated pulls skip that same
non-newer artifact until GitHub publishes a different artifact or the installed
app version changes.
