# Compandroid

Compandroid is the Android member of the Compan developer companion family.

It is repo-integrated scaffolding for Android apps: developers add the
Compandroid SDK to their own private GitHub repo, GitHub Actions builds APKs
from that repo, and the installed development app can pull newer APK builds
from GitHub Actions with explicit user approval.

## Product Shape

- `Compandroid` is embedded into the developer's Android app.
- The user's app remains a normal Android APK.
- Shaking the device opens the hidden Compandroid settings panel.
- The settings panel can inspect the configured private GitHub repo, find the
  latest successful APK artifact, download it, and start Android's installer.
- Code changes still require a new APK build and user-confirmed install.

## Repo Files

- `COMPAN.md` is the canonical human and LLM-readable protocol.
- `compan.json` is the machine-readable app/build/update contract.
- `AGENTS.md` and `CLAUDE.md` are short pointers for coding agents.
- `.github/workflows/compan-android-apk.yml` builds and uploads APK artifacts.

See `templates/android` for files that should be copied into a user app repo.

