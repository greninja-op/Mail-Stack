# Phase 29 — Production OAuth & Play Store Preparation

## Status: In progress (2026-10-11) — keystore DONE, Cloud/Play steps need the user

This document records the production preparation work for Mailstack 1.0.0.

## Release Keystore — DONE (2026-10-11)

A real production keystore has been generated (RSA 4096, validity 30 years):

- **Alias:** `mailstack`
- **SHA-1:** `5A:34:E6:F1:10:1B:73:62:01:B3:67:16:5B:06:8E:6A:E7:E6:CE:D5`
- **SHA-256:** `CD:15:7F:9A:7C:07:ED:87:95:6E:25:D2:56:15:B4:CA:23:D0:8B:E5:FF:F4:A4:F5:D3:41:F1:20:1E:59:ED:7F`
- **Location:** kept OUTSIDE the repo (never committed). Back it up somewhere safe — losing it means you can never update the app on Play Store.
- **Password:** stored alongside the keystore file, OUTSIDE the repo. Ask the assistant for its location; it is never written into the repo, chat logs, or memory notes.
- **Build wiring:** `android/app/build.gradle.kts` signs `release` builds with this keystore when `MAILSTACK_KEYSTORE_PATH` and `MAILSTACK_KEYSTORE_PASSWORD` env vars are set (falls back to debug signing otherwise so local builds never break).

## Production OAuth Configuration

### Debug vs Production

The app currently uses a **debug** OAuth client:
- Client ID: `922852545613-f0vel0oe1i2g10rc9cst028s8mhnhl13.apps.googleusercontent.com`
- Package: `com.greninjaop.mailorganizer`
- Debug SHA-1: `D8:B7:ED:C4:41:31:5C:B8:C8:CE:96:83:55:15:F7:43:C8:56:1E:7B`

### Production Requirements (BEFORE Play Store release) — USER ACTION

1. **Create a production OAuth client** in Google Cloud Console (phone browser works):
   - Go to APIs & Services > Credentials
   - Create OAuth client ID > Android
   - Package: `com.greninjaop.mailorganizer`
   - SHA-1: `5A:34:E6:F1:10:1B:73:62:01:B3:67:16:5B:06:8E:6A:E7:E6:CE:D5` (release, from above)

2. **Update the app** to use the production client ID for release builds.
   Currently `google_oauth_client_id` in `strings.xml` is the debug client.
   For production, add a separate `strings.xml` in `src/release/res/values/`
   overriding `google_oauth_client_id`. (Not done yet — needs the prod client ID.)

3. **OAuth consent screen**: Move from "Testing" to "Production" in
   Google Cloud Console. This requires:
   - App verification (sensitive/restricted scopes: gmail.readonly needs verification, gmail.modify needs a security assessment)
   - Privacy policy URL
   - Terms of service URL

### Scopes Requested

| Scope | Purpose | Phase |
|-------|---------|-------|
| `gmail.readonly` | Read Gmail messages for on-device organization | 3 |
| `gmail.modify` | Mark read/unread, archive (user-confirmed only) | 22 |
| `calendar.events` | Create calendar events (user-confirmed only) | 15 |
| `tasks` | Create tasks (user-confirmed only) | 16 |
| `openid`, `email`, `profile` | Basic account identity | 3 |

**Note**: `gmail.modify` is a restricted scope. Production use requires
Google verification with a security assessment.

## Release Signing

The release build uses R8 minification (`isMinifyEnabled = true`).
ProGuard rules are in `app/proguard-rules.pro`.

**IMPORTANT**: The release keystore must be kept secure and backed up.
Losing it means you cannot update the app on Play Store.

## Play Store Requirements Checklist

- [ ] Production OAuth client created with release SHA-1
- [ ] OAuth consent screen in Production mode
- [ ] Privacy policy URL (required for sensitive scopes)
- [ ] App icon (adaptive, all densities) — DONE (Phase: logo integration)
- [ ] Feature graphic (1024x500)
- [ ] Screenshots (phone, tablet)
- [ ] App description (short + full)
- [ ] Content rating questionnaire
- [ ] Data safety form (required for Gmail API):
  - Data collected: Email content (for on-device organization)
  - Data shared: None (all processing on-device)
  - Data retention: Until user deletes account
- [ ] Target API level 35 (current) — meets Play Store requirements

## Version

- versionCode: 1
- versionName: 1.0.0

Semantic versioning from this point forward.

## Known Limitations (Pre-Release)

1. **No physical device testing**: The sandbox has no emulator or
   physical device. On-device testing must be done by the user before
   Play Store submission.

2. **OAuth end-to-end**: The full PKCE flow is implemented and unit-tested
   (including against a local fake token endpoint), but the real Google
   sign-in must be verified on a real device with a real Google account.

3. **Gmail/Calendar/Tasks API verification**: API calls must be tested
   with a real Google account on-device after the Cloud Console setup
   (APIs enabled, test user added) is done.

## Next: Phase 30

Phase 30 is the final production hardening and release audit.
