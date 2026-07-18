# Android Adoption

This is the detailed integration path for adding Compandroid to a private
Android app repo. For the short version, see `QUICKSTART.md`.

## 1. Add The SDK

No packaged SDK has been published yet. Include the Compandroid SDK as a source
module for now:

```kotlin
// settings.gradle.kts
include(":compandroid-sdk")
project(":compandroid-sdk").projectDir = file("vendor/compan/compandroid-sdk")
```

```kotlin
// app/build.gradle.kts
debugImplementation(project(":compandroid-sdk"))
```

Long term, prefer a versioned dependency after one is published:

```kotlin
debugImplementation("dev.compan:compandroid-sdk:0.1.0")
```

Use `debugImplementation` or a dedicated development build type. Do not include
Compandroid in production builds unless the app's distribution and policy model
explicitly supports developer APK updates.

## 2. Install From The Main Activity

Call Compandroid only in development builds:

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

Shaking the device opens the Compandroid settings panel.

## 3. Add Repo Protocol Files

Copy these template files into the target app repo:

```text
templates/android/compan.json
templates/android/COMPAN.md
templates/android/AGENTS.md
templates/android/CLAUDE.md
templates/android/.github/workflows/compan-android-apk.yml
```

Update `compan.json` to match the app module, package name, branch, and artifact
name.

Recommended target repo layout:

```text
compan.json
COMPAN.md
AGENTS.md
CLAUDE.md
.github/workflows/compan-android-apk.yml
```

`COMPAN.md` is the canonical human and LLM-readable protocol. `compan.json` is
the machine-readable source of truth.

Also copy `compan.json` into the app's debug assets:

```text
app/src/debug/assets/compan.json
```

The SDK reads this asset at runtime to prefill settings. This avoids making the
developer type the repo, branch, workflow, artifact name, and package manually
on every device.

## 4. Build Automation

The default workflow builds:

```bash
./gradlew :app:assembleDebug
```

If the app uses another module or variant, update both the workflow and
`compan.json`.

## 5. GitHub Permissions

For private repos, the installed app should use read-only GitHub access:

```text
Metadata: read
Contents: read
Actions: read
```

This lets Compandroid read repo metadata, list workflow runs, and download
GitHub Actions APK artifacts. It should not require repo write permissions.

The LLM agent or developer is responsible for write actions:

- pushing source changes
- fixing build failures
- updating the workflow
- bumping version codes
- changing dependencies

## 6. APK Update Requirements

Android will only accept the downloaded APK as an update when it is compatible
with the installed app:

- same package name
- same signing certificate lineage
- higher `versionCode`
- compatible Android SDK constraints

Compandroid validates package name and `versionCode` before invoking the system
installer. Android performs the final signing and install checks.

## 7. Verify The Integration

In the target app repo:

1. Confirm `compan.json` matches the Gradle module and package name.
2. Push to the configured branch, usually `compan-android`.
3. Confirm the GitHub Actions workflow succeeds.
4. Confirm an APK artifact is uploaded with the configured artifact name.
5. Install the APK on device.
6. Shake the device to open Compandroid settings.
7. Enter the read-only GitHub token and repo settings.
8. Tap **Pull Latest APK** after a newer successful build exists.

## 8. Troubleshooting

`No matching successful APK artifact found` usually means the workflow failed,
the branch is wrong, or the artifact name in GitHub does not match
`compan.json`.

`APK package ... does not match ...` means the downloaded artifact is not an
update for the currently installed app.

`APK versionCode ... is not newer ...` means the build did not increment
`versionCode`.

If Android rejects the install after Compandroid starts the installer, check
signing certificate continuity and whether the existing app was installed from a
different signing key.
