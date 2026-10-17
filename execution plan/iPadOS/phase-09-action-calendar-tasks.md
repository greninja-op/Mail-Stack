# Phase 9 — Google OAuth & Account Connection

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Implement official Google OAuth for iPadOS using browser/system authentication mechanisms. Configure callback/deep-link state, least-privilege Gmail scopes, incremental authorization, cancellation, retry, token refresh/expiry, account identity and disconnect/revoke. Never collect Gmail passwords or build a fake Google credential form. Validate success, denial, cancellation, invalid callback/state, expired token, revoked access, second-account connection and logs. Do not implement broad Gmail synchronization.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Official Google OAuth via ASWebAuthenticationSession (trusted context) — never an in-app imitation credential form; the app never sees the password.
- Redirect URI `com.greninjaop.mailorganizer:/oauth2redirect` (RFC 8252 single slash). Custom URL scheme in Info.plist + `onOpenURL` for cold-start AND warm redirects. Pending PKCE flow in Keychain. Replay + consume-once on the redirect result.
- **One sign-in grants Gmail + Calendar + Tasks.** Base scopes = gmail.readonly + calendar.events + tasks + openid/email/profile. Auth URL uses `prompt=consent` + `include_granted_scopes=true` + `access_type=offline`. gmail.modify stays incremental, confirmation-gated.
- Console-side: Custom URI Scheme must be enabled on the OAuth client (Google rejects with Error 400 otherwise). Testing-mode consent → only listed test users; Testing-mode refresh tokens expire after 7 days.
- Validate success, denial, cancellation, invalid callback/state, expired token, revoked access, second-account connection, and logs. Do not implement broad Gmail synchronization.
