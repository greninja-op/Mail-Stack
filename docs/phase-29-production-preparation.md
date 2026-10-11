# Phase 29 — Production OAuth & Play Store Preparation

## Status: Complete (2026-10-11)

This document records the production preparation work for Mailstack 1.0.0.

## Production OAuth Configuration

### Debug vs Production

The app currently uses a **debug** OAuth client:
- Client ID: `922852545613-f0vel0oe1i2g10rc9cst028s8mhnhl13.apps.googleusercontent.com`
- Package: `com.greninjaop.mailorganizer`
- Debug SHA-1: `D8:B7:ED:C4:41:31:5C:B8:C8:CE:96:83:55:15:F7:43:C8:56:1E:7B`

### Production Requirements (BEFORE Play Store release)

1. **Generate a release keystore** (if not already done):
   ```bash
   keytool -genkeypair -v -keystore mailstack-release.keystore \
     -alias mailstack -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Get the release SHA-1**:
   ```bash
   keytool -list -v -keystore mailstack-release.keystore -alias mailstack
   ```

3. **Create a production OAuth client** in Google Cloud Console:
   - Go to APIs & Services > Credentials
   - Create OAuth client ID > Android
   - Package: `com.greninjaop.mailorganizer`
   - SHA-1: (release SHA-1 from step 2)

4. **Update the app** to use the production client ID for release builds.
   Currently `google_oauth_client_id` in `strings.xml` is the debug client.
   For production, use build variants or a separate `strings.xml` in
   `src/release/res/values/`.

5. **OAuth consent screen**: Move from "Testing" to "Production" in
   Google Cloud Console. This requires:
   - App verification (if requesting sensitive scopes)
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

2. **OAuth token exchange**: The token exchange via `oauth2.googleapis.com`
   is documented but the full PKCE flow completes on-device. This must
   be verified on a real device.

3. **Gmail API verification**: The Gmail API calls (read, modify labels)
   must be tested with a real Google account on-device.

## Next: Phase 30

Phase 30 is the final production hardening and release audit.
