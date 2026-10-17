# Mail Organizer Android Tablet — Editor Rules

> **2026-10-18 update.** These rules now carry every standing directive earned building the Android phone app (1.0.39 + in-main fixes), adapted to the tablet. The phone app is the reference implementation for the design philosophy; the tablet carries it over with large-screen layout adaptation. An editor reads this file + `design.md` and can reproduce the UI without re-reading the phone code. Where this file and an older phase file disagree, this file wins. Do NOT start executing phases from this update — the user executes the plan separately; this update only makes the plan current.

## 0. Mandatory platform isolation — FIRST RULE OF EVERY PHASE

This is an **Android tablet-only** phase.

- Put all tablet-specific documents and implementation artifacts under repository-root `AndroidTablet/`.
- Never create or modify `iOS/`, `iPadOS/`, `Android/`, `macOS/`, `Windows/`, or `Linux/` during this plan.
- Do not place tablet-specific code in a sibling platform folder.
- Shared KMP/Android code remains shared; do not duplicate it into `AndroidTablet/` just to satisfy folder separation.
- If shared code changes, prove that the tablet phase requires the change.
- Never touch unrelated platform projects.
- Persist this rule in the root repository editor rules if a root rules file exists.

## 1. Continuity (repo-wide, non-negotiable)

- **Read `CONTEXT.md` fully before starting ANY work** on this repo (phase, fix, doc, push — anything). If the user ever says "read CONTEXT.md", stop everything and re-read it before continuing.
- **Update `CONTEXT.md` after EVERY prompt/request is handled** — no matter what. Update current build state, append dated entries, keep standing rules current. Never delete history — append and update.
- **Fetch the REMOTE `CONTEXT.md` first and merge** — never push a stale local copy over a newer remote one.
- Check `MEMORY.md` + memory search before acting, so the user never has to repeat what matters or correct the same mistake twice.

## 2. Pushing

- Branch: `main`. Push DIRECTLY to main — no pull requests, no separate branches. Pushes go through the GitHub App/API only, never git-over-SSH.
- Push as you go; never stall on push approval. One push call per phase = at most one approval prompt per phase.
- **Byte-verify every pushed file** against the remote afterward. When the user asks whether anything is unpushed, verify push state rather than asserting it.
- **Binaries: NEVER push raw binaries via the GitHub API** (it double-base64-corrupts them). Use base64 split parts + README. Split parts must NOT live under `res/` (AAPT rejects non-.png/.xml names there). Delete repo files with `delete_file` (needs the blob SHA).

## 3. Releases

- **No per-fix APKs.** Build + deliver a test APK ONLY when the user explicitly asks. Every release increments the version — never reuse a version name. APK naming: clean `Mailstack-X.Y.Z.apk` matching `versionName`.
- **Sign every test APK with the stable debug keystore** (`/home/hatch/keys/mailstack-debug.keystore`, SHA-1 `07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05`). Never change the signing key — the OAuth client is bound to it. Production signing uses a separate release key.
- **No emulator-only claims.** Never present on-device launch, live sign-in, token exchange, or real sync as verified until a real tablet has done them. Emulator success alone never means a phase is complete.
- Final release is a human-controlled decision. Do not publish automatically.

## 4. How to reply / work

- **Brief, fix-first replies.** "Just tell me the fix, only that particular thing." Exact numbers (test counts, hashes, sizes). Own mistakes plainly and fast.
- **When the user reports an error: audit fully before fixing.** (Lesson: a "403 permission" error was a per-second throttle mislabeled for days.) With screenshots: analyse them, find the root cause, fix exactly what was named.
- Suggestions that fit: concrete build fixes, exact console steps with values to copy verbatim, honest statements of what was checked here vs what only the device can prove.

## 5. Copy rules (design)

