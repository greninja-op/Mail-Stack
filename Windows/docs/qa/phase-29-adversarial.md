# Phase 29 — Security, Adversarial, Migration & Recovery QA

**Date:** 2026-10-20  
**Rule:** No fabricated live runs. Unit tests added in source; **execution blocked** by missing `link.exe`.

## 1. HTML / URL / prompt-injection attacks

| Attack | Expected defense | Status | Notes |
|--------|------------------|--------|-------|
| `<script>` in mail body | Stripped | **CODE + unit test** | `html::tests::strips_scripts` |
| Remote `<img src=https://…>` | Blocked, no placeholder line, never fetched | **CODE + unit test** | `blocks_remote_images_without_placeholder` |
| `onerror` / `onclick` handlers | Stripped | **CODE + unit test** | Added Phase 29 |
| `javascript:` href | Neutralized | **CODE + unit test** | Added Phase 29 |
| `<!DOCTYPE>` / `<?xml?>` as visible text | Removed | **CODE + unit test** | Added Phase 29 |
| Prompt-injection text in body | Treated as data; scripts still stripped | **CODE + unit test** | Sanitizer does not “obey” instructions |
| AI prompt boundary | Markers wrap untrusted content | **CODE + unit test** | `ai::wrap_prompt_boundary` |
| `javascript:` / `file:` / `data:` external open | Rejected | **CODE + unit test** | `privacy::validate_external_url`, `WindowsBrowser` |
| Live render of malicious HTML in GUI | **NOT RUN** | Shell not launched |

## 2. Cross-account attacks

| Attack | Expected defense | Status |
|--------|------------------|--------|
| Persist message with wrong `account_id` | `AccountIsolationViolation`; cursor not advanced | **CODE** (`persistence::account_isolation_on_persist`) |
| Unified join mixing ownership | `require_account` per row | **CODE** (`multi_account::cross_account_message_rejected_in_unified_join`) |
| Credential store key collision across accounts | Keys namespaced by account | **CODE** (`MemoryCredentialStore` / WinCred target prefix `Mailstack/{account}/`) |
| Live UI switch accounts leak | **NOT RUN** | |

## 3. Secrets audit (static, observed 2026-10-20)

| Check | Result |
|-------|--------|
| Hardcoded `ya29.` / Google API keys / PEM / PFX in `desktop-core` + `Windows` sources | **No production secrets.** Only test fixture string `ya29.secret` inside `privacy.rs` unit test. |
| `.env` / `.pem` / `.pfx` under Windows/desktop-core | **None found** (android excluded from scan scope per isolation) |
| OAuth client secret in repo | **Not present** — `MAILSTACK_OAUTH_CLIENT_ID` from env |
| DB migration SQL contains token columns | **Forbidden by design** — `schema_forbids_tokens` + migration tests |
| Log redaction | **CODE** — `privacy::sanitize_log_line`, `Windows/src/redacting_log.rs` |
| Secrets in UI screenshots | **NOT RUN** (no screenshots) |

## 4. Destructive actions / automation loops

| Case | Status |
|------|--------|
| Automation proposals always require confirmation | **CODE** |
| Email body cannot author automation | **CODE** (`reject_email_authored_automation`) |
| Circuit breaker after repeated failures | **CODE** |
| AI output forbidden verbs / no SECURITY downgrade | **CODE** |
| Live destructive Gmail write | **NOT RUN** |

## 5. Migration / recovery / integrity

| Case | Status |
|------|--------|
| Ordered migrations, token-free SQL | **CODE** |
| Cursor saved only after page persist | **CODE** |
| Stale cursor / partial sync contracts | **CODE** (`sync_engine`) |
| DB corruption / process death / sleep-wake recovery | **NOT RUN** (no SQLite on-disk adapter exercised) |
| Account removal purge (local only; Gmail untouched) | **CODE** inventory asserts `remote_gmail_modified_on_purge == false` |
| Interrupted writes / offline conflicts | **NOT RUN** live |

## 6. Unit tests added / extended this phase

| Location | What |
|----------|------|
| `desktop-core/src/html.rs` | onerror/onclick strip, javascript: neutralize, doctype/PI, prompt-injection data-only |
| `desktop-core/src/privacy.rs` | file/data/content/vbscript rejection; secrets inventory wording |
| `Windows/src/adapters/browser.rs` | reject javascript:/data: opens |
| `desktop-core/src/lib.rs` | Wired all phase modules so tests can compile when MSVC available |

**Execution:** `cargo test -p desktop-core` attempted → **FAILED** (`link.exe` not found). Do not treat tests as green on this PC.

## Phase 29 outcome

Adversarial checklist + unit tests **landed in source**. Live adversarial GUI / migration / recovery **BLOCKED** by toolchain and missing signed runtime.
