# Phase 3 — Google OAuth & Gmail iOS Connection

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. All iOS-specific OAuth/browser/configuration/tests/docs belong under `iOS/`. Never touch sibling platform plans or implementations. Shared OAuth/domain abstractions may change only when required.

## Mission

Implement production-shaped Google authentication for iPhone using official Google OAuth/browser mechanisms and least privilege.

## Scope

- official OAuth authorization flow
- secure callback/deep-link/universal-link boundary as appropriate
- Keychain-backed token handling
- account identity normalization
- token refresh/expiry/error states
- disconnect/revoke behavior
- least-privilege Gmail scopes
- incremental authorization architecture
- authentication UI states
- no-password rule
- account cancellation/retry

Never create a fake Google login form or ask users to enter Gmail passwords.

## Security validation

Inspect redirect handling, state/nonce where applicable, token storage, logs, screenshots, error messages, and account isolation. Confirm OAuth errors do not expose credentials.

Do not implement broad Gmail synchronization yet.

**STOP AFTER PHASE 3.**

## 2026-10-18 — Current-design update

- Redirect URI `com.greninjaop.mailorganizer:/oauth2redirect` (RFC 8252 single slash). iOS: custom URL scheme in Info.plist + `onOpenURL` for cold-start AND warm redirects. Pending PKCE flow persisted in Keychain. Replay + consume-once on the redirect result.
- **One sign-in grants Gmail + Calendar + Tasks.** Base scopes = gmail.readonly + calendar.events + tasks + openid/email/profile. Auth URL uses `prompt=consent` + `include_granted_scopes=true` + `access_type=offline`. gmail.modify stays incremental, confirmation-gated.
- Console-side: Custom URI Scheme must be enabled on the OAuth client (Google rejects with Error 400 otherwise). Testing-mode consent → only listed test users; Testing-mode refresh tokens expire after 7 days.
- Sign-in opens in the system browser via ASWebAuthenticationSession (trusted context) — never an in-app imitation credential form; the app never sees the password. The old Chrome-loop lesson: a sign-in that strands the user in the browser is a bug, not a user error.
- Clear-all/disconnect also clears tokens.
