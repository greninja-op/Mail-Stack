# Phase 08 — Secure Storage

**Status:** COMPLETE — 2026-10-20

COMPLETE (contract). Tokens stay in CredentialStore (platform.rs MemoryCredentialStore + SecureSecret); persistence schema has no token columns. OAuth pending state is in-memory PendingOauthStore (consume-once) — OS credential adapter remains Windows-side.

Shared crate: `desktop-core`. Windows UI adapters consume these contracts; do not reimplement precedence/sync honesty in presentation code.
