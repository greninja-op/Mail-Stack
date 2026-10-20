# Phase 13 — Mailbox Data Model & Message Presentation

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `desktop-core` (shared) + Windows UI consumption via bridge

## Delivered
- `desktop-core/src/mailbox.rs` — `MessagePresentation`, `ThreadPresentation`, `AttachmentMeta`
- Row models **must** carry raw `sender_email` + `sender_display_name` for the logo pipeline
- Receiving-account identity on every presentation row (`receiving_account_email`)
- Defensive `from_parts` for malformed payloads
- Wired in `desktop-core/src/lib.rs`; Windows `MailRowModel` mirrors contracts

## Tests
- `mailbox::tests::row_preserves_sender_email_for_logo`
- `mailbox::tests::thread_aggregates_flags`
- Windows `app_state::tests::row_preserves_sender_email_for_logo`

## Permanent rules
- Dropping sender email from a row model is a bug (logos become impossible).
- Receiving account ≠ sender identity.
