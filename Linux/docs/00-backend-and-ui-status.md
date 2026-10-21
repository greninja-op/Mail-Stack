# Linux backend and UI status

Audit date: 2026-10-21. Read from the files, on a Windows machine. No test was run. No Linux desktop was launched. No Gmail account was connected. No OAuth flow succeeded. No sync percentage is reported.

## UI

The UI has not been touched in the 2026-10-21 work.

There is no Linux product UI in the repo. The earlier scaffold was removed on 2026-10-20 because it was bad.

The UI still needs to be built. Do not treat any leftover shell as acceptable. When it is built later, it must match the current Android phone app. The current and previous Linux UI is not a base to keep.

This document does not design or start that UI.

Phase 00 of the Linux execution plan asks for an architecture audit before implementation. Phase 06 asks for a narrow boundary: the Linux host depends on the shared core, and the core does not depend on the UI. That UI boundary does not exist yet, because there is no Linux product UI.

## What exists for Linux right now

The shared Rust crate is `desktop-core` (`desktop-core/src/lib.rs`, package `desktop-core`, library name `desktop_core`).

`lib.rs` describes it as platform-neutral contracts. The crate comment says macOS and Linux plans reuse it. `desktop-core/Cargo.toml` describes it as reusable by Windows and macOS. `#![forbid(unsafe_code)]` is set.

Root `Cargo.toml` workspace members, as read for this audit, are `desktop-core` and `Windows`. Linux is not a workspace member in that file. This audit does not edit it.

Linux-specific adapters are being added under `Linux/crates` by the other four agents. This document does not invent their APIs.

`desktop-core` dependencies, from its `Cargo.toml`: `serde`, `thiserror`, `sha2`, `base64`, `url`, `percent-encoding`, `uuid`, `regex`. There is no SQLite crate and no HTTP client crate.

## What `desktop-core` exports

Public modules in `desktop-core/src/lib.rs`:

`account`, `actions`, `ai`, `automation`, `background`, `cancel`, `capability`, `category`, `classification`, `company`, `conversation`, `error`, `gmail`, `html`, `logo`, `mailbox`, `multi_account`, `oauth`, `pacing`, `persistence`, `platform`, `priority`, `privacy`, `rules`, `search`, `sync`, `sync_engine`, `temporal`.

Re-exports from the same file:

- Account scope: `AccountId`, `AccountScoped`.
- Actions: `ActionEngine`, `ActionProposal`, `ActionStatus`, `ActionType`, `CalendarService`, `DisconnectedCalendar`, `DisconnectedTasks`, `TasksService`.
- AI (off by default): `ai_suggestion_badge`, `minimize_for_ai`, `summary_advisory_notice`, `validate_ai_output`, `wrap_prompt_boundary`, `AiCapability`, `AiRouter`, `AiSettings`, `AiStructuredOutput`, `DataMinimizer`, `MinimizedAiInput`, `MAX_BODY_CHARS`, `MAX_MODEL_CONFIDENCE`.
- Automation: `reject_email_authored_automation`, `AutomationActionKind`, `AutomationEngine`, `AutomationProposal`, `AutomationRule`, `AutomationTrigger`, `CIRCUIT_BREAKER_THRESHOLD`.
- Background: `build_notification`, `clamp_periodic_interval`, `should_run_lifecycle_work`, `user_message_for_failure`, `BackgroundScheduler`, `BackgroundWorkKind`, `BackgroundWorkRequest`, `Connectivity`, `FailureClass`, `MemoryBackgroundScheduler`, `NotificationDeduper`, `MIN_PERIODIC_INTERVAL`, `STALENESS_THRESHOLD`.
- Cancel: `CancelToken`, `Cancelled`.
- Capability: `base_sign_in_capabilities`, `AccountCapabilities`, `CapabilityRegistry`, `IntegrationCapability`, `PerformanceProfile`, `PerformanceTier`.
- Categories: `CategoryGroupId`, `CategoryGroupKind`, `CategoryGroupModel`.
- Classification: `bind_result`, `classify_deterministic`, `ClassificationResult`, `ClassificationSignals`.
- Companies: `company_key_from_sender`, `filter_keys_in_category`, `sort_companies_for_filter`, `CompanyKey`, `CompanyPinStore`, `CompanyRecord`.
- Conversation: `assess_conversation`, `is_follow_up_candidate`, `ConversationAssessment`, `ConversationSignals`, `ConversationState`, `FOLLOW_UP_CANDIDATE_DAYS`, `STALE_THRESHOLD_DAYS`.
- Errors: `CoreError`, `CoreResult`.
- Gmail adapter contract: `classify_provider_error`, `get_message_never_skip`, `GmailApi`, `GmailApiErrorKind`, `GmailMessage`, `GmailMessageRef`, `GmailProfile`, `ListPage`, `MockGmail`.
- HTML: `SanitizedHtml`, `SanitizePolicy`.
- Logo pipeline input: `LogoPipelineInput`, `SenderDomain`.
- Mailbox presentation: `AttachmentMeta`, `MessageId`, `MessagePresentation`, `ThreadId`, `ThreadPresentation`.
- Multi-account: `assert_may_sync`, `unified_inbox_join`, `ReceivingAccountIndicator`, `UnifiedInboxRow`.
- OAuth contract: `build_auth_url`, `default_scopes`, `generate_pkce`, `AuthUrlRequest`, `OAuthClientConfig`, `PendingPkce`, `PendingPkceStore`, `PkcePair`, `SCOPE_CALENDAR_EVENTS`, `SCOPE_EMAIL`, `SCOPE_GMAIL_MODIFY`, `SCOPE_GMAIL_READONLY`, `SCOPE_OPENID`, `SCOPE_PROFILE`, `SCOPE_TASKS`.
- Pacing: `CursorState`, `PaceDecision`, `RequestPacer`, `ThrottlePolicy`, `TokenBucketPacer`, `DEFAULT_REQUESTS_PER_SECOND`.
- Persistence contract: `migrations`, `schema_forbids_tokens`, `AccountRecord`, `LocalStore`, `MemoryStore`, `Migration`, `StoredMessage`.
- Platform traits: `BrowserOpen`, `CredentialStore`, `MemoryCredentialStore`, `NotificationRequest`, `NotificationService`, `SecureSecret`.
- Priority: `assess_action_required`, `assess_priority`, `ActionRequiredAssessment`, `MailListBadgeFlags`, `PriorityAssessment`, `PriorityLevel`, `PrioritySignals`.
- Privacy: `data_inventory`, `launch_safe_link`, `sanitize_log_line`, `validate_external_url`, `DataInventoryEntry`, `LocalPurgePlan`, `PrivacyCenterModel`, `PrivacyCenterTab`, `UrlSafety`.
- Rules: `find_conflicts`, `first_matching_rule`, `resolve_category`, `resolve_with_optional_ai`, `CorrectionField`, `CorrectionStore`, `DecisionSource`, `EffectiveDecision`, `RuleAction`, `RuleCondition`, `UserCorrection`, `UserRule`.
- Search: `build_fts_match`, `escape_fts_term`, `SearchDebouncer`, `SearchQuery`, `SEARCH_DEBOUNCE`.
- Sync progress types: `SyncPhase`, `SyncProgress`, `SyncProgressSnapshot`.
- Sync engine: `classify_sync_error`, `sync_error_user_message`, `ImportFlowState`, `SyncCoordinator`, `SyncErrorClass`.
- Temporal: `extract_temporal`, `TemporalConfidence`, `TemporalExtractionInput`, `TemporalItem`, `TemporalItemType`.

