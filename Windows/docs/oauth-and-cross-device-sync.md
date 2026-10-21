# Google OAuth (Windows vs Android) & cross-device pin/star sync

This document is for two audiences:

1. **Product / account owner (non-engineer)** — what you must configure in Google Cloud so Windows can sign in, and what pin vs star means across phone and PC.
2. **Engineering** — what the Windows codebase already has, what is still missing for live login, and an honest cross-device sync design (no invented cloud backend).

---

## Part A — For the account owner (Google Cloud + expectations)

### Short version

| Topic | Android today | Windows today | Across devices |
|-------|---------------|---------------|----------------|
| Sign-in | Live Google OAuth works (custom app redirect) | Contracts only — **not** live login yet | Same Google account can be used on both once Windows login is finished |
| Email **star** | Writes Gmail’s `STARRED` label | Will use the same Gmail label when sync + write are live | **Stars follow the Gmail account** if both apps sync |
| Company **pin** | Stored only on that phone (local DB) | Local product state (memory + DB schema); not a Gmail feature | **Pins do not sync** unless we later add an app-owned store |

**Pin ≠ star.** Pinning a company only reorders that company’s filter inside a category. Starring marks an individual email in Gmail.

---

### What you must do in Google Cloud Console

Windows does **not** use the Android OAuth client as-is. Android redirects to a private app scheme; Windows must use a **loopback** redirect (`http://127.0.0.1:…`). You either create a **Desktop** OAuth client or add the Windows redirect to an existing Desktop client in the **same** Google Cloud project that already powers Android.

#### Checklist

