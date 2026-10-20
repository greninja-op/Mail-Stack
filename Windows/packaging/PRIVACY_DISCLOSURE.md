# Privacy & data disclosure (Windows release stub)

**Status:** Draft for release packaging — not a legal final. Human review required before Store / public distribution.

## What Mailstack stores locally

| Data | Where | Remote Gmail modified on purge? |
|------|-------|-----------------------------------|
| Message metadata / bodies cache | Local DB (planned SQLite; in-memory contracts today) | **No** |
| Sync cursors | Local DB | **No** |
| Classifications / rules / pins | Local DB | **No** |
| OAuth tokens | **OS Credential Manager** (not DB) | **No** |
| Window layout prefs | Local app state | **No** |

## Network

- Gmail / Google APIs when the user connects an account (OAuth).
- Optional AI provider **only** if user enables AI (disabled by default); payloads minimized.
- Remote images in mail bodies are **not** fetched by the sanitizer policy.

## User controls

- Disconnect account → delete local cache + credential targets under `Mailstack/`.
- Privacy Center UI surfaces inventory (`ui/privacy_center.rs` + `desktop_core::privacy::data_inventory`).

## Google verification note

Public apps using restricted Gmail scopes typically require Google verification and often a CASA-style assessment. **Do not claim verified/approved** without Partner evidence.