`AiSettings` defaults to `enabled: false` with an empty capability list. `ai.rs` says the router cannot authorize actions. Deterministic classification stays in charge.

`SyncProgress` and `SyncProgressSnapshot` are types. They are not a measured sync of a mailbox.

## What is still missing for a Linux host

These three gaps are in the crate as it stands. They are not filled by the in-memory stand-ins.

### Real Secret Service

Not present.

`platform.rs` defines the `CredentialStore` trait (`store`, `load`, `delete`) and `SecureSecret`. The only implementation in the crate is `MemoryCredentialStore`, a process-local map. Its own comment says it is for contract tests only, not for production. The module comment still calls these Windows and macOS stubs. There is no Secret Service client, no libsecret link, and no keyring call in `desktop-core`.

### Real SQLite file

Not present.

`persistence.rs` defines the `LocalStore` trait and `MemoryStore`. `MemoryStore` keeps accounts, messages, and cursors in memory. `migrations()` returns four SQL strings (accounts and cursors, messages, classification / rules / company pins, and an FTS5 virtual table). The module says those strings are for a later SQLite adapter and are not executed by the in-memory store. `schema_forbids_tokens` checks that credential columns stay out of that SQL. `desktop-core/Cargo.toml` does not depend on `rusqlite` or any other SQLite crate. Nothing in this crate opens a database file.

### Live HTTP

Not present.

`gmail.rs` defines the `GmailApi` trait (`get_profile`, `list_messages`, `get_message`). The only implementation in the crate is `MockGmail`, documented as the mock used by sync-engine unit tests. `classify_provider_error` classifies a status code and body the caller already has. It does not send a request.

`oauth.rs` builds a Google authorization URL (`build_auth_url`), makes a PKCE S256 pair (`generate_pkce`), and can read `MAILSTACK_OAUTH_CLIENT_ID` plus optional `MAILSTACK_OAUTH_REDIRECT` from the environment. It does not exchange an authorization code, refresh a token, or call Google. The crate has no `reqwest`, `hyper`, or `ureq` dependency.

`MockGmail` and `MemoryStore` must not be described as a connected mailbox.

## Honest limits of this audit

- Source was read. Behavior was not executed.
- This machine is Windows. A Linux desktop launch is not claimed.
- No Gmail data was fetched. No OAuth success is claimed. No sync percentage is claimed.
- `Linux/crates` APIs are out of scope here. Do not invent them.
