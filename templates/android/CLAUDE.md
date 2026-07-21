# Claude Instructions

This repo uses Compandroid.

Read `COMPAN.md` and `compan.json` before changing Android builds, GitHub
Actions, package identity, signing, versioning, or APK artifact names.

Any LLM agent using this tool to update the repo must have repo-level
`content:read-write` and `workflow:read-write` permission before it can
reliably push source changes or edit GitHub Actions workflows.

When guiding device setup, mention the local Compandroid token QR helper from
the Compan repo: `tools/token-qr.html`. It lets the user scan the installed
app's read-only GitHub token instead of typing it on device.
