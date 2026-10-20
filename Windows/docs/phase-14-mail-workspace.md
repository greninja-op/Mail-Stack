# Phase 14 — Windows Mail Workspace & Conversation Viewer

**Status:** COMPLETE (2026-10-20)

## Delivered
- `Windows/src/ui/mail_list.rs` — flat rows, 40px avatar, min 88px, no card chrome
- Logo pipeline input via sender domain → letter fallback
- `Windows/src/ui/conversation.rs` — sanitized HTML, remote images never fetched
- Receiving-account indicator on every unified row

## Permanent rules
- Flat mail rows only (no border/shadow/card in the list).
- Sanitize before display; blocked remote images render as nothing (no per-image placeholders).
