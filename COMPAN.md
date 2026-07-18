# Compan Protocol

This repo contains the Android implementation, **Compandroid**.

Compandroid is designed for private GitHub repos where an LLM agent or developer
pushes code changes, GitHub Actions builds APK artifacts, and the installed
development app pulls those artifacts with read-only GitHub access.

## Rules For Humans And Agents

Before changing Android build, update, signing, package identity, or GitHub
Actions behavior:

1. Read `compan.json`.
2. Preserve the configured package name unless the user explicitly approves a
   migration.
3. Keep APK updates installable by using the same signing lineage and increasing
   `versionCode`.
4. Keep the GitHub Actions artifact name aligned with `compan.json`.
5. Do not require write GitHub permissions from the installed app.
6. Treat the installed app as read-only against GitHub: it may read workflow
   runs and artifacts, but repo writes belong to the developer or LLM agent.

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

