# Phase 09 — Google OAuth & Account Connection

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Implement official Google OAuth/browser flow for tablet. Configure callback/deep-link handling, least privilege/incremental scopes, token refresh, cancellation, retry, account identity and disconnect/revoke. Never collect Gmail passwords. Test success, denial, cancellation, invalid callback, expiry, revocation and second-account connection. No broad sync yet.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 09.**

## 2026-10-18 — Current-design update

- Redirect URI `com.greninjaop.mailorganizer:/oauth2redirect` (RFC 8252 single slash). Manifest scheme-only filter (VIEW + DEFAULT + BROWSABLE), `exported=true`, `MainActivity` singleTask. Handle cold-start redirects (process may be killed while the browser is in front) + warm redirects. Replay + consume-once on the redirect result.
- **One sign-in grants Gmail + Calendar + Tasks.** Base scopes = gmail.readonly + calendar.events + tasks + openid/email/profile. Auth URL uses `prompt=consent` + `include_granted_scopes=true` + `access_type=offline`. gmail.modify stays incremental, confirmation-gated.
- Console-side: Custom URI Scheme must be enabled on the OAuth client (Google rejects with Error 400 otherwise). Testing-mode consent → only listed test users; Testing-mode refresh tokens expire after 7 days.
- Sign-in opens in the plain system browser (trusted context) — never an in-app imitation credential form; the app never sees the password. The old Chrome-loop lesson: a sign-in that strands the user in the browser is a bug, not a user error.
- Test success, denial, cancellation, invalid callback, expiry, revocation, and second-account connection. No broad sync yet.
