# Mailstack iOS — Development Status

Plan source: `execution plan/iOS/`  
Implementation root: `iOS/`  
Continuity / skips: **`iOS/CONTEXT.md`** (root `CONTEXT.md` is not used for iOS)

Status legend: `[ ]` not started · `[-]` in progress · `[x]` source complete · `[!]` blocked / skipped verification

| Phase | Name | Status | Notes |
|------:|------|--------|-------|
| 0 | Platform Architecture Audit | `[x]` | docs |
| 1 | Application Shell & KMP Integration | `[x]` | Simulator `[!]` |
| 2 | Local Persistence & State | `[x]` | In-memory demo; SQLite `[!]` |
| 3 | Google OAuth & Gmail Connection | `[x]` | Seams + Keychain; live OAuth `[!]` |
| 4 | Gmail Sync & iOS Lifecycle | `[x]` | Demo sync; live Gmail `[!]` |
| 5 | Mailbox, Navigation & Email Viewer | `[x]` | Device UI `[!]` |
| 6 | Classification, Company & Categories | `[x]` | Logo pipeline partial `[!]` |
| 7 | Search, Priority & Action Required | `[x]` | In-memory search |
| 8 | Rules, Temporal & Conversation | `[x]` | UI seam |
| 9 | Action Engine, Calendar & Tasks | `[x]` | Seams; live APIs `[!]` |
| 10 | Multi-Account & Unified Inbox | `[x]` | Demo multi-account |
| 11 | Offline, Background & Notifications | `[x]` | Plist + settings; BG tasks `[!]` |
| 12 | Privacy, Security & Data Protection | `[x]` | Sanitize + Keychain |
| 13 | Optional AI Fallback | `[x]` | Stub off |
| 14 | Advanced Automation | `[x]` | Stub |
| 15 | Performance, Accessibility & Adaptive UX | `[x]` | Hardware a11y `[!]` |
| 16 | Comprehensive QA | `[x]` | Unit tests authored; XCTest run `[!]` |
| 17 | Production Signing, App Store & OAuth | `[!]` | Checklist only |
| 18 | Final Production Hardening & Release | `[!]` | Checklist only |

## Current blockers

1. No macOS / Xcode on Cloud Agent → no Simulator verification.
2. Live Google OAuth / Gmail / Calendar / Tasks require client secrets + Mac.
3. App Store signing requires Apple Developer account (Phase 17–18).
