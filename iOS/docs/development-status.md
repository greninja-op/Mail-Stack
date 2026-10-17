# Mailstack iOS — Development Status

Plan source: `execution plan/iOS/`  
Implementation root: `iOS/`

Status legend: `[ ]` not started · `[-]` in progress · `[x]` complete and verified · `[!]` blocked

| Phase | Name | Status | Notes |
|------:|------|--------|-------|
| 0 | Platform Architecture Audit & Boundary Foundation | `[x]` | 2026-10-18 — docs + `iOS/` boundary; no Xcode yet |
| 1 | Application Shell & KMP Integration | `[ ]` | Needs macOS/Xcode; shared module missing |
| 2 | Local Persistence & State | `[ ]` | |
| 3 | Google OAuth & Gmail Connection | `[ ]` | |
| 4 | Gmail Sync & iOS Lifecycle | `[ ]` | |
| 5 | Mailbox, Navigation & Email Viewer | `[ ]` | |
| 6 | Classification, Company & Categories | `[ ]` | |
| 7 | Search, Priority & Action Required | `[ ]` | |
| 8 | Rules, Temporal & Conversation | `[ ]` | |
| 9 | Action Engine, Calendar & Tasks | `[ ]` | |
| 10 | Multi-Account & Unified Inbox | `[ ]` | |
| 11 | Offline, Background & Notifications | `[ ]` | |
| 12 | Privacy, Security & Data Protection | `[ ]` | |
| 13 | Optional AI Fallback | `[ ]` | |
| 14 | Advanced Automation | `[ ]` | |
| 15 | Performance, Accessibility & Adaptive UX | `[ ]` | |
| 16 | Comprehensive QA | `[ ]` | |
| 17 | Production Signing, App Store & OAuth | `[ ]` | |
| 18 | Final Production Hardening & Release | `[ ]` | |

## Current blockers

1. No Kotlin Multiplatform shared module in the repo (Android is a single Android app module).
2. No Xcode project under `iOS/` yet.
3. Cloud Agent host is Linux — cannot run Simulator builds here.

## Last completed

**Phase 0** — see `phase-00-architecture-audit.md`.
