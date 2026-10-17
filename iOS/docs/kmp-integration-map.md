# KMP ↔ SwiftUI Integration Map (Phase 0)

This map describes **what iOS should eventually consume** from shared Kotlin.  
It is a planning artifact. Nothing here implies the shared module already exists.

## Reality check

Today all of these live inside `android/app` as Android Kotlin. Classification:

| Area | Portability today | Notes |
|------|-------------------|-------|
| `core/classify`, `core/priority`, `core/company`, `core/search`, `core/rules`, `core/temporal`, `core/conversation`, `core/actions`, `core/cleanup`, `core/analytics`, `core/ai` (pure parts) | **PARTIAL → extractable** | Mostly pure Kotlin; verify Android-free before moving |
| `core/email` (parser/sanitizer) | **PARTIAL** | Good candidate; audit Android deps |
| `domain/*` use cases | **PARTIAL** | Often depend on repository interfaces — good if interfaces move to common |
| `data/repository` interfaces | **PARTIAL** | Interfaces → common; Room impl → androidMain |
| `data/local` Room | **Android-only** | Need SQLDelight or sqldelight/alternative for common, or expect/actual DB |
| `data/auth` EncryptedSharedPreferences | **Android-only** | iOS: Keychain actual |
| `data/sync` WorkManager pieces | **PARTIAL** | Coordinator logic extractable; scheduler is platform |
| `data/http` | **PARTIAL** | Transport interface → common; URLSession/OkHttp actuals |
| `ui/*` Compose | **Android-only** | SwiftUI rebuild under `iOS/` |

## Target shared surface (Swift consumes)

Stable, account-scoped APIs (names indicative, not final):

1. **Accounts** — list, active selection (single/unified), connect/disconnect *triggers* (no raw tokens)
2. **Mail** — observe threads/messages by destination; open thread; star; body fetch hooks
3. **Sync** — start/status/progress (fixed total, never-backwards semantics)
4. **Intelligence** — classification, company filter, priority, action-required explanations
5. **Search** — structured query in; ranked results out
6. **Rules / corrections** — CRUD + apply + undo
7. **Temporal / conversation** — extracted items + conversation state
8. **Actions** — candidates, review, confirm (external effects platform-backed)
9. **Integrations** — Calendar/Tasks status (Gmail is not an “integration row”)
10. **Privacy** — inventory, purge, account wipe
11. **AI (optional)** — availability + on-demand advisory only

## Platform boundary (iOS owns)

- SwiftUI screens, navigation, design tokens (match `execution plan/iOS/design.md`)
- Keychain token storage
- `ASWebAuthenticationSession` + URL scheme handling (cold + warm)
- Background refresh / notifications scheduling honesty
- EventKit / Reminders adapters
- Accessibility (Dynamic Type, VoiceOver, Reduce Motion)
- Entitlements, Privacy Info, App Store metadata

## Logo / progress / sync contracts to preserve

From Android 1.0.39 + standing rules — shared semantics SwiftUI must present:

- Logo pipeline: bundled → asset catalog → favicon → letter (people = letter)
- Progress: fixed denominator once; cap 99% while running; Done = 100%; never fake %
- Sync: join in-flight per account; resume page cursor; pace for per-second throttle
- One OAuth grant: gmail.readonly + calendar.events + tasks (+ openid/email/profile)

## Phase 1 minimum

Enough shared/bootstrap surface that the SwiftUI shell compiles and can inject stubs for later mailbox screens — without implementing OAuth, sync, or intelligence yet.