- A supplied reference / screenshot / logo is the SPEC to copy exactly — "nothing more, nothing less."
- "Fix means remove": delete unwanted components, don't restyle them.
- Auto-readjust on removal: when an element is removed, everything that depended on its position readjusts automatically, without being told.
- Follow the user's exact geometric description (e.g. "vertically centred, not horizontally").
- **STRICT Material Design 3 on every UI change, no exceptions** — including Material 3 large-screen / adaptive guidance on tablet. Call out any deliberate deviation before building it.
- **No app-created emojis anywhere.** Only emojis inside real mail content render.
- **Never fabricate** unread counts, account details, or any data the app does not have.
- Preserve tab state, cached data, ViewModels, scroll position, pane selection, account isolation, edge-to-edge.
- NEVER reintroduce Breeze artwork, glass UI (outside the import card's user-supplied frosted host), photo backgrounds, splash animation, or tab-triggered mailbox reloads.

## 6. Logos

- **Company logos in EVERY area where a mail row appears** — Mail list, Categories, detail pane, import rows, everywhere. Same pipeline everywhere: bundled logo → ICONS-ASSETS → sender's website favicon → letter fallback. People keep letter avatars.
- Never fabricate a logo. A user-supplied logo is the spec to copy exactly.
- The favicon fetch sends the sender's domain to Google (flagged in code).
- Images inside mail bodies are NEVER fetched (privacy: a fetch tells the sender the mail was opened). Blocked images render as nothing.

## 7. Animations

- Every animation discussed — built or only proposed — gets a study in `android/reference.html` (the shared animation library): live demo + annotated diagram + zoomed frames + handwritten notes + exact specs. Never skip adding it. Never remove an animation section, even if superseded. Tablet-specific adaptations (pane transitions, multi-window) get their own studies.
- The import animation rule: the conduit arc is NOT a progress bar (a pulse travels it); ONLY the Mailstack progress ring moves as the progress bar.

## 8. Architecture

Presentation → Application/Use Cases → Domain → Data → External APIs.

Shared KMP/Android code owns business/data behavior. Tablet Compose owns large-screen presentation and tablet platform concerns.

Reuse existing systems before creating replacements. Do not build duplicate sync engines, classification engines, Action Engines, search engines, rule engines, AI routers, or persistence systems.

## 9. Security

Use official OAuth/browser authentication. Never collect passwords. Least privilege. Secure token storage in EncryptedSharedPreferences. No secrets in source/log/UI/ordinary DB.

Email is hostile/untrusted input. Sanitize HTML. No JavaScript execution from email. No automatic URL activation or external action authorization based solely on email content.

Explicit account boundaries are mandatory. Account isolation is a first-class DB boundary (`AccountId`, FK-enforced) — never break it.

## 10. Product behavior

Gmail is cloud source of truth. Local state is minimized and rebuildable where possible.

User correction > user rule > deterministic intelligence > optional AI > unknown.

AI never authorizes external actions. AI is optional, disabled by default; AI output never directly executes actions; AI can never downgrade SECURITY signals.

Automation is finite, explainable, account-scoped, idempotent, and safety-validated. Email content cannot create or approve an automation.

## 11. Tablet UI/performance

Use Jetpack Compose + Material 3 + Android large-screen/adaptive guidance (WindowSizeClass, pane/scaffold patterns). Support portrait/landscape, split-screen/multi-window, resizing, stylus, mouse/trackpad (hover, context menus, wheel), keyboard focus/shortcuts, and useful drag & drop. Touch remains first-class.

Optimize sustained performance, memory, battery, thermal behavior, and scheduler-friendly background work.

Do not claim 120 FPS merely because hardware supports 120 Hz. Build a lightweight capability profile (tiers, not device names).

## 12. OAuth facts (current — from the phone build)

- Redirect URI: `com.greninjaop.mailorganizer:/oauth2redirect` (RFC 8252 single slash). Manifest scheme-only filter (VIEW + DEFAULT + BROWSABLE), `exported=true`, `MainActivity` singleTask. Pending PKCE flow persisted in EncryptedSharedPreferences.
- **One sign-in grants Gmail + Calendar + Tasks.** Base scopes = gmail.readonly + calendar.events + tasks + openid/email/profile. gmail.modify stays incremental, confirmation-gated, via GmailWriteUseCase only.
- Auth URL uses `prompt=consent` + `include_granted_scopes=true` + `access_type=offline`.
- Testing-mode consent → only listed test users can sign in; Testing-mode refresh tokens expire after 7 days.
- Console-side: Custom URI Scheme must be enabled on the OAuth client or Google rejects the request with Error 400.
- Sign-in opens in the plain system browser (trusted context) — never an in-app imitation credential form; the app never sees the password. The old Chrome-loop lesson: a sign-in that strands the user in the browser is a bug. Replay + consume-once on the redirect result; handle cold-start redirects (process may be killed while the browser is in front). Clear-all/disconnect also clears tokens.

## 13. Sync facts (current — from the phone build)

- Daily quota is effectively never the problem (one 500-message first sync ≈ 2,682 units vs 1,000,000,000 daily). The real limit is the **per-second throttle (250 units/user/sec)**.
- Pace requests (token bucket, ~10 req/sec) + honor Gmail's `Retry-After` header. **Never silently skip a throttled per-message GET** — retry dropped GETs instead; a skipped message is silently missing forever.
- Do NOT fire retry LIST calls while Gmail is throttling (they extend the throttle).
- Retries resume from the saved page cursor (cursor saved only after a page fully persists). Join in-flight syncs per account, never cancel/restart.
- Progress from the real sync only, never backwards; fixed denominator fetched once (Gmail profile `messagesTotal`); caps at 99% while running; Done = 100%.
- Background worker + scheduler call the same sync entry point (staleness > 15 min). Spam pass runs once per first sync.
- Classify failures honestly: per-second throttle ("wait a minute, tap once") vs daily quota ("resets at midnight Pacific, try tomorrow") vs permission vs offline. Never say "Reconnect" when offline.

## 14. Phase protocol

Read the root instructions and AndroidTablet master docs, identify the first incomplete phase, inspect the actual repository, implement only that phase, validate it, update documentation/status, and stop.

When a genuinely permanent rule is discovered, add it without deleting or rewriting unrelated rules.

Never claim unrun tests or device verification. Never commit secrets. Never auto-publish.
