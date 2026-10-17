# Mail Organizer iPadOS — Requirements

## Purpose
iPadOS is a first-class tablet experience over the same Mail Organizer product core. It is not a phone layout stretched to a larger screen. Gmail remains the authoritative cloud source of truth.

## Platform boundary
This document applies only to iPadOS. iPadOS-specific implementation belongs under iPadOS/. iPhone, Android phone, Android tablet, macOS, Windows and Linux are separate platform plans.

## Shared source
Keep these common wherever technically appropriate:
- domain models
- Gmail normalization
- repositories
- synchronization
- classification
- company intelligence
- priority and Action Required
- search/indexing
- rules/corrections
- temporal/conversation intelligence
- Action Engine
- Calendar/Tasks abstractions
- automation
- optional AI routing
- persistence/state contracts

Do not clone common business logic into iPadOS.

## iPad information architecture
The larger viewport must expose more useful information simultaneously. Depending on available width:
- navigation/sidebar
- category/company context
- message list
- selected message/conversation detail
- contextual action/inspector region

The layout must adapt continuously to portrait, landscape, split configurations, Stage Manager/resizable windows and Dynamic Type.

## Mail contract
Preserve All Inbox, Primary, Promotional, Social, Spam and Starred; unified multi-account inbox; receiving-account identity; company filtering inside categories; company pinning independent from email star; organized Promotional/Social; first-class Spam/recovery; global Starred.

## Information density
Compared with iPhone, iPad may show sender, subject, preview, time, source account, category, company, priority, Action Required, attachment indicators and conversation state simultaneously when space permits. More information must mean more useful context, not clutter.

## Interaction
Support touch, pointer/trackpad, keyboard/focus, contextual menus, split view, Stage Manager and drag/drop where genuinely useful. Apple Pencil is optional and never required.

## Security and intelligence
Same product rules as mobile: official OAuth, least privilege, Keychain, account isolation, sanitized email HTML, no email-authorized external actions, deterministic-first intelligence, optional AI, explainable Action Engine and safe automation.

## Quality
Verify multiple viewport/window sizes, orientations, narrow split widths, full screen, Stage Manager, Dynamic Type, VoiceOver, pointer, keyboard, offline state, multi-account and large datasets. Build success alone is insufficient.

## 2026-10-18 addendum — cross-cutting requirements from the Android build

- Company logos in EVERY area where a mail row appears (same pipeline everywhere: bundled → asset catalog → favicon → letter fallback). People keep letter avatars. Never fabricate a logo. Images inside mail bodies are NEVER fetched.
- Never fabricate unread counts or any data the app does not have. No app-created emojis.
- Progress from the real sync only, never backwards; Continue gated on Done. The import conduit arc is not a progress bar; only the Mailstack ring is.
- Per-second Gmail throttle (250 units/user/sec) is the real sync limit, not the daily quota. Pace requests, honor `Retry-After`, never silently skip throttled GETs.
- One sign-in grants Gmail + Calendar + Tasks; gmail.modify stays incremental and confirmation-gated.
- A supplied reference/screenshot/logo is the spec to copy exactly — "nothing more, nothing less." "Fix means remove." Removal auto-readjusts everything that depended on the removed element's position.
- The iPad is not a large iPhone: multi-column workspace (sidebar → list → detail → optional inspector) with content-driven width thresholds; selection persists across columns and resizes; narrow widths fall back to the iPhone-like single-column contract.

