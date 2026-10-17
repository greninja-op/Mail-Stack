# Mail Organizer Linux — Editor Rules

> **2026-10-18 update.** These rules now carry every standing directive earned building the Android app (1.0.39 + in-main fixes), adapted to Linux. The Android app is the reference implementation for the design philosophy. An editor reads this file + `design.md` and can reproduce the UI without re-reading the Android code. Where this file and an older phase file disagree, this file wins. Do NOT start executing phases from this update — the user executes the plan separately; this update only makes the plan current.

## 0. Mandatory platform isolation — FIRST RULE OF EVERY PHASE

This is a **Linux** phase.

- Put all Linux-specific documents and implementation artifacts under the Linux target/application area.
- Never create or modify `macOS/`, `Windows/`, `Android/`, `AndroidTablet/`, `iOS/`, or `iPadOS/` during this plan.
- Do not place Linux-specific code in a sibling platform folder.
- Shared desktop Rust-core changes are permitted only when required by an explicitly shared contract and must preserve all existing platform contracts.
- If shared Rust-core code changes, prove that the Linux phase requires the change.
- Never touch unrelated platform projects.

## 1. Continuity (repo-wide, non-negotiable)

- **Read `CONTEXT.md` fully before starting ANY work** on this repo. If the user ever says "read CONTEXT.md", stop everything and re-read it.
- **Update `CONTEXT.md` after EVERY prompt/request is handled** — no matter what. Update current build state, append dated entries, keep standing rules current. Never delete history — append and update.
- **Fetch the REMOTE `CONTEXT.md` first and merge** — never push a stale local copy over a newer remote one.
- Check `MEMORY.md` + memory search before acting, so the user never has to repeat what matters or correct the same mistake twice.

## 2. Pushing

- Branch: `main`. Push DIRECTLY to main — no pull requests, no separate branches. Pushes go through the GitHub App/API only, never git-over-SSH.
- Push as you go; never stall on push approval. One push call per phase = at most one approval prompt per phase.
- **Byte-verify every pushed file** against the remote afterward. When the user asks whether anything is unpushed, verify push state rather than asserting it.
- **Binaries: NEVER push raw binaries via the GitHub API** (it double-base64-corrupts them). Use base64 split parts + README. Split parts must NOT live under a build-compiled asset directory. Delete repo files with `delete_file` (needs the blob SHA).

## 3. Releases

- **No per-fix test builds.** Build + deliver a test build ONLY when the user explicitly asks. Every release increments the version — never reuse a version name.
- **No single-machine claims.** Never present launch, live sign-in, token exchange, or real sync as verified until a real Linux machine has done them on a supported environment. Local-build success alone never means a phase is complete.
- Final release is a human-controlled decision. Do not publish automatically.

## 4. How to reply / work

- **Brief, fix-first replies.** "Just tell me the fix, only that particular thing." Exact numbers (test counts, hashes, sizes). Own mistakes plainly and fast.
- **When the user reports an error: audit fully before fixing.** (Lesson: a "403 permission" error was a per-second throttle mislabeled for days.) With screenshots: analyse them, find the root cause, fix exactly what was named.
- Suggestions that fit: concrete build fixes, exact console steps with values to copy verbatim, honest statements of what was checked here vs what only the user's machine can prove.

## 5. Copy rules (design)

