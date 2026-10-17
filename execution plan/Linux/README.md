# Mail Organizer — Linux Execution Plan

## Purpose
This directory is the Linux-specific execution plan for Mail Organizer. Linux is a first-class desktop target and must have a native-feeling desktop experience while sharing the reusable desktop Rust core with macOS and Windows.

## Architecture
- Rust is the primary implementation language for the reusable desktop core and Linux application.
- The shared desktop Rust core owns domain, application, Gmail, sync, classification, company intelligence, search/indexing, priority, temporal/conversation intelligence, rules, actions, automation, AI routing, persistence, and integration abstractions.
- Linux-specific UI, windowing, desktop integration, packaging, permissions, notifications, keyring/secret-service integration, and filesystem/platform adapters remain Linux-specific.
- Do not duplicate business logic in another language or framework.
- Linux must not modify macOS, Windows, Android, AndroidTablet, iOS, or iPadOS implementation folders.
- Shared desktop Rust-core changes are allowed only where a feature is genuinely shared with macOS/Windows and must preserve their contracts.

## UI
Linux should use the same Mail Organizer product design language and information architecture as the other desktop targets, while adapting to Linux desktop conventions, window managers, scaling, keyboard/mouse, accessibility, and distribution differences. Do not merely ship a scaled mobile UI.

## Phase sequence
00 Architecture & shared Rust-core audit
01 Linux target & toolchain foundation
02 Shared Rust contract & Linux platform boundary
03 Application shell, lifecycle & state restoration
04 Desktop navigation & multi-column workspace
05 Visual system, Material-inspired product language, tokens & theming
06 Rust core integration & dependency boundary
07 Local persistence & state hydration
08 Secure storage, keyring & account state
09 Google OAuth & account connection
10 Gmail data access & API adapter
11 Initial Gmail sync
12 Incremental sync, pagination & recovery
13 Mailbox data model & message presentation
14 Linux mail workspace & conversation viewer
15 Categories, company intelligence & company workspace
16 Search & local index
17 Priority, Action Required & explainability
18 Rules & user corrections
19 Temporal intelligence & conversation intelligence
20 Action cards, Calendar & Tasks
21 Multi-account & unified inbox
22 Offline, background work & notifications
23 Privacy, security & data protection
24 Optional AI fallback
25 Advanced automation
26 Desktop interaction, keyboard, menus & multi-window
27 Performance, accessibility & reliability hardening
28 Comprehensive functional & integration QA
29 Security, adversarial, migration & recovery QA
30 Production packaging, signing/distribution & final release hardening

**STOP AFTER PHASE 30.**

## 2026-10-18 — Plan updated to the current Mailstack UI

This plan was updated to match the CURRENT Mailstack UI as shipped on Android (1.0.39 + in-main fixes), adapted to the Linux desktop (GTK/Qt shell, multi-column workspace, keyboard/mouse-first). The Android app is the reference implementation for the design philosophy.

- `design.md` rewritten as the full current visual & UX contract (brand tokens, dark-mode charcoal, flat rows, card rules, mail-row spec, categories, import flow, scrollbar, motion, copy rules).
- `editor-rules.md` rewritten with every standing directive earned building Android (copy-exact, fix-means-remove, auto-readjust, logos everywhere, no fabrication, no app emojis, OAuth + sync facts, push/continuity rules), adapted to Linux.
- Each phase carries a "2026-10-18 — Current-design update" section with the post-Phase-30 requirements for that phase. The original phase text is preserved; the update section wins on disagreement.
- Do NOT execute phases from this update. The user executes the plan separately.
