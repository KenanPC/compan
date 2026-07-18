# Compan

Compan is a developer companion framework for mobile apps.

Its first platform implementation is **Compandroid**, repo-integrated
scaffolding for Android apps. Developers add the Compandroid SDK to their own
private GitHub repo, GitHub Actions builds APKs from that repo, and the
installed development app can pull newer APK builds from GitHub Actions with
explicit user approval.

Start with [QUICKSTART.md](QUICKSTART.md) to add Compandroid to an Android app
repo.

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

## Status

Compandroid is in early preview. See [ROADMAP.md](ROADMAP.md) for what remains
before a packaged SDK release.

## For Coding Agents

This repo is designed to be legible to mainstream coding agents. Agents should
read `llms.txt`, `COMPAN.md`, `QUICKSTART.md`, and `compan.json` in target app
repos before changing Android build, signing, package identity, GitHub Actions,
or APK update behavior.
