# Version scheme

## SemVer (Cargo)

| Field | Source |
|-------|--------|
| Cargo package version | `Windows/Cargo.toml` → `version` (workspace default `0.1.0`) |
| Runtime display | `env!("CARGO_PKG_VERSION")` via `identity::app_identity()` |
| Monotonic `version_code` | `identity.rs` integer for installer comparisons |

## MSIX Identity Version

Format: **`MAJOR.MINOR.PATCH.0`** (fourth field reserved / revision).

| Cargo | MSIX `Identity Version` |
|-------|-------------------------|
| `0.1.0` | `0.1.0.0` |
| `1.2.3` | `1.2.3.0` |

Update **both** `Cargo.toml` and `packaging/msix/AppxManifest.xml` (or generated manifest) in the same release PR.

## Rules

1. **Never reuse** a version string that was shipped or submitted to the Store.
2. Pre-release labels (`0.1.0-rc.1`) are for Cargo/git tags only — MSIX version must remain numeric `A.B.C.D`.
3. Hotfix that does not change package identity still needs a new MSIX version.
4. Record every published version in release notes with git tag `windows-vMAJOR.MINOR.PATCH`.