- A supplied reference / screenshot / logo is the SPEC to copy exactly — "nothing more, nothing less."
- "Fix means remove": delete unwanted components, don't restyle them.
- Auto-readjust on removal: when an element is removed, everything that depended on its position readjusts automatically, without being told.
- Follow the user's exact geometric description (e.g. "vertically centred, not horizontally").
- **Strict platform design-system adherence on every UI change.** On Linux: the Mailstack design language in `design.md` rendered with the GTK/Qt desktop shell and Linux desktop conventions. Call out any deliberate deviation before building it.
- **No app-created emojis anywhere.** Only emojis inside real mail content render.
- **Never fabricate** unread counts, account details, or any data the app does not have.
- Preserve pane sizes, selection state, cached data, scroll position, account isolation, window state.
- NEVER introduce photo backgrounds, glass UI (outside the import card's user-supplied frosted host), splash animation, or view-switch-triggered mailbox reloads.

## 6. Logos

- **Company logos in EVERY area where a mail row appears** — Mail list, Categories, import rows, everywhere. Same pipeline everywhere: bundled logo → asset catalog → sender's website favicon → letter fallback. People keep letter avatars.
- Never fabricate a logo. A user-supplied logo is the spec to copy exactly.
- The favicon fetch sends the sender's domain to Google (flagged in code).
- Images inside mail bodies are NEVER fetched (privacy: a fetch tells the sender the mail was opened). Blocked images render as nothing.

## 7. Animations

- Every animation discussed — built or only proposed — gets a study in the platform's animation/design reference (Linux: the design-reference section / `design.md` motion notes): spec + annotated diagram + zoomed frames + notes + exact specs. Never skip adding it. Never remove an animation study, even if superseded.
- The import animation rule: the conduit arc is NOT a progress bar (a pulse travels it); ONLY the Mailstack progress ring moves as the progress bar.

## 8. Architecture

Presentation → Application/Use Cases → Domain → Data → External APIs/adapters.

Rust is the primary implementation language for Linux and the reusable desktop core. Do not introduce duplicated business logic in C#, C++, Java, Kotlin, Python, JavaScript, or another language.

Keep the Rust API boundary explicit, narrow, testable, cancellation-safe, and ownership-safe. Prevent business logic leakage into UI.

Reuse existing systems before creating replacements. Do not build duplicate sync engines, classification engines, Action Engines, search engines, rule engines, AI routers, or persistence systems.

## 9. Security

Use official OAuth/browser authentication. Never collect passwords. Never use cookie scraping, mailbox scraping, or AccessibilityService-style mailbox control. Least privilege. Secure token storage via the platform Secret Service/keyring. No secrets in source/log/UI/ordinary DB.

Email is hostile/untrusted input. Sanitize HTML. No JavaScript execution from email. No automatic URL activation or external action authorization based solely on email content.

Explicit account boundaries are mandatory. Account isolation is a first-class boundary — never break it.

## 10. Product behavior

Gmail is cloud source of truth. Local state is minimized and rebuildable where possible.

User correction > user rule > deterministic intelligence > optional AI > unknown.

AI never authorizes external actions. AI is optional, disabled by default; AI output never directly executes actions; AI can never downgrade SECURITY signals.

Automation is finite, explainable, account-scoped, idempotent, and safety-validated. Email content cannot create or approve an automation.

## 11. Linux UI/performance

Use the GTK/Qt desktop shell and Linux desktop conventions: resizable multi-column workspace, keyboard-first navigation, mouse/trackpad/wheel, context menus, standard desktop focus behavior, system notifications, high-DPI scaling, dark/light themes, reduced motion.

Optimize sustained performance, memory, battery/CPU use, and scheduler-friendly background work.

Do not claim 120 FPS merely because hardware supports 120 Hz. Build a lightweight capability profile (tiers, not device names).

## 12. OAuth facts (current — from the Android build)

- Redirect URI: `com.greninjaop.mailorganizer:/oauth2redirect` (RFC 8252 single slash). Linux: loopback redirect or custom-scheme handler registered with the desktop environment + callback handling for both cold-start and warm redirects.
- **One sign-in grants Gmail + Calendar + Tasks.** Base scopes = gmail.readonly + calendar.events + tasks + openid/email/profile. gmail.modify stays incremental, confirmation-gated.
- Auth URL uses `prompt=consent` + `include_granted_scopes=true` + `access_type=offline`.
- Testing-mode consent → only listed test users can sign in; Testing-mode refresh tokens expire after 7 days.
- Console-side: Custom URI Scheme must be enabled on the OAuth client or Google rejects the request with Error 400.
- Pending PKCE flow persisted in the Secret Service/keyring. Replay + consume-once on the redirect result.

## 13. Sync facts (current — from the Android build)

- Daily quota is effectively never the problem (one 500-message first sync ≈ 2,682 units vs 1,000,000,000 daily). The real limit is the **per-second throttle (250 units/user/sec)**.
- Pace requests (token bucket, ~10 req/sec) + honor Gmail's `Retry-After` header. **Never silently skip a throttled per-message GET** — retry dropped GETs instead; a skipped message is silently missing forever.
- Retries resume from the saved page cursor. Join in-flight syncs per account, never cancel/restart.
- Progress from the real sync only, never backwards; fixed denominator fetched once; caps at 99% while running; Done = 100%.

## 14. Phase protocol

Before every phase read requirements, spec, design, editor-rules, the current phase, and actual repository state. Implement only that phase.

After implementation build, run automated tests, launch and inspect runtime behavior, perform relevant data/security/accessibility/visual checks, fix failures, rebuild and retest.

Update documentation and status only after verification. Add genuinely permanent new rules without deleting unrelated rules.

Never claim unrun tests or unsupported platform verification. Never commit credentials or secrets. Stop after the current phase.
