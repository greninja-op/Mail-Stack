# macOS Editor Rules

> **2026-10-18 update.** These rules now carry every standing directive earned building the Android app (1.0.39 + in-main fixes), adapted to macOS. The Android app is the reference implementation for the design philosophy. An editor reads this file + `design.md` and can reproduce the UI without re-reading the Android code. Where this file and an older phase file disagree, this file wins. Do NOT start executing phases from this update — the user executes the plan separately; this update only makes the plan current.

## 0. Mandatory platform isolation — FIRST RULE OF EVERY PHASE

This is a **macOS** phase.

- Put all macOS-specific documents and implementation artifacts under the macOS platform boundary.
- Never create or modify `iOS/`, `iPadOS/`, `AndroidTablet/`, `Android/`, `Windows/`, or `Linux/` during this plan.
- Do not place macOS-specific code in a sibling platform folder.
- Shared Rust desktop core changes are allowed only when genuinely platform-neutral and directly required by this phase.
- Never touch unrelated platform projects.

## 1. Architecture — Rust first (PRESERVED, not changed by this update)

1. **Rust is the primary implementation language for macOS and the reusable desktop core.**
2. Swift/SwiftUI is used for native macOS UI and OS integration where appropriate; it must not duplicate Rust business logic.
3. Keep the Rust core reusable by Windows. Avoid macOS-specific assumptions in shared Rust modules.
4. Keep the Rust↔Swift boundary narrow, explicit, memory-safe, testable, cancellation-aware, and ownership-safe.
5. Keep domain/data/business logic in Rust when platform-neutral.
6. Presentation → Application/Use Cases → Domain → Data → External APIs.
7. Reuse existing systems before creating replacements. Do not build duplicate sync engines, classification engines, Action Engines, search engines, rule engines, AI routers, or persistence systems.

## 2. Continuity (repo-wide, non-negotiable)

- **Read `CONTEXT.md` fully before starting ANY work** on this repo. If the user ever says "read CONTEXT.md", stop everything and re-read it.
- **Update `CONTEXT.md` after EVERY prompt/request is handled** — no matter what. Update current build state, append dated entries, keep standing rules current. Never delete history — append and update.
- **Fetch the REMOTE `CONTEXT.md` first and merge** — never push a stale local copy over a newer remote one.
- Check `MEMORY.md` + memory search before acting, so the user never has to repeat what matters or correct the same mistake twice.

## 3. Pushing

- Branch: `main`. Push DIRECTLY to main — no pull requests, no separate branches. Pushes go through the GitHub App/API only, never git-over-SSH.
- Push as you go; never stall on push approval. One push call per phase = at most one approval prompt per phase.
- **Byte-verify every pushed file** against the remote afterward. When the user asks whether anything is unpushed, verify push state rather than asserting it.
- **Binaries: NEVER push raw binaries via the GitHub API** (it double-base64-corrupts them). Use base64 split parts + README. Split parts must NOT live under a build-compiled asset dir. Delete repo files with `delete_file` (needs the blob SHA).

## 4. Releases

- **No per-fix builds.** Build + deliver a test build ONLY when the user explicitly asks. Every release increments the version — never reuse a version name.
- **No device-only claims.** Never present on-device launch, live sign-in, token exchange, or real sync as verified until a real Mac has done them.
- Final release is a human-controlled decision. Do not publish automatically. Production release is an explicit final gate (signing, hardened runtime, sandbox, notarization + stapling where applicable).

## 5. How to reply / work

- **Brief, fix-first replies.** "Just tell me the fix, only that particular thing." Exact numbers (test counts, hashes, sizes). Own mistakes plainly and fast.
- **When the user reports an error: audit fully before fixing.** (Lesson: a "403 permission" error was a per-second throttle mislabeled for days.) With screenshots: analyse them, find the root cause, fix exactly what was named.
- Suggestions that fit: concrete build fixes, exact console steps with values to copy verbatim, honest statements of what was checked here vs what only the user's machine can prove.

## 6. Copy rules (design)

