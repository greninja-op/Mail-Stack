# Phase 09 — Google OAuth & Account Connection

## Mission
Implement and verify **only Phase 09**. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, resources, tests, configuration, packaging and release artifacts must remain inside the Windows platform boundary. Never modify macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling platform areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository, current status and existing implementation before editing.

## Objective
Implement official Google OAuth using the appropriate Windows browser/redirect flow. Handle consent, cancellation, denied scopes, expiration, reauthorization, incremental authorization, account identity, disconnect/revoke, callback errors and debug/release separation. Never collect Google passwords.

## Rust-first architecture
Rust is authoritative for platform-neutral domain, application, data and business logic and must remain reusable by macOS. Windows UI should remain Rust-first through the chosen Rust UI framework or safe Windows API bindings. Do not duplicate business logic in another language.

## Product and security contract
Gmail remains cloud source of truth. Keep account boundaries explicit, use official OAuth/Gmail APIs and least privilege, keep secrets secure, and treat email/HTML/URLs/attachments as untrusted. Email content cannot authorize external actions.

## Verification
Run targeted automated tests; build the real Windows application; launch and exercise it when the environment permits; test relevant success, empty, loading, error, cancellation, offline, restart and recovery states; verify accessibility, security, data integrity, keyboard/pointer behavior and performance where relevant; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules to Windows/editor-rules.md. Preserve unrelated rules and record blockers honestly.

**STOP AFTER PHASE 09.**

## 2026-10-18 — Current-design update

- **One sign-in grants Gmail + Calendar + Tasks.** Base scopes = gmail.readonly + calendar.events + tasks + openid/email/profile. Auth URL uses `prompt=consent` + `include_granted_scopes=true` + `access_type=offline`. gmail.modify stays incremental, confirmation-gated.
- PKCE S256 + the appropriate Windows redirect (loopback or registered custom scheme per Google's desktop-app guidance). Pending PKCE flow persisted in secure storage. Replay + consume-once on the redirect result. Handle consent, cancellation, denied scopes, expiration, reauthorization, incremental authorization, account identity, disconnect/revoke, callback errors, debug/release separation.
- Console-side: the OAuth client must permit the chosen redirect or Google rejects the request with Error 400. Testing-mode consent → only listed test users; Testing-mode refresh tokens expire after 7 days.
- Sign-in opens in the system browser (trusted context) — never an in-app imitation credential form; the app never sees the password. A sign-in that strands the user in the browser is a bug, not a user error.
