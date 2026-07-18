# Android Adoption

This is the minimal integration path for a private Android app repo.

## 1. Add The SDK

Include the Compandroid SDK as a module or versioned dependency. During local
development of this repo, use a module include:

```kotlin
implementation(project(":compandroid-sdk"))
```

Long term, prefer a versioned dependency:

```kotlin
debugImplementation("dev.compan:compandroid-sdk:0.1.0")
```

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

## 4. Build Automation

The default workflow builds:

```bash
./gradlew :app:assembleDebug
```

If the app uses another module or variant, update both the workflow and
`compan.json`.