1. Open [Google Cloud Console](https://console.cloud.google.com/) → select the Mailstack / Mail Organizer project (the one that already has the Android client).
2. **APIs & Services → Library** → enable **Gmail API** (and Calendar / Tasks APIs if those features remain in scope).
3. **APIs & Services → Credentials → Create credentials → OAuth client ID**
   - Application type: **Desktop app** (recommended for Windows installed apps), **or**
   - Edit an existing **Desktop** client and add the redirect below.
4. **Authorized redirect URI** (exact form Windows expects):
   - `http://127.0.0.1:<port>/oauth2redirect`
   - `<port>` must match whatever the Windows app binds for the one-shot OAuth listener (engineering will pick a fixed or ephemeral port; the URI registered in Console must match).
   - Placeholder used in code today when env is unset: `http://127.0.0.1:1/oauth2redirect` — **replace with the real listener port before live login**; do not leave port `1` in production.
5. Copy the Desktop client’s **Client ID** (looks like `….apps.googleusercontent.com`).
   - Windows reads it from the environment variable **`MAILSTACK_OAUTH_CLIENT_ID`** (never committed to source).
   - Optional override: **`MAILSTACK_OAUTH_REDIRECT`** if the redirect URI is not the default above.
6. **OAuth consent screen**
   - If the app is still in **Testing**: add every Google account that will sign in under **Test users**.
   - Testing-mode refresh tokens expire after **7 days** (Google policy) — expect re-consent during development.
7. **Scopes** the product requests (must be listed on the consent screen):

   | Scope | Purpose | When requested |
   |-------|---------|----------------|
   | `https://www.googleapis.com/auth/gmail.readonly` | Read mail for sync | Base sign-in |
   | `https://www.googleapis.com/auth/calendar.events` | Calendar actions | Base sign-in |
   | `https://www.googleapis.com/auth/tasks` | Tasks actions | Base sign-in |
   | `openid` | Identity | Base sign-in |
   | `email` | Account email | Base sign-in |
   | `profile` | Display name / profile | Base sign-in |
   | `https://www.googleapis.com/auth/gmail.modify` | Star / archive / label writes | **Only when the user performs a write** (incremental; not in the first consent set) |

8. Do **not** put a client secret into the Windows app for the public Desktop + PKCE flow the product uses. Client ID + PKCE is enough for this installed-app pattern.

#### Android vs Windows (what stays different)

| | Android (already live) | Windows (target) |
|--|------------------------|------------------|
| Client type | **Android** OAuth client (package + SHA-1) | **Desktop** OAuth client |
| Client ID location | App resource `google_oauth_client_id` | Env `MAILSTACK_OAUTH_CLIENT_ID` |
| Redirect URI | `com.greninjaop.mailorganizer:/oauth2redirect` | `http://127.0.0.1:<port>/oauth2redirect` |
| Browser | System / Chrome Custom Tab → custom scheme | System browser → loopback HTTP |
| Same Google Cloud project? | Yes — reuse the project; add Desktop client / redirect | Same |

You can keep using the existing Android client for the phone app. Windows needs its own Desktop client ID (or shared Desktop client) with the loopback redirect registered.

---

### Cross-device: stars and company pins

#### Email star (Gmail `STARRED`)

- On Android, starring calls the Gmail API and adds/removes the system label **`STARRED`** (requires `gmail.modify`, granted only when the user does a write).
- That label lives on the **Gmail account**, not inside Mailstack’s private cloud.
- **If** Windows later syncs the same account and also applies/reads `STARRED`, stars appear on both devices after sync — the same way Gmail web and the Android app stay aligned.
- Until Windows has live Gmail sync + write, starring on the phone will not show on Windows (and the reverse).

#### Company pin (product UI only)

- Company pin is **not** a Gmail label. It only sorts companies inside a category filter.
- **Android:** pins are stored in the phone’s local SQLite (`companies.pinned`).
- **Windows:** pin logic exists as an in-memory `CompanyPinStore`; a `company_pins` table is defined in the local DB schema, but there is **no** Mailstack cloud service in this repo that syncs pins between devices.
- **Honest current state:** pinning a company on Android does **not** pin it on Windows, and vice versa.
- **Recommended designs (pick one later; do not invent a backend that does not exist):**
  1. **Account-local only (simplest):** pins stay per device / per install. Document that to users. Acceptable for v1.
  2. **App-owned sync later:** only if/when the product adds a real app backend or sync channel (e.g. encrypted account-scoped preference store). Until that exists, do not claim cross-device pin sync.

There is no Firebase / product cloud pin API in this repository today.

---

## Part B — For engineering (accurate code state)

### What already exists on Windows / desktop-core

| Piece | Location | Status |
|-------|----------|--------|
| PKCE S256 (`generate_pkce`) | `desktop-core/src/oauth.rs` | Done (contracts + unit tests) |
| Auth URL builder (`prompt=consent`, `access_type=offline`, `include_granted_scopes=true`) | same | Done |
| Base scopes + incremental `gmail.modify` flag | `default_scopes()`, `AuthUrlRequest.include_gmail_modify` | Done |
| Client config from env | `OAuthClientConfig::from_env` → `MAILSTACK_OAUTH_CLIENT_ID`, optional `MAILSTACK_OAUTH_REDIRECT` | Done |
| Default redirect placeholder | `http://127.0.0.1:1/oauth2redirect` | Placeholder only |
| Pending PKCE consume-once | `PendingPkceStore` | Done |
| System browser opener (http/https only) | `Windows/src/adapters/browser.rs` | Done |
| Credential Manager adapter | `Windows/src/adapters/credential_windows.rs` (`CredWriteW` / `CredReadW` / `CredDeleteW`, memory fallback) | Adapter present; not yet driven by a live OAuth exchange |
| Gmail API **contracts** + mock | `desktop-core/src/gmail.rs` | Read path contracts only; **no** live HTTP |
| `starred` on message models / sync mapping | `gmail.rs`, `mailbox.rs`, `persistence.rs` | Field exists; no `modifyLabels` / star write API yet |
| Company pin ≠ star | `desktop-core/src/company.rs`, phase-15 docs | Done as product rule |
| Local `company_pins` schema | `desktop-core/src/persistence.rs` migration | Schema only; pin store itself is still in-memory |

### What is missing for live Windows login

| Gap | Notes |
|-----|--------|
| Google Cloud **Desktop** client + redirect allow-list | User/console work (Part A) |
| Loopback HTTP listener | Bind `127.0.0.1:<port>`, receive `?code=&state=`, match pending PKCE, then shut down |
| Token exchange / refresh HTTP | POST to `https://oauth2.googleapis.com/token` with `code`, `code_verifier`, `client_id`, `redirect_uri` — **no `reqwest` (or other HTTP client) in `desktop-core` / Windows crates today** |
| Wire tokens into `WindowsCredentialStore` | Persist `refresh_token` / `access_token` under `Mailstack/<accountId>/…`; clear on logout |
| Live Gmail HTTP client | Implement `GmailApi` with bearer auth; map throttle / daily / scopes / auth errors |
| Star / label writes | Android already has `setStarred` → `STARRED`; Windows needs equivalent + `gmail.modify` upgrade path |
| End-to-end UI path | Connect account → open auth URL → handle redirect → store tokens → first sync |

Phase docs already call this out: Phase 09 blockers = live consent + loopback; Phase 10 blockers = live reqwest Gmail client.

### Android reference (do not change `android/` from this doc)

| Item | Value in repo |
|------|----------------|
| Redirect | `com.greninjaop.mailorganizer:/oauth2redirect` (`GoogleAuthRepository.REDIRECT_URI`) |
| Manifest | Scheme-only intent-filter for `com.greninjaop.mailorganizer` |
| Client ID | `R.string.google_oauth_client_id` (Android-type client) |
| Base scopes | Same list as `desktop-core::oauth::default_scopes()` |
| Star write | `GmailApiClient.setStarred` → label `STARRED` via `gmail.modify` |
| Company pin | Room `companies.pinned` — device-local |

### Eng build order for live login (recommended)

1. **Console:** Desktop client + exact `http://127.0.0.1:<port>/oauth2redirect`; set `MAILSTACK_OAUTH_CLIENT_ID` (and redirect env if needed) for local runs.
2. **Loopback listener** on Windows: pick port strategy (fixed documented port vs ephemeral + dynamic redirect registration); consume-once with `PendingPkceStore`.
3. **HTTP token client** (e.g. `reqwest`): authorize code exchange + refresh; never log tokens; store via `WindowsCredentialStore`.
4. **Live `GmailApi`**: profile / list / get with pacing; then incremental `gmail.modify` for star/archive.
5. **Pins:** either persist `CompanyPinStore` to local `company_pins` (still device-local) or defer; **do not** claim Android↔Windows pin sync without an app-owned sync channel.

### Quick “what works when both apps sync”

```
Star on Android ──(Gmail API STARRED)──► Gmail cloud ──(sync)──► Windows (once live)
Pin on Android  ──(local SQLite only)──► ✗ no shared store ──► Windows pin unchanged
```

---

## Related files

- `desktop-core/src/oauth.rs` — PKCE + scopes + env config  
- `desktop-core/src/company.rs` — company pin store  
- `desktop-core/src/gmail.rs` — Gmail contracts (mock only)  
- `desktop-core/src/persistence.rs` — `company_pins` / `is_starred` schema  
- `Windows/src/adapters/browser.rs` — system browser for OAuth URL  
- `Windows/src/adapters/credential_windows.rs` — Credential Manager  
- `Windows/docs/phase-08-credentials.md`, `phase-09-oauth.md`, `phase-10-gmail.md`, `phase-15-categories-company.md`  
- Android (reference only): `GoogleAuthRepository.kt`, `GmailApiClient.kt`, `CompanyRecord.kt`
