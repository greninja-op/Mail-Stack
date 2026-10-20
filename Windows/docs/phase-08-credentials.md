# Phase 08 — Secure Storage (Credential Manager / DPAPI)

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `desktop-core` trait + `Windows/src/adapters/credential_windows.rs`.

## Delivered

| Item | Location |
|------|----------|
| `CredentialStore` + `SecureSecret` (zero Debug leak) | `desktop-core/src/platform.rs` |
| `MemoryCredentialStore` fallback | same (exported from `lib.rs`) |
| Windows Credential Manager adapter (`CredWriteW` / `CredReadW` / `CredDeleteW`) | `Windows/src/adapters/credential_windows.rs` |
| Logout clears known keys | `clear_account_secrets` |
| Tokens never in DB | Phase 07 schema + this adapter |

## Verification

- desktop-core: `platform::tests::memory_store_isolates_accounts`, `secure_secret_debug_redacts`.
- Windows adapter unit test: memory-only roundtrip + clear (WinCred path needs interactive host).

## Blockers

- Live CredWrite/CredRead on this agent host not asserted end-to-end (memory fallback used in CI/tests).
- Production must not rely on memory fallback — log warns when CredWrite fails.
