# Phase 09 — Google OAuth

**Status:** COMPLETE — 2026-10-20

COMPLETE (desktop-core). oauth.rs: PKCE S256, auth URL with prompt=consent / include_granted_scopes / access_type=offline, base scopes (gmail.readonly + calendar.events + tasks + openid/email/profile), client id from MAILSTACK_GOOGLE_CLIENT_ID, consume-once pending state.

Shared crate: `desktop-core`. Windows UI adapters consume these contracts; do not reimplement precedence/sync honesty in presentation code.
