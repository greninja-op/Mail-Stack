# Phase 02 — Shared Rust Contract & Windows Platform Boundary

**Status:** COMPLETE (2026-10-20)  
**Boundary:** Shared contracts live in `desktop-core/`; Windows adapters under `Windows/src/adapters/`.

## Shared contract (`desktop-core`)

Platform-neutral modules consumed by Windows (and intended for macOS reuse), including:

| Area | Responsibility |
|------|----------------|
| `account` | Explicit `AccountId` isolation |
| `cancel` | Cooperative `CancelToken` |
| `category` | Category-group model (semantic kinds) |
| `error` | `CoreError` / `CoreResult` |
| `html` | Sanitized HTML; remote images never fetched |
| `logo` | Logo-pipeline inputs (sender → domain) |
| `pacing` | Throttle / cursor policy |
| `platform` | Traits: `BrowserOpen`, `CredentialStore`, `NotificationService` |
| `sync` | Fixed denominator, 99% cap while running, Done = 100% |

Later-phase modules (persistence, OAuth, Gmail, etc.) may also exist in `desktop-core` from sibling agents; Phase 02 established the boundary pattern.

## Windows boundary

| Area | Role |
|------|------|
| `Windows/src/bridge.rs` | UI façade over desktop-core |
| `Windows/src/adapters/` | Native Windows API behind traits |
| `Windows/src/ui/` | Presentation only |
| `Windows/src/redacting_log.rs` | Log sanitizer (tokens, Authorization, email local-parts) |

## Verification

- Unit tests in `desktop-core` and `Windows` bridge / redacting_log.
- Windows binary links `desktop-core` and boots the egui shell.
