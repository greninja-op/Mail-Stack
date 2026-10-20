# Phase 09 — Google OAuth & Account Connection (PKCE S256)

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `desktop-core/src/oauth.rs` + `Windows/src/adapters/browser.rs`.

## Delivered

| Item | Location |
|------|----------|
| Base scopes (gmail.readonly + calendar.events + tasks + openid/email/profile) | `default_scopes()` |
| `gmail.modify` incremental only | `include_gmail_modify` flag |
| Client id from `MAILSTACK_OAUTH_CLIENT_ID` (not in source) | `OAuthClientConfig::from_env` |
| PKCE S256 (`sha2`) | `generate_pkce` |
| Auth URL: `prompt=consent`, `access_type=offline`, `include_granted_scopes=true` | `build_auth_url` |
| Pending PKCE consume-once | `PendingPkceStore` |
| System browser opener (http/https only) | `WindowsBrowser` / `open_oauth_url` |

## Verification

- desktop-core oauth tests: S256 shape, auth URL params, consume-once.
- Windows browser test: rejects `file:` / `javascript:` / `data:`.

## Blockers

- Live Google consent + redirect loopback not run here (needs real client id + console redirect allow-list).
- Testing-mode refresh tokens expire after 7 days (Google console fact).
