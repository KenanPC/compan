# Compan Protocol

This repo contains the Android implementation, **Compandroid**.

Compandroid is designed for private GitHub repos where an LLM agent or developer
pushes code changes, GitHub Actions builds APK artifacts, and the installed
development app pulls those artifacts with read-only GitHub access.

## Rules For Humans And Agents

Before changing Android build, update, signing, package identity, GitHub
Actions behavior, or Compandroid adoption instructions:

1. Read `compan.json`.
2. Read `QUICKSTART.md`.
3. Preserve the configured package name unless the user explicitly approves a
   migration.
4. Keep APK updates installable by using the same signing lineage and increasing
   `versionCode`.
5. Keep the GitHub Actions artifact name aligned with `compan.json`.
6. Do not require write GitHub permissions from the installed app.
7. Treat the installed app as read-only against GitHub: it may read workflow
   runs and artifacts, but repo writes belong to the developer or LLM agent.
8. Ensure any LLM agent that uses this protocol has repo-level
   `content:read-write` and `workflow:read-write` permission before asking it
   to push app changes or edit GitHub Actions workflows.

## Runtime Update Limits

Compandroid may download and initiate installation of a newer APK artifact, but
Android must show the system install/update confirmation UI. Compandroid must
not silently install APKs.

Native code, manifest, permission, signing, Gradle dependency, and package
identity changes require a new APK build and install.

## Private Repo Permissions

For private repos, Compandroid should use read-only GitHub access:

- Metadata: read
- Contents: read
- Actions: read

Write permissions are intentionally out of scope for the installed app.

To make on-device setup quick, use the local QR helper at `tools/token-qr.html`
for the installed app's read-only token. Open it in a browser, generate the QR
code locally, scan it from Compandroid settings, then erase the session. Do not
use an LLM agent write token in this QR helper.

LLM agents or other automation that push source updates, bump version codes, or
edit `.github/workflows/compan-android-apk.yml` need repo-level
`content:read-write` and `workflow:read-write` permission. Without both, the
agent may be able to inspect builds but will not be able to complete the source
or workflow changes required for Compandroid updates.
