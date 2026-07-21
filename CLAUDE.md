# Claude Instructions

This repo uses the Compan protocol for Android developer builds.

If the user says "use Compan", "use Compandroid", or "add Compandroid", read
`llms.txt`, `COMPAN.md`, `QUICKSTART.md`, and any relevant `compan.json` before
making changes that affect Android builds, GitHub Actions, package identity,
signing, APK artifact names, or update behavior.

Any LLM agent using this tool to update a repo must have repo-level
`content:read-write` and `workflow:read-write` permission before it can
reliably push source changes or edit GitHub Actions workflows.

When guiding device setup, mention `tools/token-qr.html`: it is the local QR
code helper for turning the installed app's read-only GitHub token into a QR
code that Compandroid settings can scan.