- A supplied reference / screenshot / logo is the SPEC to copy exactly — "nothing more, nothing less."
- "Fix means remove": delete unwanted components, don't restyle them.
- Auto-readjust on removal: when an element is removed, everything that depended on its position readjusts automatically, without being told.
- Follow the user's exact geometric description (e.g. "vertically centred, not horizontally").
- **Strict platform design-system adherence on every UI change.** On macOS: SwiftUI + Apple HIG, SF Symbols for system glyphs with the same semantics as Android's Material Symbols. Call out any deliberate deviation before building it.
- **No app-created emojis anywhere.** Only emojis inside real mail content render.
- **Never fabricate** unread counts, account details, or any data the app does not have.
- Preserve selection state, cached data, account isolation, window state, and state restoration.
- NEVER introduce photo backgrounds, glass UI (outside the import card's user-supplied frosted host), splash animation, or destination-switch-triggered mailbox reloads.

## 7. Logos

- **Company logos in EVERY area where a mail row appears** — Mail list, Categories, import rows, everywhere. Same pipeline everywhere: bundled logo → asset catalog → sender's website favicon → letter fallback. People keep letter avatars.
- Never fabricate a logo. A user-supplied logo is the spec to copy exactly.
- The favicon fetch sends the sender's domain to Google (flagged in code).
- Images inside mail bodies are NEVER fetched (privacy: a fetch tells the sender the mail was opened). Blocked images render as nothing.

## 8. Animations

- Every animation discussed — built or only proposed — gets a study in the platform's animation/design reference: live demo or spec + annotated diagram + zoomed frames + notes + exact specs. Never skip adding it. Never remove an animation study, even if superseded.
- The import animation rule: the conduit arc is NOT a progress bar (a pulse travels it); ONLY the Mailstack progress ring moves as the progress bar.

## 9. Security

Use official Google OAuth/Gmail APIs and least-privilege scopes. Never collect passwords or reuse cookies. Store secrets/tokens securely in Keychain; never commit, log, screenshot, or expose credentials. No secrets in source/logs/UI/ordinary DB.

Email content is untrusted. Sanitize HTML, do not execute embedded JavaScript, and never let email text authorize external actions.

Explicit account boundaries are mandatory. Account isolation is a first-class boundary — never break it.

## 10. Product behavior

Gmail is cloud source of truth; local data is account-scoped, minimized and rebuildable where practical.

User correction > user rule > deterministic intelligence > optional AI > unknown.

AI never authorizes external actions. AI is optional, disabled by default; AI output never directly executes actions; AI can never downgrade SECURITY signals. AI output must be schema/domain validated and cannot directly access Gmail, Calendar, Tasks, filesystem, shell, browser, Keychain, or OS automation.

Automation is finite, explainable, account-scoped, idempotent, and safety-validated. Email content cannot create or approve an automation.

Consequential external actions require confirmation, account scope and idempotency. Never silently send/reply/forward, permanently delete, unsubscribe, or perform destructive bulk actions.

## 11. macOS UI/performance

Native macOS UI must be adaptive, accessible, keyboard/pointer friendly and dark-mode capable. Use native resizable desktop windows, split views, menus, shortcuts, pointer/trackpad, context menus, multiple windows/scenes, dark mode, VoiceOver, accessibility text sizing, reduced motion, and responsive layouts.

Optimize sustained performance, memory, battery, and scheduler-friendly background work. Do not claim 120 FPS merely because hardware supports 120 Hz. Build a lightweight capability profile (tiers, not device names).

## 12. OAuth facts (current — from the Android build)

- Redirect URI pattern `com.greninjaop.mailorganizer:/oauth2redirect` (RFC 8252 single slash). macOS: custom URL scheme registered in Info.plist + Apple-event handling for both cold-start and warm redirects. Pending PKCE flow persisted in Keychain. Replay + consume-once on the redirect result.
- **One sign-in grants Gmail + Calendar + Tasks.** Base scopes = gmail.readonly + calendar.events + tasks + openid/email/profile. Auth URL uses `prompt=consent` + `include_granted_scopes=true` + `access_type=offline`. gmail.modify stays incremental, confirmation-gated.
- Console-side: Custom URI Scheme must be enabled on the OAuth client (Google rejects with Error 400 otherwise). Testing-mode consent → only listed test users; Testing-mode refresh tokens expire after 7 days.
- Sign-in opens in the system browser (trusted context) — never an in-app imitation credential form; the app never sees the password. A sign-in that strands the user in the browser is a bug, not a user error.
- Clear-all/disconnect also clears tokens.

## 13. Sync facts (current — from the Android build)

- **Daily quota is effectively never the problem** (one 500-message first sync ≈ 2,682 units vs 1,000,000,000 daily). The real limit is the **per-second throttle (250 units/user/sec)**.
- Pace requests (token bucket, ~10 req/sec) + honor Gmail's `Retry-After` header. **Never silently skip a throttled per-message GET** — retry dropped GETs; a skipped message is silently missing forever.
- Do NOT fire retry LIST calls while Gmail is throttling (they extend the throttle).
- Join in-flight syncs per account, never cancel/restart. Cursor resume (cursor saved only after a page fully persists).
- Progress from the real sync only, never backwards: total fetched ONCE (Gmail profile `messagesTotal`) before the loop, bounded by the per-run max; caps at 99% while running; Done = 100%.
- Background refresh + scheduler call the same sync entry point (staleness > 15 min). Spam pass runs once per first sync.
- Classify failures honestly: per-second throttle ("wait a minute, tap once") vs daily quota ("resets at midnight Pacific, try tomorrow") vs permission vs offline. Never say "Reconnect" when offline.

## 14. Phase protocol

Execute exactly one phase per session and inspect the real repository before editing. Read the root instructions and macOS master docs, identify the first incomplete phase, implement only that phase, validate it, update documentation/status, and stop.

After each phase update status and add only genuinely permanent rules; never delete unrelated rules. Build/test/run/fix/rebuild/retest before declaring completion. Never claim unrun tests or device verification. Never commit secrets. Never auto-publish.

Do not claim tests, runtime behavior, screenshots, performance, signing or notarization unless actually observed.
