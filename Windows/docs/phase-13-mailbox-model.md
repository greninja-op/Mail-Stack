# Phase 13 — Mailbox Model

**Status:** COMPLETE — 2026-10-20

COMPLETE (desktop-core). mailbox.rs: MessagePresentation / ThreadPresentation; row carries raw sender email + display name for logo pipeline; receiving_account_email for multi-account.

Shared crate: `desktop-core`. Windows UI adapters consume these contracts; do not reimplement precedence/sync honesty in presentation code.
