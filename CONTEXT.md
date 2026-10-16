# CONTEXT.md — Mail Organizer (canonical continuity file)

## How to use this file
1. **READ this file fully before starting ANY work** on this repo (phase, fix, doc, push — anything). If the user ever says "read CONTEXT.md", stop everything and re-read it before continuing.
2. **UPDATE it after every prompt/request is handled**: update `Current build state`, append dated entries to `Decision log`, keep `Standing rules` current.
3. Never delete history — append and update. Keep entries short and factual.
4. When updating: fetch the REMOTE version of this file first and merge — never push a stale local copy over a newer remote one. (A stale push once wiped Phase 5's entry; don't repeat that.)
5. Purpose: this file alone must let any agent reconstruct every conversation, decision, and rule — even with zero prior context (e.g. user switches accounts/chats).

## Standing rules (non-negotiable)
- **Branch:** `main`. Push DIRECTLY to main — no pull requests, no separate branches.
- **Push as you go:** push each completed phase immediately; do NOT wait for approval taps. Standing user authorization (2026-10-09).
- **One push call per phase** = at most one approval prompt per phase. (Batch a phase's files into a single `push_files` call.)
- **Phases run one by one, continuously**, without asking. Next phase starts when the previous is pushed.
- **Deferred to the very end** (user has no computer access for keys): Phase 3 (Google OAuth & Gmail connection), Phases 15/16 (Calendar/Tasks), Phase 22 (Gmail write), Phase 29 (production OAuth/Play Store). Build clean seams/interfaces for them; never fake them.
- **Reports:** after EVERY phase, post a completion report in the side chat "Mail Organizer — build progress" (chat_id cc10335a-62bf-4626-becc-abbd55b0613c) — what was added/changed, what was verified, honest caveats. During long phases, post periodic progress updates too (user asked after a 2.5h silent Phase 0).
- **Honesty:** never fake completion — only real, verified work counts. Never put secrets (API keys, tokens, credentials) in the repo. Secret-audit every phase.
- **Speed:** run at FULL SPEED, but only within the free weekly quota.
- **Quota guardrail (UPDATED 2026-10-09 ~22:48 IST):** user EXPLICITLY APPROVED using the 1B additional-token pool. If free weekly quota crosses 90-100%, DO NOT STOP — continue working, drawing from the 1B pool. No per-incident approval needed (standing approval). Free quota was 87% used on 2026-10-09 ~22:47 IST; resets Oct 15 9:36 PM IST; 1B pool untouched at 0%.
- **Approval prompts** come from the app's runtime safety system, not the agent — the user only ever sees an "allow" button (no always-allow option). The agent cannot disable them; never re-ask from its side.

## User
- **Arjun Sabu** (GitHub: `greninja-op`, India). Primary contact for this build.
- Writes casually in English (sometimes Spanish); keep reports in **plain language, no jargon**.
- This side chat is the dedicated build-progress thread — all phase reports and push confirmations go here.

## Project
- **Mail Organizer** — Android-first Gmail client. Kotlin + Jetpack Compose + Material 3. Gmail is the source of truth; the app adds local organization, intelligence, search, prioritization.
- **Repo:** `greninja-op/mail-organizer`. Layout: Android project under `android/`; repo root keeps the execution-plan README (`README.md`, 808 bytes — do NOT overwrite) and this file; plan docs under `execution plan/`.
- **App ID:** `com.greninjaop.mailorganizer`. **Toolchain:** AGP 8.13.2, Kotlin 2.3.21, Compose BOM 2026.06.01, compileSdk/targetSdk 35 (35 kept deliberately over 36 — reason in version catalog), minSdk 26.
- **Architecture notes:** account isolation is a first-class DB boundary (`AccountId`, FK-enforced); privacy-aware logging (never email bodies/tokens); no tokens/passwords in DB by schema design; email treated as untrusted input (HTML sanitized).

## Current build state (2026-10-12)
- **App version now 1.0.1** (versionCode 2). Latest delivered APK: `Mailstack-1.0.1.apk` (debug, stable cert SHA-1 `07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05`). Contains: always-visible "Connect Google account" button, disconnect-crash fix, FTS missing-table guards, Connect-screen theme background hardening. OAuth Google-side blocker cleared (custom URI scheme enabled by user 2026-10-12); live sign-in still needs the user's phone + test-user listing.
- **Phase 0** — Project Audit & Development Foundation: COMPLETE, pushed.
- **Phase 1** — Android Application Foundation (design tokens, Material 3 light/dark, 10-route nav, foundation screen, zero-permission manifest): COMPLETE, pushed. 24/24 tests.
- **Phase 2** — Local Data Architecture (Room v2, 12 tables, v1→v2 migration, 5 repositories): COMPLETE, pushed. 47/47 tests.
- **Phase 3** — Google OAuth & Gmail Connection: **DEFERRED** (see Standing rules).
- **Phase 4** — Gmail Synchronization Engine (sync coordinator, cursors, retry policy, scheduler seam): COMPLETE, pushed. 92/92 tests.
- **Phase 5** — Email Data Model & Parsing (`core/email/`: canonical model, total MIME parser, HTML sanitizer, v2→v3 migration): COMPLETE, pushed (2 commits). 38/38 tests (caught 2 real bugs, fixed).
- **Phase 6** — Core Inbox & Email Viewer (`ui/mail/`: MailScreen + drawer + lazy lists, ThreadScreen oldest-first, MessageCard viewer, no-WebView HTML-as-native-text, fixture data clearly labeled): COMPLETE, pushed (3 commits). 44/44 unit tests; `:app:compileDebugKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked.
- **Phase 7** — Email Classification Engine (deterministic local classifier: `core/classify/` — 30 rules, 10 categories + UNCLASSIFIED, honest confidence-as-rule-strength; `domain/classify/` use cases with persist + idempotency + user-override safety; Promotional/Social/Spam destinations now show real classified mail; category chips + "Why this category?" explanations in UI): COMPLETE, pushed. 140/140 unit tests pass (76 core classifier + 12 use-case + 52 UI); `:app:compileDebugKotlin` BUILD SUCCESSFUL; secret audit clean. 10,000 emails classify in ~933ms (~0.09ms/msg). No device — device checks blocked, never faked.
- **Phase 8** — Company & Sender Intelligence (company detection from sender domains, sender profiles with honest recurring-sender counts, company filter chips inside Promotional/Social/Spam, company pinning, classifier v2 with IMPORTANT_RECURRING_SENDER rule): COMPLETE, pushed. 214/214 unit tests pass (37 new Phase 8 + 177 regression); `:app:compileDebugKotlin` BUILD SUCCESSFUL; MIGRATION_3_4 SQL validated on real SQLite; secret audit clean. No device — device checks blocked, never faked.
- **Phase 9** — Priority & Action-Required Engine (deterministic priority engine v1: `core/priority/` 15 rules, LOW/NORMAL/HIGH/CRITICAL independent from category; `domain/priority/` use cases with manual-override safety; action-required filter chip + priority badges + "why this priority?" in UI): COMPLETE, pushed. 252/252 unit tests pass (38 new Phase 9 + 214 regression); `:app:compileDebugKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked.
- **Phase 10** — Search & Local Indexing (on-device FTS5 `messages_fts` + `search_index_meta`, schema v4→v5; `core/search/` parser/ranking/highlight; `data/repository/SearchRepository` with parameterized filters + account isolation; `domain/search/` index use case + maintenance seam; write-path hooks in RoomMailRepository/RoomAccountRepository/CompanyIntelligenceUseCase; `ui/search/` screen + 300ms-debounce ViewModel + SEARCH nav route; IntelligenceRepository.getClassifications batch): COMPLETE, pushed. 39 new tests; 338/338 pure-JVM tests pass; FTS5 DDL/behavior validated on real SQLite; `:app:compileDebugKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked. No OAuth.
- **Phase 11** — Dashboard & Information Architecture (`ui/navigation/`: MoNavBar + MoAppTopBar; `ui/home/`: dashboard hierarchy account→attention→priority→recent→categories→companies; `ui/categories/`, `ui/companies/`, `ui/actions/` (view-only), `ui/settings/` (Settings/Accounts/Privacy/Integrations/Appearance, honest); `ClassificationDao.countByCategory` + `MessageDao.companyMessageCounts` GROUP BY; detail routes with validated args): COMPLETE, pushed. 15 new ViewModel tests; 354/367 pass (13 env-only failures unchanged); `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; GROUP BY SQL validated on real SQLite; secret audit clean. No device — device checks blocked, never faked. No OAuth.
- **Phase 12** — Rules & User Corrections (`core/rules/`: RuleEngine v1 deterministic evaluator + hand-rolled JSON codecs; `domain/rules/`: ApplyRulesUseCase effective pipeline (correction > rule > deterministic base), RecordCorrectionUseCase (message/sender/domain/company scopes + undo), RuleManagementUseCase (CRUD/preview/conflicts/reorder); schema v5→v6 additive MIGRATION_5_6; `ui/rules/`: correction bottom sheet with scope picker, rules list with conflicts banner, rule editor with live preview; ThreadScreen "Correct" affordance + "Set by you" markers; Settings → Rules route): COMPLETE, pushed. 68 unit tests pass (25 core + 17 domain + 26 regression); `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; MIGRATION_5_6 SQL validated on real SQLite; secret audit clean. No device — device checks blocked, never faked. No OAuth.
- **Phase 13** — Meeting & Deadline Extraction (`core/temporal/` + `domain/temporal/`: 14 item types, bounded regexes, reference time = message received, ambiguity policy, idempotent, reused extracted-items table — no migration): COMPLETE, pushed (6 commits). 50/50 new tests; full suite 454/456 (2 pre-existing Robolectric env failures); compile + test-compile BUILD SUCCESSFUL; 3 real bugs caught and fixed; secret audit clean. No device.
- **Phase 14** — Action Cards & Action Engine (`core/actions/`: ActionCandidateGenerator v1 (10 documented rules; deterministic dedup id `messageId|actionType|targetKey`) + ActionSafety (separate validation layer; missing-info → degraded review cards; confidence-capped urgency); `domain/actions/`: GenerateActionsUseCase (bounded incremental, thread dedup, stale-version replacement, overdue-expiry pass), ReviewActionUseCase (review/dismiss/complete/confirm → RecordedInternal/ExternalNotConnected/Executed/Failed), ActionExecutor interface + empty registry (confirming external proposals honestly reports "not connected", never fake success), ActionPayloadJson; schema v6→v7 additive (`MIGRATION_6_7` extends action_items: threadId/title/description/urgency/source/status/externalEffect/payloadJson/version/updatedAtEpochMs; status backfilled from legacy booleans); `ui/actions/`: reusable ActionCard + confirmation dialog + reworked Actions destination; generation hooked into MailViewModel background pipeline): COMPLETE, pushed. 46 new unit tests (12 generator + 7 safety + 8 generate + 9 review + 3 payload + 6 viewmodel); 498/498 full suite pass via direct JUnitCore (7 Robolectric classes excluded — pre-existing sandbox limitation); `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; real bugs caught+fixed: duplicate ExternalEffect enum, MoResult/MoError wrong package, migration threadId backfill NULL crash (COALESCE fix, validated on real SQLite); secret audit clean. No device — device checks blocked, never faked. No OAuth. No APK.
- **Phase 17** — Integration Manager (`core/integrations/`: IntegrationId/IntegrationStatus/IntegrationCapability/PermissionDescription/IntegrationSnapshot (usableCapabilities gated on CONNECTED)/IntegrationError/RetryDecision/IntegrationEvent + IntegrationAdapter contract + pure IntegrationPolicies (error normalization + retry semantics); `domain/integrations/`: IntegrationManager (adapter registry, account-scoped snapshots, isCapable capability queries for Action Engine, integrationForAction routing MEETING→Calendar/REMINDER→Tasks/DEADLINE unmapped, safe connect/disconnect, refreshAll health check, handleAccountRemoved cleanup boundary, SharedFlow events); `data/integrations/`: GmailIntegrationAdapter on Phase 4's fail-closed seam (never CONNECTED without OAuth; AUTH_REQUIRED naming Phase 3) + Calendar/Tasks as honest UNAVAILABLE deferred adapters; schema v7→v8 additive (`MIGRATION_7_8` creates integration_states — metadata only, never credentials); `ui/integrations/`: list + detail screens on the manager (status pills, offline banner, capability usable-vs-declared distinction, permissions with purposes, Connect/Disconnect with confirmation); ReviewActionUseCase enriched with the manager's honest per-integration reason): COMPLETE, pushed. 33 new unit tests (8 policies + 10 manager + 10 adapters + 4 viewmodel + 1 scratch removed); 535/535 pure-JVM tests pass via direct JUnitCore (6 Robolectric classes blocked — pre-existing sandbox limitation); `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; MIGRATION_7_8 SQL validated on real SQLite; real bugs caught+fixed: Room nullable-PK rejection ("" sentinel for no-account), missing imports, String-vs-IntegrationId arg; secret audit clean. No device — device checks blocked, never faked. No OAuth. No APK.
- **Phase 18** — Multi-Account & Unified Inbox (`data/prefs/`: `ActiveAccountPreferences` with DataStore persistence; `AccountSelection.Unified` vs `AccountSelection.Single`; `data/local/`: `ThreadDao.observeUnified`, `MessageDao.observeUnified/observeUnifiedUnread/observeUnifiedStarred`, `SenderDao/CompanyDao.suggestByTextUnified`, `AccountDao.setEnabled`; `data/repository/`: `MailRepository.observeUnifiedThreads/observeUnifiedUnread/observeUnifiedStarred` Room & test fake implementations; `core/search/` + `data/repository/SearchRepository`: nullable `SearchQuery.accountId` for unified search joining `accounts a WHERE a.isEnabled = 1`, `SearchUiState.isUnified` + `accountsById` + badge rendering; `ui/settings/`: `AccountsViewModel` + `AccountsScreen` with active account switching, toggling enabled state, disconnect confirmation dialog with cascading cleanup, local account creation; `ui/mail/`: `MailViewModel` & `MailScreen` unified inbox presentation, top bar and drawer showing "All Accounts", account badges on thread rows; ViewModels updated with `activeAccountPreferences` support): COMPLETE. 10 new unit tests (3 prefs + 5 viewmodel + 2 search + 6 in-memory Room suite); 545/545 pure-JVM tests pass via direct JUnitCore (6 Robolectric classes excluded per sandbox limitation); `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked. No OAuth. Stopped strictly at Phase 18 boundary.
- **Phase 19** — Background Sync & Offline Behavior (`data/sync/`: `AccountSyncWorker` executing account-isolated sync + background intelligence pipeline with explicit `AccountId` context; `BackgroundProcessingPipeline` running local normalization, classification, priority, temporal, actions, and search indexing with error isolation; `BatteryConsciousSyncScheduler` implementing Android 15m periodic limits, unmetered/charging/battery constraints, staleness-aware app start, app resume, and connectivity recovery triggers; `SyncModels` providing `AccountSyncStatus`, `UnifiedSyncStatus`, `SyncTrigger`, `SyncConstraints`, `SyncWorkerResult`; `SyncTimeFormatter` honest relative timestamps; `ConnectivityObserver` enriched with `NetworkState` detecting metered/unmetered networks; `MailOrganizerApp` onAppStart hook + `MainActivity` onResume hook; `MailViewModel` integrating per-account & unified sync status + last synced timestamp; `AccountsScreen` per-account sync state rendering): COMPLETE. 25 new unit tests (4 sync models + 6 worker + 8 pipeline + 7 scheduler); 570/570 pure-JVM tests pass via direct JUnitCore (6 Robolectric classes excluded per sandbox limitation); `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked. No OAuth. Stopped strictly at Phase 19 boundary.
- **Phase 20** — Noise, Newsletter & Cleanup System (`core/cleanup/`: `CleanupModels.kt`, `UnsubscribeSafety.kt` parsing RFC 2369 headers without executing links, `CleanupProtector.kt` shielding security/receipts/career/education/action-required/starred/high-priority items, `NewsletterAnalyzer.kt` multi-signal confidence scoring, `NoiseAnalyzer.kt` notification & low-value detection, `CleanupRecommendationEngine.kt` conservative explainable recommendations; `domain/cleanup/`: `CleanupUseCase.kt` account-isolated candidate gathering and sender clustering; `ui/cleanup/`: `CleanupViewModel.kt` + `CleanupScreen.kt` M3 UI with recommendations and newsletter tabs, non-destructive safety notices, unified/single account awareness; navigation and settings entry points wired): COMPLETE. 26 new unit tests (5 unsubscribe + 7 protector + 3 newsletter + 3 noise + 3 recommendation + 3 usecase + 2 viewmodel); 596/596 pure-JVM tests pass via direct JUnitCore across 72 test classes; `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked. No OAuth / write mutations (deferred to Phase 22). Stopped strictly at Phase 20 boundary.
- **Phase 21** — Waiting-for-Reply & Conversation Intelligence (`core/conversation/`: `ConversationModels.kt` (7 deterministic states: NO_ACTION, AWAITING_USER_REPLY, AWAITING_OTHER_PARTY, RECENTLY_REPLIED, STALE_CONVERSATION, RESOLVED, UNKNOWN; ParticipantRole; timeline; structured explainability), `ParticipantClassifier.kt` (account-aware user/other-party/automated classification with alias handling and display name normalization), `ConversationSignals.kt` (reply-expectation requests, direct question detection, closure/resolution pattern extraction), `ConversationAnalyzer.kt` (local-first, deterministic thread analyzer with negative signal shielding for newsletters, receipts, notifications, no-reply senders; follow-up opportunity detection after 3 days without response); `domain/conversation/`: `ConversationIntelligenceUseCase.kt` (thread and message analysis with reactive flows); `ui/mail/`: `ConversationVisuals.kt` (M3 status badge pills, expandable intelligence card with timeline & reasons), wired into `ThreadRow.kt`, `ThreadScreen.kt`, and `ThreadViewModel.kt`): COMPLETE. 25 new unit tests (6 participant + 4 signals + 11 analyzer + 4 use-case); full suite stands at 621/621 pure-JVM tests passing across 75 test classes; `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked. Stopped strictly at Phase 21 boundary.
- **Phase 23** — Privacy Center & Security Hardening (`core/privacy/`: `PrivacyModels.kt` (4 sensitivity tiers, 8-category Data Inventory, Google scope disclosures, live security audit snapshot) + `SecuritySanitizer.kt` (regex engine redacting Bearer tokens, raw OAuth tokens, authorization headers, email addresses preserving domains; safe URL/mailto validators); `core/MoLogger.kt` hardened with logging sanitization; `res/xml/data_extraction_rules.xml` + `backup_rules.xml` + `AndroidManifest.xml` excluding databases and app-private files from backup leakage; `domain/privacy/`: `PrivacyUseCase.kt` (audits, cascading account data deletion, full local wipe without remote Gmail mutation); `ui/privacy/`: `PrivacyCenterScreen.kt` + `PrivacyViewModel.kt` + `PrivacyViewModelFactory.kt` (M3 tabbed Privacy Center: Overview, Your Data, Google Access, Security, account deletion dialog, local data purge dialog); `ThreadScreen.kt` hardened external link launcher): COMPLETE. 17 new unit tests (7 sanitizer + 3 models + 3 use case + 4 viewmodel); full pure-JVM suite stands at 638/638 passing across 79 test classes; `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked. Stopped strictly at Phase 23 boundary.
- **Phase 24** — Performance, Scalability & Battery Optimization (`core/performance/`: `PerformanceBenchmarkTest.kt` synthetic benchmark suite over 100/1k/5k/10k emails; `DeterministicClassifier`: static `PRECEDENCE_RANK` map + `ENTRY_COMPARATOR` with `maxWithOrNull`; `SignalExtractor`: fast-path URL substring guard; `CompanyDetector`: thread-safe bounded LRU cache of 512 entries; `DeterministicPriorityEngine`: streamlined rule evaluation, single-pass precedence winner, conditional sorting; `data/local/`: `MIGRATION_8_9` with 10 composite indexes, Room bumped to v9, schema `9.json`; `ExtractedItemDao` & `IntelligenceRepository`: batch `getByMessages`; `ThreadViewModel`: eliminated 2 N+1 query patterns for classifications and temporal items; `SearchIndexStore`: compiled statement batching `indexDocuments`; `RoomMailRepository`: batch company resolution; `MessageCard`: Compose `remember(html)` for `htmlToText`): COMPLETE. 8 benchmark test suites pass; 646/646 unit tests pass across 80 test classes; `:app:assembleDebug` BUILD SUCCESSFUL (`app-debug.apk` 15MB); secret audit clean. No device. Stopped strictly at Phase 24 boundary.
- **Phase 25** — Analytics & Insights (`core/analytics/`: `AnalyticsModels.kt` (4 date ranges: Today/Last 7 days/Last 30 days/All time; 8 deterministic insight types; UNIFIED_ACCOUNT_ID constant) + `InsightGenerator.kt` (deterministic insight generation from local data); `domain/analytics/`: `AnalyticsUseCase.kt` (account-isolated analytics aggregation with reactive flows); `ui/analytics/`: `AnalyticsScreen.kt` + `AnalyticsViewModel.kt` + `AnalyticsViewModelFactory.kt` (M3 analytics dashboard with date range selector, insight cards, volume charts); navigation route wired in `AppNavGraph.kt`; settings entry point in `SettingsScreens.kt`; DI container updated in `AppContainer.kt`; dashboard integration in `HomeScreen.kt`): COMPLETE. 3 new test files (InsightGeneratorTest, AnalyticsUseCaseTest, AnalyticsViewModelTest); `:app:compileDebugKotlin` BUILD SUCCESSFUL; secret audit clean. No device — device checks blocked, never faked. No OAuth. Stopped strictly at Phase 25 boundary.
- **Phase 26** — Optional AI Fallback Architecture (`core/ai/`: `AiCapability`, `AiAvailabilityState`, `AiProviderId`, `AiRequest`, `AiResult`, structured outputs `AiClassificationOutput`/`AiThreadSummaryOutput`/`AiTemporalOutput`/`AiIntentOutput`; `DataMinimizer.kt` stripping HTML, MIME headers, recipient lists, truncating text <=500 chars, regex sanitizing emails/tokens; `PromptBoundary.kt` anti-injection delimiter shielding; `AiOutputValidator.kt` strict JSON schema parsing, 0.85 confidence cap, blocking prohibited action commands like delete/send/drop, and security downgrade shield; `AiCache.kt` thread-safe LRU cache of 500 entries; `data/ai/`: `AiProvider.kt` pluggable provider interface, `AiProviderRegistry.kt`, `LocalRuleAiProvider.kt` offline on-device fallback, `StubRemoteAiProvider.kt` remote BYOK HTTPS seam with connectivity awareness, `FakeTestAiProvider.kt`; `data/prefs/`: `AiPreferences.kt` DataStore with default-OFF master toggle & BYOK API key; `domain/ai/`: `AiManager.kt` orchestrator + `AiFallbackUseCase.kt`; `ClassifyMessageUseCase.kt` integrated to invoke AI only when deterministic engine returns UNCLASSIFIED; `PrivacyUseCase.kt` registered Optional AI Fallback Context as 9th Data Inventory item and wired cascading deletion; `ui/settings/`: `AiSettingsScreen.kt` + `AiSettingsViewModel.kt` with master toggle, provider picker, capability filters, BYOK API key dialog, live availability indicator, disconnect action; `ui/mail/`: subtle "AI suggestion" badge pill and user-initiated on-demand `ThreadSummaryCard.kt` with advisory notice): COMPLETE. 41 dedicated AI unit tests; 736/736 unit tests pass across 97 test classes via direct JUnitCore; `:app:assembleDebug` BUILD SUCCESSFUL (`app-debug.apk` 15MB); secret audit clean. No device — device checks blocked, never faked. No OAuth. Stopped strictly at Phase 26 boundary.
- Push history: phases 0/1/2/4 in commits cc38873, 855ff48, 6f00813, 8a9267f; Phase 5 in 2 commits; Phase 6 in 3 commits; Phase 7 pushed (see phase report for commit); Phase 8 pushed (5 commits); Phase 9 pushed (5 commits: 4ea16fc, d0939e8, 7425b52, f9a0e67, 69254f9); Phase 10 pushed (4 commits); Phase 11 pushed (4 commits); Phase 12 pushed (7 commits); Phase 13 pushed (6 commits); Phase 14 pushed (see phase report for commit); Phase 17 pushed (9 commits); Phase 18 pushed (7 commits); Phase 19 pushed (commit faa5205); Phase 24 pushed (7 commits: 8fde3aa, e46f68b, a38990c, f4eaed4, b008520, 0abfc05, b8be78b).

## Decision log
- **2026-10-10:** Phase 26 (Optional AI Fallback Architecture) COMPLETE. Key decisions: AI is an optional secondary fallback, never the foundation of Mail Organizer; deterministic local intelligence remains authoritative; strict precedence hierarchy enforced: User Correction > User Rule > Deterministic Intelligence > Optional AI Fallback > Unknown; AI disabled by default with explicit user opt-in (`isAiEnabled = false`, `hasUserConsented = false`); BYOK remote HTTPS seam (`StubRemoteAiProvider`) isolated from credentials/OAuth; local offline rule provider (`LocalRuleAiProvider`) operates without internet; DataMinimizer aggressively removes HTML, MIME headers, recipient lists, truncates bodies to 500 characters, and sanitizes identifiers; PromptBoundary enforces clear task delimiter boundaries against prompt injection; AiOutputValidator enforces strict structured JSON schemas, caps model confidence at 0.85, rejects executable command verbs (e.g., delete/send/drop), and prevents downgrading deterministic SECURITY classifications; AI output never directly executes actions; on-demand thread summarization is explicitly user-initiated with clear advisory notices ("Advisory summary · May contain errors"); Data Inventory updated with "Optional AI Fallback Context" (9th inventory item) with full cascading account deletion and local purge wiring. Test lessons: full unit test suite now stands at 736 tests across 97 test classes passing with 0 failures; build compiles cleanly and generates 15MB debug APK.
- **2026-10-10:** Phase 24 (Performance, Scalability & Battery Optimization) COMPLETE. Key decisions: measure first, optimize real bottlenecks, re-measure; eliminate all N+1 queries in ViewModels; static precedence tables and single-pass winner determination in classification and priority engines; short-circuit regex matching on long email bodies when triggers are absent; bounded LRU cache (512 entries) for recurring sender company lookups; compile SQLite statements once for FTS index upsert loops; Room schema bumped to v9 with additive composite indexes supporting high-frequency sorting and filtering; memoize costly Compose transformations during recompositions; privacy and security guarantees strictly preserved with zero telemetry and zero plain credential leakage. Test lessons: micro-benchmarks require JVM warm-up to separate classloading/JIT compilation from steady-state measurements; full test suite stands at 646 pure-JVM tests across 80 test classes.
- **2026-10-10:** Phase 23 (Privacy Center & Security Hardening) COMPLETE. Key decisions: minimize what is collected, minimize where data is sent, minimize what is stored, and make the rest understandable; plain secrets/tokens strictly forbidden in database and logs; MoLogger automatically sanitizes all log outputs by redacting bearer tokens, OAuth tokens, and email addresses while preserving domain context; Android backup rules (`data_extraction_rules.xml` and `backup_rules.xml`) strictly exclude local SQLite databases and app-private cache files from cloud backup and device transfer; Privacy Center provides a 4-tab Material 3 experience detailing the complete 8-category Data Inventory, Google scope disclosures with honest deferred status notes (Gmail write Phase 22, Calendar Phase 15, Tasks Phase 16), live security audit checks, and honest local deletion/purge flows that never modify remote Gmail data; external link launcher validates safe http/https schemes and rejects dangerous schemes (javascript:, file:, content:, data:). Test lessons: test suite now stands at 638 pure-JVM tests across 79 test classes.
- **2026-10-09:** Phase 21 (Waiting-for-Reply & Conversation Intelligence) COMPLETE. Key decisions: conversation intelligence explains what appears to be happening in a thread without inventing intent and without automatically sending messages; never equates "no reply" with "user forgot" or assumes an incoming email requires a reply unless explicit reply-expectation signals or action items exist; negative signals (newsletters, notifications, automated receipts, spam, no-reply senders) strictly shield against false "awaiting reply" states; direction is multi-account aware based on the owning account's email address and aliases; thread chronology enforces strict oldest-to-newest ordering; 7-day threshold marks awaiting conversations as stale; 3-day threshold identifies follow-up candidates when user replied and is waiting for other party; UI provides clear M3 state badge pills and an expandable header with full transparent reasoning. Test lessons: full unit test suite now stands at 621 tests across 75 test classes.
- **2026-10-09:** Phase 20 (Noise, Newsletter & Cleanup System) COMPLETE. Key decisions: identify and organize first, recommend cleanup second; newsletters are never equated with unwanted mail, and low-value is never equated with safe-to-delete; destructive mutations (trash/spam/label removal) are NOT executed in Phase 20 and are cleanly deferred to future Phase 22 Gmail-write architecture; unsubscribe links and mailto headers are treated as UNTRUSTED and NEVER automatically invoked or fetched in the background; hard protection shield explicitly preserves security, receipts/orders, career, education, action-required, starred, and high/critical priority emails with transparent human-readable explanations; recommendations support multi-account or single-account scopes with strict account isolation. Test lessons: full unit test suite now stands at 596 tests across 72 test classes; state flows in ViewModels require test-dispatcher collection loops; Room and mock instances respect account foreign keys.
- **2026-10-09:** Phase 19 (Background Sync & Offline Behavior) COMPLETE. Key decisions: background sync workers carry explicit AccountId and never read active UI account (protects against race conditions when switching accounts); periodic background intervals clamped to platform minimum 15 minutes; BackgroundProcessingPipeline runs strictly local on-device intelligence (company attribution → classification → priority → temporal extraction → action card generation → search index) with failure isolation per engine so malformed emails never abort the pipeline; offline state cleanly distinguished from authentication failure or API errors (never shows "Reconnect Gmail" when simply offline); last sync timestamps are truthful and formatted without fake precision; lifecycle triggers (onAppStart, onAppResume, onConnectivityChanged) apply 15m staleness thresholds to prevent aggressive network polling; manual refresh in unified mode executes independent concurrent sync across enabled accounts. Test lessons: full unit test suite now stands at 570 tests across 65 test classes.
- **2026-10-09 ~23:00 IST:** User called HOLD on all build execution. Gemini Pro quota exhausted (resets ~00:08 IST); Phase 19 partial work pushed as WIP to preserve it (not complete, not verified). Auto-retry cron removed per user — resume Phase 19 manually only when user signals. Nothing fires on its own.
- **2026-10-09:** Phase 18 (Multi-Account & Unified Inbox) COMPLETE. Key decisions: unified inbox is presentation-layer only — never shared ownership or cross-account data leaking; Room queries for unified views dynamically join on `accounts a WHERE a.isEnabled = 1` ordered newest-first; disconnecting an account cascades through Room FK boundaries leaving other accounts unaffected; SearchQuery.accountId is nullable (null = unified search across all enabled accounts); UI displays clear account indicators and badges in unified mode; ViewModels cleanly fall back to primary account when ActiveAccountPreferences is null preserving compatibility with tests from earlier phases.
- **2026-10-09:** Phase 17 (Integration Manager) COMPLETE and pushed. Key decisions: manager coordinates, never absorbs API logic or performs external side effects; CONNECTED reported only when genuinely usable (usableCapabilities empty otherwise); Calendar/Tasks are first-class UNAVAILABLE (never fake-connected); Gmail wraps Phase 4's fail-closed seam (AUTH_REQUIRED naming Phase 3, never CONNECTED without OAuth); DEADLINE deliberately unmapped in action routing (ambiguous — manager doesn't guess); account-removal clears only that account's integration metadata; credentials never in DB (integration_states = metadata only, "" sentinel for no-account since Room forbids nullable PKs). Test lessons: runTest must share ONE TestDispatcher (pass it explicitly — separate dispatchers cause "different schedulers" crash); backgroundScope.launch + SharedFlow.collect doesn't observe emissions on the test scheduler (use test-scope launch + runCurrent + cancelAndJoin); runTest's testScheduler is the reliable virtual-time handle.
- **2026-10-09:** Phase 14 (Action Cards & Action Engine) COMPLETE and pushed. Key decisions: engine proposes / user confirms / nothing external automatic; generation and safety validation are separate layers (ActionCandidateGenerator v1 + ActionSafety); ActionUrgency independent from email Priority; ActionExecutor registry intentionally empty — confirming external proposals honestly reports "not connected", never fake success; deterministic dedup id `messageId|actionType|targetKey`; thread-dedup on (actionType, targetKey) — temporal cross-message same-event matching stays out (no fuzzy matching); schema v6→v7 additive with status backfilled from legacy booleans. Test lessons: MoResult/MoError live in the root package (not core.*); Room autoGenerate respects preset non-zero ids (mirror in fakes for identity-pinned tests); test fakes must mirror DAO boolean-sync semantics; validate migration SQL on real sqlite3 when MigrationTest is Robolectric-blocked.
- **2026-10-08:** User greenlit Android execution. Build UI + foundation now; skip all Google Auth/API-key/OAuth work to the very end. Report after every phase. Push directly to `main`.
- **2026-10-09:** Dedicated side chat "Mail Organizer — build progress" created for reports/push confirmations.
- **2026-10-09:** User required periodic progress updates during long phases (after Phase 0 ran 2.5h silently).
- **2026-10-09:** CONTEXT.md rule created at user's request: read before work, update after every prompt; README links to it; continuity across accounts/chats.
- **2026-10-09:** Standing push authorization: push as you go, no approval waits; phases one by one, continuously.
- **2026-10-09:** 4 approval prompts in a row were a one-time 105-file catch-up split (arg-size limit) — not the norm; one prompt per phase going forward.
- **2026-10-09:** Quota guardrail set: hard stop at 98% free-weekly usage, notify in side chat, never touch 1B pool without explicit approval. (Supersedes earlier "full speed even if it burns the 1B pool" wording.)
- **2026-10-09:** User emphasized CONTEXT.md is THE continuity file — redesigned for agent-efficient understanding. Phase 5's agent had failed to update it (stale push); rule 4 above added to prevent repeats.
- **2026-10-09:** Phase 7 (Email Classification Engine) COMPLETE and pushed. Key decisions: message-level classification; LOW_VALUE gated (wins only when nothing else scored); Social/Spam destinations are Gmail-label-driven, Promotional is classifier-driven; confidence = rule strength not probability; no schema migration needed. Test lesson: `android.util.Log` no-op stub (`/tmp/mo-test/out/stubs`) must be FIRST on the JUnitCore runtime classpath — without it, MoLogger calls throw "Stub!" from android.jar and break error-path tests (this caused 2 pre-existing MailViewModelTest failures).
- **2026-10-09:** Phase 8 (Company & Sender Intelligence) COMPLETE and pushed. Key decisions: company identity = registrable domain (subdomains merge); pinning only reorders filter list (never stars/moves mail); company filter lives INSIDE Promotional/Social/Spam (never a drawer destination); chip counts from global GROUP BY (never page-derived); classifier VERSION 1→2 with new IMPORTANT_RECURRING_SENDER rule (weight 45, below IMPORTANT_PERSONAL's 50); recurring = ≥3 messages (documented heuristic). Caught 2 real bugs during verification (detector accepted "@example.com"; 4 test-flow races). Test lesson: Turbine `test {}` needs `cancelAndIgnoreRemainingEvents()` when breaking early from await loops.
- **2026-10-09:** Phase 10 (Search & Local Indexing) COMPLETE and pushed. Key decisions: FTS5 `messages_fts` virtual table (messageId/accountId UNINDEXED; unicode61 remove_diacritics 1; body capped 20k chars); schema v4→v5; structured SearchQuery all the way down (UI never builds SQL); every FTS term double-quote-escaped (injection-proof by construction); bm25 positional weights tied to FTS column order; write-path hooks index in the same Room transaction; queries never logged. Test lessons: `advanceUntilIdle()` ADVANCES virtual time — use `runCurrent()` for debounce tests; `Dispatchers.setMain` required for ViewModel tests; Log stub needs 3-arg overloads.
- **2026-10-09:** Phase 11 (Dashboard & Information Architecture) COMPLETE and pushed. Key decisions: dashboard hierarchy account→attention→priority→recent→categories→companies over real repository flows (no placeholders); MoNavBar (5 primary) + MoAppTopBar canonical chrome; detail routes validate args → NotFound; counts are DAO GROUP BY (account-scoped, NULL-safe); Settings honest (no fake login; account switching = Phase 18 seam); Actions view-only (Phase 14 seam). Test lessons: `stateIn(WhileSubscribed)` emits initial value first — account-list flows feeding `flatMapLatest` must use `SharingStarted.Eagerly`; ViewModel tests need `advanceUntilIdle()` after VM creation + skip transient Loading emissions.
- **2026-10-09:** Phase 12 (Rules & User Corrections) COMPLETE and pushed. Key decisions: explicit corrections (message > sender > domain) outrank user rules, which outrank the deterministic base; undo deletes correction rows so engines restore the base (never stored twice); rule precedence = (order, id), first match per action type wins; empty-condition rules never match; conflicts detected by same-condition-signature + different values; legacy Phase 2 rule rows derive equivalent structured rules. Test lessons: Room/SQLite need stub sources for pure-JVM compiles; `flatMapLatest` needs explicit import; nested vs top-level classes (RulePreview is top-level).

## Environment limitations (sandbox)
- No emulator/adb — device install/launch/screenshot checks blocked until real hardware or CI. Mark device criteria `[!]`.
- Gradle test worker crashes on startup — run tests manually (JUnitCore); environment issue, not code.
- JVM network blocked — dependencies pre-fetched locally; standard Maven remotes configured for normal machines.
- Full Gradle build/lint sometimes skipped (daemon issues); per-file compilation verified instead.

## Key files
- `android/docs/development-status.md` — per-phase progress log
- `android/docs/architecture.md` — architecture decisions
- `android/docs/data-model.md` — every table, index, token boundary
- `android/docs/skills/` — 10 persistent engineering skill docs
- `execution plan/android/` — phase specs 00–30; `execution plan/android/editor-rules.md` — master rules

## Decision log (continued 2026-10-11)
- **2026-10-11 ~04:00 IST:** Mailstack logo integrated as circular launcher icon. Generated PNGs for mdpi-xxxhdpi with logo centered on white background; adaptive foreground PNG (432px); `android:roundIcon` added; `colors.xml` with white `ic_launcher_background`. NOTE: the commit briefly contained BOTH `drawable/ic_launcher_foreground.xml` and `.png` (duplicate resource — build failure); the XML was removed, PNG kept. Verified in final APK.
- **2026-10-11:** User directed to use Muse credits (not Antigravity) for completion. Antigravity OAuth was failing repeatedly with "Malformed auth code" errors.
- **2026-10-11 — HONEST STATUS of the OAuth/integration work (partial, NOT complete):** foundation code for Phase 3 (AuthRepository, SecureTokenStorage via EncryptedSharedPreferences, GoogleAuthRepository, GmailApiClient, ConnectAccount UI), Phase 15 (RealCalendarIntegrationAdapter), Phase 16 (RealTasksIntegrationAdapter), Phase 22 (mark read/unread, archive helpers), Phase 29 (version 1.0.0, production-prep docs) was written BUT is not verified and not complete. Known gaps: token exchange returns null (OAuth cannot complete), PKCE/state not implemented, redirect URI vs manifest host mismatch, Connect UI not wired into nav graph, no auth-state restore, no real revocation, scopes expanded all-at-once (not incremental), Calendar/Tasks not wired into Action Engine, no tests for any of it. Earlier "COMPLETE" claims for these phases were overstated — corrected here. This work needs a real device + real Google sign-in to finish and verify.
- **2026-10-11 — Phase 30 verdict: RELEASE BLOCKED** (corrected; earlier "CONDITIONAL GO" was wrong while OAuth is nonfunctional). Blockers: Phase 3 incomplete, no production keystore, no production OAuth client, no Play Console setup, no physical device validation.
- **2026-10-11 ~09:40 IST — CRASH REPAIR DONE:** user reported the app crashes immediately on launch (same symptom as OmniBuds, different cause — OmniBuds was missing library resources in its hand-built APK; Mailstack is Gradle-built). Hardened startup: `MailOrganizerApp.onCreate()` can never kill the process (init failures recorded as `startupError`, scheduler starts on a background thread); `MainActivity` shows a `StartupErrorScreen` with copyable diagnostics instead of crashing; FTS5 table creation is now failure-safe (`SearchIndexStore.createTableOrSkip` — a missing FTS5 module no longer aborts DB creation); uncaught exceptions persist to `filesDir/crash-reports/` via new `core/CrashReporter` (pure JVM, 7 unit tests). Verification: 781/781 tests pass, lint 0 errors, `assembleDebug` BUILD SUCCESSFUL. Fresh APK built 09:38 IST (16.3 MB, com.greninjaop.mailorganizer 1.0.0, "Mailstack" label, icons + CrashReporter/StartupErrorScreen confirmed in dex) and delivered to the user for on-phone validation.
- **2026-10-11 ~10:00 IST — ICON API QUIRK (final):** tried hard to get the circular logo PNGs onto the remote (user asked to "at least try to rename"). Definitive finding: the GitHub MCP API **cannot store binary files correctly at all** — every method (`push_files`, `create_or_update_file`) stores base64 text instead of decoded bytes (verified against git blobs; the `get_file_contents` API misleadingly decodes on read, hiding the corruption). All corrupt PNGs removed. Repo uses the vector "M" foreground + adaptive icon and builds cleanly (`assembleDebug` BUILD SUCCESSFUL). The DELIVERED APKs keep the real circular logo (built locally). To restore logo PNGs in the repo, re-add them via a git client with proper auth (regenerate from `logo/logo.png`).
- **2026-10-11 ~13:30 IST — PHASES 3/15/16/22 COMPLETE (code-complete, device verification pending):** user authorized finishing all deferred OAuth/integration work ("make everything possible and fully working"). What was built: (1) **Phase 3 Google OAuth**: PKCE S256 + state, real HTTPS token exchange/refresh/revocation (`GoogleOAuthHttp`), encrypted pending-flow storage, stable account IDs, Gmail-profile smoke test, Connect UI wired into nav graph + manifest redirect + `onNewIntent`, `gmail.readonly` base scope only; real Gmail sync engine (`HttpGmailSyncApi`: messages.list/get, history.list, profile) replacing `DeferredGmailSyncApi`. (2) **Phase 15 Calendar**: real Calendar API v3 client (`GoogleCalendarApi`), idempotent event creation (iCalUID + `mailstackKey` extended property + receipt cache + recovery lookup), `CalendarActionExecutor` registered in `AppContainer.actionExecutorRegistry`, incremental `calendar.events` scope. (3) **Phase 16 Tasks**: real Tasks API client (`GoogleTasksHttp`, PATCH via `X-HTTP-Method-Override`), idempotent task creation (marker-in-notes + execution store + recovery scan), `CreateTaskActionExecutor` registered, incremental tasks scope. (4) **Phase 22 Gmail writes**: `GmailWriteApi`/`GmailWriteUseCase` (fail-closed confirmation gate, account isolation, idempotency pre-check, Gmail-verified-before-local-update, per-item bulk), UI (MessageCard overflow menu → confirm dialog → snackbar; 403 → incremental-auth upgrade dialog). Cross-cutting: new `data/http/HttpTransport` seam (all 5 API clients injectable; `UrlConnectionTransport` production) — required because the sandbox blocks ALL JVM TCP including loopback, so tests cannot use a local HTTP server; `org.json` removed from `GmailApiClient` (android.jar stub) in favor of shared `JsonParser`; fixed 2 lint `NewApi` errors (`LocalDate.ofInstant` → `atZone().toLocalDate()`). Verification: **957/957 tests pass** (781 baseline + 176 new), lint 0 errors, `assembleDebug` BUILD SUCCESSFUL, all new classes confirmed in dex. APK built 13:07 IST (16.4 MB). Honest gaps: NO live Google sign-in/sync/API calls verified here (no device, no emulator — OAuth flow, token exchange, sync, calendar/task creation, and write actions need the user's phone + enabled Gmail/Calendar/Tasks APIs + test-user consent); release keystore exists locally but production OAuth client/Play Console not set up (Phase 29/30 still release-blocked).
- **2026-10-11 ~evening IST — OAUTH REPAIR v3 (app-side, code):** user hit "authentication not working" — Google rejected the auth request because the redirect URI used the double-slash form and (console-side) custom URI schemes were disabled on the new Android OAuth client. App-side fixes: redirect URI changed to RFC 8252 single-slash `com.greninjaop.mailorganizer:/oauth2redirect` (`GoogleAuthRepository.kt`); manifest uses scheme-only OAuth intent filter (`AndroidManifest.xml`); `MainActivity.onCreate(intent)` now handles cold-start OAuth redirects (MIUI kills the process while the browser is in front — `onNewIntent` alone missed it); stable debug keystore created at `/home/hatch/keys/mailstack-debug.keystore` (alias/pass `mailstack-debug`, SHA-1 `07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05`) and pinned for debug builds in `app/build.gradle.kts` (the sandbox Gradle auto-key is NOT stable across agents — every key change breaks the Cloud Console OAuth registration). Endpoint probe with a valid PKCE S256 request returned 302 to `signin/oauth/error` with decoded reason "Custom URI scheme is not enabled for your Android client" — a Cloud Console toggle, not a code bug. Verification: 25/25 OAuth tests, full 957/957 suite, `assembleDebug` BUILD SUCCESSFUL, APK signature confirmed as the stable cert. Delivered `Mailstack-v1.0.0-oauth-v3.apk` (16,374,970 bytes). User checklist given: Cloud Console → Credentials → Android client (…f0vel0oe1i2g10rc9cst028s8mhnhl13) → Advanced → Enable Custom URI Scheme → ON; register the stable SHA-1; enable Gmail/Calendar/Tasks APIs; add their Gmail as OAuth consent test user; uninstall old builds before installing.
- **2026-10-12 — GOOGLE-SIDE BLOCKER CLEARED:** user enabled the Custom URI Scheme toggle and saved. Re-probed the authorize endpoint immediately → 302 to `v3/signin/identifier` (accepted). Sign-in can now proceed once the remaining console steps (APIs, test user) are done.
- **2026-10-12 — CONNECT BUTTON FIX:** user's screenshot showed only the local/demo account dialog — the real "Connect with Google" button was unreachable whenever demo/local accounts existed (Accounts empty-state only showed "Connect Gmail"). Added an always-visible **"Connect Google account"** button on the Accounts screen labeled as real Gmail sign-in (`SettingsScreens.kt`). Delivered `Mailstack-v1.0.0-oauth-v4.apk` (16,575,796 bytes, stable cert). User expectation confirmed: tap → system browser opens Google's standard sign-in page (account chooser / email+password) → consent → redirect back into Mailstack. No password is ever entered in-app.
- **2026-10-12 — DISCONNECT CRASH FIX (v1.0.1):** user reported tapping × to remove an account crashed the app to the phone home screen, account not removed (reproduced twice). Root cause: `RoomAccountRepository.deleteById` calls `SearchIndexStore.deleteForAccount` BEFORE `accountDao.deleteById`; the store issued raw `DELETE FROM messages_fts` with no table guard, and on devices where FTS5 creation was skipped (`createTableOrSkip` failure-safe path) the table doesn't exist → `SQLiteException: no such table: messages_fts` escaped `AccountsViewModel.disconnectAccount`'s coroutine → process crash before the account row was deleted. Fixes: `tableExists()` guard added to `deleteDocument`/`deleteForAccount`/`clearAll` (`SearchIndexStore.kt`); `disconnectAccount` restructured so every cleanup step (auth revoke, integration metadata, FTS rows, account delete, selection fallback) is individually best-effort — a cleanup throw can never crash the app nor block the account removal (`SettingsScreens.kt`). Regression tests: new `SearchIndexStoreTest` (5 tests; 4 failed pre-fix with the exact `SQLiteException` from the user's crash; all pass post-fix) + existing `AccountsViewModelTest` disconnect test still green (10/10 via direct JUnitCore). NOTE: test classpath from `:app:printTestClasspath` embeds main classes via the stale `runtime_app_classes_jar` — prepend `app/build/tmp/kotlin-classes/debug` when running JUnitCore directly or tests run old code.
- **2026-10-12 — THEME CLARIFICATION (not a code bug):** user reported the Connect Gmail screen rendering in light mode while the app was dark. Verified in code: `ConnectAccountScreen` uses only the ambient `MaterialTheme` (no hardcoded colors, no override) and sits inside the same `MailOrganizerTheme` wrapper as every other screen. Explanation: the v3→v4 reinstall (user uninstalled first, as instructed for the cert change) wiped the DataStore theme preference (user had Dark) → reset to System → system in light mode rendered the screen light. Fix for user: Settings → Appearance → Dark (segmented System/Light/Dark control exists). Hardening added anyway: the screen now explicitly paints `MaterialTheme.colorScheme.background` (the activity's platform theme is `Theme.Material.Light`, so unpainted areas would flash white).
- **2026-10-12 — TEST USERS REQUIRED:** user asked about Cloud Console Audience → Test users (their email not listed). Confirmed: while the OAuth consent screen is in **Testing** mode, ONLY listed test users can complete sign-in; anyone else gets blocked (Error 403). User must add their Gmail address there — sign-in cannot work without it. Also confirmed APK naming going forward: clean `Mailstack-1.0.1.apk` style (matches `versionName`), no descriptive suffixes — Android file providers dedupe/rename on identical names and the user couldn't find downloads.
- **2026-10-12 — PUSH STATE:** the 2026-10-11 OAuth v3 repair files were NEVER pushed (push attempt timed out on approval). Pushed in one batch on 2026-10-12 (10 files, all byte-verified remote): `GoogleAuthRepository.kt`, `AndroidManifest.xml`, `MainActivity.kt`, `OAuthPhase3Test.kt`, `app/build.gradle.kts` (stable-key signing + 1.0.1 bump), `SettingsScreens.kt`, `SearchIndexStore.kt`, `ConnectAccountScreen.kt`, `SearchIndexStoreTest.kt` (new), `CONTEXT.md`.
- **2026-10-12 — LAUNCHER ICON v1.0.2 (user request):** user wanted the square logo cut precisely as a circle, zero padding, no white background. Generated from `logo/logo.png` (1254x1254): inscribed circular crop, transparent corners; adaptive foreground at 108dp canvas (full-bleed circle) + legacy/round 48dp icons, all densities, in `res/mipmap-*/`. **LOCAL-ONLY:** PNGs cannot be pushed via the GitHub MCP API (binary corruption — verified 2026-10-11), so `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` (now referencing `@mipmap/ic_launcher_foreground` with transparent background), the manifest `roundIcon` attribute, and all PNGs stay in the working tree only; the repo keeps the vector "M" fallback and stays buildable. Do NOT push these files without re-adding PNGs via a real git client. Delivered in `Mailstack-1.0.2.apk` (16.9MB, stable cert, icon verified in-APK: transparent corners, opaque edge-to-edge circle).
- **2026-10-13 — FULL CODEBASE CHECKUP & AUDIT:** Comprehensive inspection across all layers (architecture, Room v9 data layer, Gmail sync engine, OAuth PKCE, Compose UI/Material 3, and working-tree changes).
  - Bugs found:
    1. [Medium] Test runtime classpath ordering issue: CompanyLogoResolverTest failed (NoSuchFieldError: company_adobe) due to a stale stub R.class embedded in bundleDebugClassesToRuntimeJar shadowing the resource R.jar.
    2. [Low / Noted] 8 test classes using RobolectricTestRunner (AccountDaoTest, AccountIsolationTest, MailDaoTest, MigrationTest, MultiAccountUnifiedInboxTest, SearchIndexStoreTest, RepositoryTest, SyncCoordinatorTest) cannot execute in the pure-JVM sandbox because Robolectric maven dependency fetching is blocked by sandbox network isolation. (Deliberately not modified; pre-existing environment constraint documented in CONTEXT.md).
    3. [Low / Noted] Direct runBlocking calls in MailOrganizerApp startup and AppContainer AI provider lambdas. Kept as-is because MailOrganizerApp startup runs in a dedicated background daemon thread ('mailstack-startup') without blocking the main UI thread, and AI provider closures run on background dispatchers during offline fallback queries.
  - Fixes made:
    - Updated test runner / verification classpath ordering to prioritize the freshly compiled R.jar ahead of runtime jars, ensuring resource field IDs match runtime bindings.
  - Build & Verification:
    - ':app:assembleDebug -Djava.net.preferIPv4Stack=true' completed with BUILD SUCCESSFUL (0 errors).
    - Full runnable unit test suite: 106 pure-JVM test classes executed via JUnitCore, 924/924 tests passing (0 failures).
- **2026-10-13 — FOUR-ISSUE FIX PASS (user bug reports, agy quota blocked):** user reported 4 issues with screenshots. Antigravity quota was exhausted (429, resets ~89h), so all fixes were implemented directly via the Muse path per the standing credits authorization. **Issue 1 — company logos cropped:** `CompanyLogo`/`RemoteCompanyLogo` used `ContentScale.Crop`, cutting wide logos (Adobe 578×512) in half — switched to `ContentScale.Fit` inside the circle with padding (whole logo always visible, never cropped/enlarged), applied to bundled AND remote artwork (`ui/companies/CompanyLogos.kt`). Bundled 12 new official favicon logos (Canva, Dribbble, Google, YouTube, Spotify, Reddit, Netflix, LinkedIn, Glasp, OpenRouter, GitHub, X) as `company_*.png` in `drawable-nodpi/` and registered them in `CompanyLogoResolver.bundledRes()` (+ `youtu.be`/`twitter.com` aliases); all 12 are full-color, shown as-is. **Issue 2 — mail order/missing vs Gmail:** ordering key (`internalDate` → `latestMessageEpochMs`) was already correct; the perceived disorder came from the broken Primary filter (fixed below). "Missing" mail = 500-msg/run cap with resume — raised `maxMessagesPerRun` 500→1000 (`SyncCoordinator.kt`). **Issue 3 — destinations didn't filter:** PRIMARY used the identical query as ALL_INBOX (no filtering at all); PROMOTIONAL used our classifier instead of Gmail's label. Fixes: new `ThreadDao.observePrimaryByAccount`/`observeUnifiedPrimary` (threads whose latest message lacks CATEGORY_SOCIAL/CATEGORY_PROMOTIONS/SPAM — Gmail Primary tab semantics), `MailRepository.observePrimary`/`observeUnifiedPrimary`, `MailViewModel` PRIMARY→observePrimary (account+unified), PROMOTIONAL→Gmail `CATEGORY_PROMOTIONS` label (company scope updated too); SOCIAL/SPAM/STARRED were already correct. Also found SPAM was never synced (list defaults to INBOX): added `GmailSyncApi.fetchSpamPage` + `HttpGmailSyncApi` impl (`labelIds=SPAM`) + one bounded 100-msg spam pass in `SyncCoordinator.runPaged` (best-effort; incremental syncs already see spam via history). **Issue 4 — empty mail bodies ("No content available"):** CONFIRMED root cause — sync stores `bodyText=null` ("bodies arrive on open") but fetch-on-open was never implemented. Implemented: `GmailApiClient.fetchFullMessage` (format=full → `RawGmailMessage` → `EmailParser`), `MessageDao.updateBody`, `MailRepository.updateMessageBody` (+best-effort FTS re-index), new `domain/mail/FetchMessageBodyUseCase`, `ThreadViewModel` triggers fetch for body-less synced messages on open (once per session, honest LOADING/FAILED states), `MessageCard` shows M3 `LinearProgressIndicator` + "Loading message…" while fetching; "No content available" now only for genuinely unfetchable mail. New `FetchMessageBodyUseCaseTest` (5 tests). Verification: `assembleDebug` BUILD SUCCESSFUL, full pure-JVM suite **931/931 tests pass** (107 classes; 8 Robolectric classes excluded — sandbox can't fetch their deps, pre-existing).

## 2026-10-13 — Search pill on all tabs + tab-switch crossfade
- Every primary destination (Home/Categories/Companies/Actions) now shows the same Gmail-style search pill in the top bar as the Mail tab (shared `MoSearchField` component); the lone magnifier icon is gone. Typing + IME search opens the Search screen with the query. Mail tab refactored to the shared component (identical look/behavior).
- Tab/page switching was a white flash: the NavHost had no explicit transitions and no background, so the raw window background bled through mid-fade. Now an explicit 300ms crossfade (shared `MoMotion.crossfade`, FastOutSlowInEasing) over the theme background — no flash.
- Verification: `assembleDebug` BUILD SUCCESSFUL, full pure-JVM suite 931/931 pass. Still unpushed, no APK per directive.

## 2026-10-13 — Splash card expanded + 4x upscaled
- User wants a Gmail-style launch splash with their Mail Stack card. Expanded their 941x1672 card to 1008x2352 (outpainted top/bottom, no stretch), then 4x AI-upscaled (Real-ESRGAN general-x4v3) to 4032x9408 PNG (~3MB).
- Asset: `~/workspace/mailstack-assets/mailstack-splash-4x.png`. Lesson: the published x4v3 weights store 33 independent prelu params (one per conv) — a shared-activation arch loads silently but outputs garbage.
- Splash implementation ON HOLD per user: they will design the final full-face card. Spec noted: fade in/out, fresh (cold-start) launch only, never from recents.
- Honest note given to user: auto-tracing this painterly art to SVG won't yield true crispness; the endgame is drawing the splash natively in Compose (Canvas gradients + vector + Text) = infinitely sharp.

## 2026-10-13 09:20 IST — Push STAGED but blocked (proactive mode)
- All 96 changes merged (remote CONTEXT.md fetched via API and merged; local entries preserved). 101 text files packed for push; 35 PNGs (drawable-nodpi/, mipmap-*/) stay local-only per the binary rule; 1 PNG deletion (`drawable/ic_launcher_logo_foreground.png`) still needs `delete_file`.
- Payloads staged at /tmp/pb00.json … /tmp/pb14.json (15 batches, ~95KB each — single-arg exec limit is 128KB). Could NOT push: proactive delivery context denies connector writes (`policy_denied`, `read_only_preparation`). The push must be fired from the main chat where write approvals work. Next step: run the 15 `push_files` batches + 1 `delete_file` from main chat, then byte-verify via `get_file_contents` readback.

## 2026-10-13 ~09:45 IST — PUSHED to main (15 commits)
- All 96 working-tree changes pushed to greninja-op/mail-organizer main as 15 batched commits (a0d85127 … debbff96): "Mailstack fixes 2026-10-13 (part N/15)". Single-arg exec limit (128KB) forced the split; one batched push_files per commit.
- 101 text files pushed, byte-verified remote-vs-local (CONTEXT.md, MoAppTopBar.kt, MoSearchField.kt, FetchMessageBodyUseCase.kt, Motion.kt all MATCH). 35 PNGs (drawable-nodpi/, mipmap-*/) remain local-only per the binary rule; the deleted launcher PNG never existed on remote (nothing to delete).
- Remote CONTEXT.md was fetched fresh and merged before pushing (anti-stale rule); no history lost.
- Still no APK delivered (user directive). Splash screen still on hold awaiting user's final card.

## 2026-10-13 ~13:15 IST — 1.0.11: demo inbox + banner removal (APK delivered)
- REMOVED the "Sample data — fixtures for UI development" banner from Mail
  (SampleDataBanner) and Home (SampleDataNote). The `isSampleData` state flag
  remains (tests use it); nothing renders it anymore.
- Seeder expanded into a realistic demo inbox: 7 original fixture threads +
  25 new company threads (Adobe, Amazon shipped→delivered 2-msg thread,
  GitHub PR + Dependabot, Netflix, Spotify, Google, LinkedIn x2, YouTube,
  Reddit, Canva, Dribbble, X, Duolingo, ClickUp, AWS, Apple, HDFC Bank,
  IndiGo, Swiggy, Flipkart, OpenRouter, Glasp, + 1 demo spam). 32 threads /
  37 messages total, all with full bodies. Gmail labels mirror Gmail
  (INBOX / CATEGORY_SOCIAL / CATEGORY_PROMOTIONS / SPAM) so every mailbox
  destination populates. Seeds only on fresh install (zero accounts).
- Flying-mail import animation REPORT (user asked why it never appeared):
  it was built in 1.0.6 and IS fully wired — Connect success → Continue →
  onConnected → import route (never-synced accounts only) → Intro →
  Importing with FlyingEnvelope Canvas animation → Done. The user never saw
  it because their account predates 1.0.6 and was already synced. Disconnect
  + reconnect shows it (disconnect cascades the sync state). No fix needed.
- Tests: 932 run, 924 pass; the only 8 failures are the known env-broken
  Robolectric classes (same as before). Test-lesson: the R class in
  kotlin-classes/debug is stale (predates logo PNGs) — put the fresh R.jar
  (processDebugResources) FIRST on the JUnitCore classpath.
- Version 1.0.11 (versionCode 12). APK verified: package, versionName,
  label Mailstack, stable cert SHA-1 07:31:05:EF:.... Delivered as
  Mailstack-1.0.11.apk (16.3 MB).

## 2026-10-13 ~14:10 IST — Fixes batch (in working tree, unpushed; NO APK until user says okay)
- FLYING-MAIL IMPORT — REAL BUG FOUND + FIXED. The animation existed and
  was wired, but never showed: MailOrganizerApp fired a background
  ACCOUNT_CONNECTED sync the moment OAuth succeeded, which marked the
  account synced BEFORE the user tapped "Continue" — so onConnected's
  never-synced check skipped the import route every time. Fix: never-synced
  accounts skip the background sync; the import flow owns the first sync.
  "Not now" now fires a background sync via ImportViewModel.skipImport()
  so the inbox still populates without the animation.
- LOGOS EVERYWHERE: mail rows now show the company logo (Mail list, company
  detail, thread view, search results). ThreadItem carries senderDomain
  (canonical company domain via CompanyDetector; null = personal sender
  keeps letter avatar). MailRowLayout renders CompanyLogo when present.
- MISSING LOGOS: Kick bundled (official K-mark from Kick's favicon,
  upscaled losslessly to 512px) + redditmail.com aliased to the Reddit logo
  (Reddit notifications come from redditmail.com, not reddit.com).
  Devpost was already correct (its monochrome glyph IS a D).
- TAB-SWITCH REDONE: (1) old crossfade drew both screens at half opacity
  simultaneously = washed-out flash. Replaced with Material fade-through:
  exit fades out first (200ms), enter fades in after 120ms delay (350ms)
  with a subtle 0.97→1.0 scale. (2) The real cause of the "refresh" feel:
  tab navigation used plain navigate() (stacked a FRESH screen per tap) or
  popUpTo without saveState (destroyed the tab). All 7 tab sites now use
  one navigatePrimary helper with popUpTo(HOME){saveState} +
  launchSingleTop + restoreState — tabs keep ViewModel/scroll/loaded data.
- ROWS +35% (shared MoSpacing tokens, identical everywhere): mail row
  88→120dp, avatar 40→54dp, company row 64→86dp, thread header 48→65dp,
  search row 72→97dp. The ROW grows, not the gap.
- Tests: 936 run, 928 pass; only the same 8 env-broken Robolectric
  classes fail. New: ThreadItemSenderDomainTest (4), resolver kick/redditmail.
- Test-lesson: R class in kotlin-classes/debug is stale (predates logo
  PNGs) — fresh R.jar FIRST on the JUnitCore classpath.

## 2026-10-13 ~14:15 IST — Universal logos: favicon fallback (in working tree; NO APK until user says okay)
- User asked: logos must exist for EVERY sender, not just familiar ones.
  Answer built: favicon fallback chain in CompanyLogoLoader.
  Resolution order now: bundled logo -> ICONS-ASSETS repo -> sender's
  website favicon (Google favicon service, 128px) -> letter avatar.
  Personal senders never reach the loader (CompanyDetector excludes
  them), so a favicon is only ever fetched for an organisation — a
  friend's Gmail never gets a Gmail logo.
- TRAP HANDLED: the favicon service returns a generic globe for unknown
  domains instead of failing (verified: 16x16 globe, byte-identical for
  bogus.invalid and example.com). The loader calibrates the globe at
  runtime by fetching it for a reserved `.invalid` domain (once per
  process), byte-compares every favicon result against it, and treats a
  match as "no logo" -> letter avatar.
- Privacy note in code: a favicon fetch reveals the sender's domain to
  Google, same as the repo fetch reveals it to GitHub. Results cached
  (memory + disk, per domain) so each domain is fetched once.
- New: CompanyLogoLoaderTest (3 tests: URL shape, globe match/mismatch).
  Total: 939 run, 931 pass, same 8 env-broken Robolectric classes.

## 2026-10-13 ~23:45 IST — "Breeze" glassmorphism redesign (app-wide; NO APK until user says okay)
- User-directed departure from strict Material 3 (their explicit request
  overrides the standing M3 rule): glassmorphism everywhere, not just the
  mail tab. Translucent cards + frosted-glass bars over the user-supplied
  Breeze background artwork (exact bytes, never recreated).
- Backgrounds: `drawable-nodpi/breeze_background_light.jpg` (pastel
  blue/white/mint) + `breeze_background_dark.jpg` (deep navy/blue-green).
  Base64 backups in `assets/backgrounds/` (light_mode_background.jpg.b64,
  dark_mode_background.jpg.b64 + README with md5 + recovery command) —
  the GitHub API stores content as text, so .b64 lands byte-exact while
  raw binaries corrupt. Drawn ONCE behind the NavHost in AppNavGraph, so
  every destination shows it and it stays static during tab transitions.
- New `ui/theme/Glass.kt`: BreezeBackground, GlassCard (24dp radius,
  1dp border, 6dp shadow), GlassPalette (light = white 62% / dark = navy
  62%), warning/amber chip colors. Dark/light read from the active
  scheme's background luminance, so it follows the theme-mode setting.
- Mail (reference target): glass search pill, glass mail cards (16dp
  sides, 12dp gaps), amber "Action required" pill with warning icon,
  amber "High priority" pill, glass drawer, transparent scaffold/top bar.
- Bottom nav: frosted-glass bar FLUSH with the bottom (never floating),
  hairline top border, selected = solid primary pill + white icon + primary
  label. M3 1.4 lesson: NavigationBarItemDefaults.colors(...) multi-arg
  is not Kotlin-visible; use .colors().copy(...) instead.
- Home/Categories/Companies/Actions/Thread/Search/CompanyDetail: glass
  cards for rows/tiles + transparent scaffolds/top bars.
- All other destinations (Settings, Accounts, Rules, Integrations,
  Cleanup, Privacy, Analytics, Automation, Foundation, Connect, Import):
  transparent scaffold/top-bar roots so the background shows; inner
  content cards unchanged (dialogs/overlays stay opaque for readability).
- Account indicator ("C" avatar) in mail rows is UNCHANGED — the
  reference screenshots show it (account avatar + sender avatar); it is
  the existing multi-account indicator, not a mockup artifact.
- Tests: 939 run, 931 pass; same 8 env-broken Robolectric classes.
  APK (debug, unverified on phone): backgrounds confirmed inside
  (77969 + 71495 bytes), 17.5 MB total.

## 2026-10-14 ~00:35 IST — Cold-launch splash (NO APK until user says okay)
- User's final splash cards implemented. Light card in light mode, dark
  card in dark mode. One plain fade: fade in 450ms (FastOutSlowIn) ->
  hold 650ms -> fade out 450ms (FastOutLinearIn), total ~1.55s. No
  slides, no gimmicks.
- Cold-launch only: shows iff savedInstanceState == null AND a
  per-process `splashShown` flag is false — never on resume from
  recents, never on rotation, only once per process. Fully closing the
  app and re-entering replays it. Error screen bypasses the splash.
- Theme gate: the animation does not start until the stored theme-mode
  preference emits (raw DataStore flow; its first emission is the real
  stored value), so the splash can never flash the wrong card. The app
  content is composed underneath and loads during the splash.
- Cards extended top/bottom by mirror extension (941x1672 -> 941x2512,
  9:24): the 280-row edge bands are flipped so the inner edge matches
  the original exactly (no seam) and stretched to 420px/side. Artwork
  untouched; ContentScale.Crop covers any phone without distortion.
- Artwork: drawable-nodpi/splash_light.jpg + splash_dark.jpg (exact
  extended bytes); base64 backups in assets/backgrounds/ with md5s in
  its README. Tests: 939 run, 931 pass, same 8 env-broken classes.

## 2026-10-14 ~09:35 IST — Splash fade-out fix (NO APK until user says okay)
- Bug caught before any APK: the fade-out animated only the artwork
  alpha, while the splash Box kept an opaque solid background — so the
  card dissolved into flat white/near-black, then hard-cut to the app.
- Fix: fade-in still animates only the artwork alpha (over the opaque
  placeholder); fade-out now animates a whole-splash `containerAlpha`
  (graphicsLayer on the Box), dissolving background + artwork together
  into the app underneath. Same 450/650/450ms timings, same theme gate,
  same cold-launch-only guard. Also renamed the image animatable to
  `imageAlpha` (an unqualified `alpha` inside graphicsLayer{} resolved
  to the outer val and failed to compile).
- Build OK; tests 939 run, 8 failures = same env-broken baseline.

## 2026-10-14 ~11:30 IST — Breeze glass everywhere (NO APK until user says okay)
- User constraint: the glass must look EXACTLY like the supplied glass —
  nothing more, nothing less. So every conversion reuses `GlassCard`
  verbatim (same palette, 24dp shape, 1dp border, 6dp shadow). No new
  glass variant, palette, blur, tint, or effect was invented.
- Converted (container surfaces/cards/rows -> GlassCard; all behaviour,
  text, icons, click handlers, semantics, navigation, data unchanged):
  Settings (SettingsRow, Accounts rows, PrivacyPoint, AppearanceSection,
  AboutSection), Rules (RuleCard, ConditionRow, ActionRow, PreviewCard,
  ConflictBanner), AI settings (toggle row, provider cards, remote
  consent card, privacy audit card, CapabilityRow, StatusCard),
  Cleanup (notice, group, newsletter cards), Privacy Center (guarantee
  banner, PillarRow, account, inventory, permissions, security cards),
  Integrations (OfflineBanner, IntegrationRow, unavailable card),
  Analytics (FreshnessHeader Surface + all 7 cards incl. InsightCard),
  Automation (rule + history cards, editor Trigger/Condition/Action/
  Preview cards), Connect (centred content in GlassCard, transparent
  root kept), Import (intro + result content in GlassCard).
- Deliberately LEFT UNTOUCHED (no glass variant invented for them):
  StatusPill badges (Automation + Integrations), SensitivityBadge
  (Privacy Center), GroupReviewDialog candidate cards (inside a dialog),
  ImportAnimationContent (full-screen flying-mail animation theater —
  carding it would reframe the flight geometry), FoundationScreen +
  PlaceholderScreen (no content cards), StartupErrorScreen (crash
  diagnostic, readability first), all dialogs, text fields, switches,
  buttons, segmented controls, chips, tabs, FABs, snackbars.
- Notes: ConflictBanner/StatusCard/InsightCard lost only their container
  tint/border; their warning signal stays via icon + tint + text
  (e.g. InsightCard icon keeps its severity tint). Provider-card
  selection stays via Check icon + bold text. Nested clickables
  (Switch/IconButton inside clickable GlassCards) behave as before —
  inner control consumes its tap.
- One self-caught bug during the pass: an extra `}` in AiSettingsScreen
  provider card (Card->GlassCard there is 1:1 in braces); fixed, all
  files brace-balanced. Build OK; tests 939 run, 931 pass, same 8
  env-broken Robolectric classes (data.local / repository / sync).

## 2026-10-14 ~11:55 IST — Glass everywhere + Mailstack 1.0.12 APK
- GlassCard (exact existing look, Glass.kt untouched) extended to all
  remaining screens: Settings rows/Accounts/Privacy/Appearance/About,
  AI settings, Rules, Cleanup, Privacy Center, Integrations, Analytics,
  Automation list + editor, Connect, Import. Dialogs, fields, switches,
  buttons, chips, tabs, FABs, StatusPill badges, and StartupErrorScreen
  untouched. Build OK; tests 939 run, 931 pass, same 8 env-broken.
- Version bumped to 1.0.12 (versionCode 13) for the APK delivery.

## 2026-10-15 ~10:00 IST — 1.0.12 audit fix batch (1.0.13 APK)
User-reported defects from the 1.0.12 screenshots, all fixed:
1. Excess space: row tokens back to Gmail parity (mail row 120->88dp,
   avatar 54->40dp, company row 86->64dp, thread header 65->48dp,
   search row 97->72dp); ThreadRow vertical padding 12->8dp; list gaps
   12->8dp on Mail/Categories/Companies/Search.
2. Dark-mode text unreadable: GlassCard now provides an explicit
   content color (theme onSurface). Previously the custom translucent
   fills left content color unspecified, so bare Text fell back to
   black — correct-looking on light cards, invisible on dark cards.
3. Ghosting white stripe/lines: no white surface exists anywhere in
   the row code (verified by audit + pixel measurement of the
   screenshots). The artifact came from the Surface compositing path
   for translucent elevated cards. GlassCard is reimplemented as
   explicit layers — shadow, fill, border — with IDENTICAL values
   (same fill, 1dp border, 24dp shape, 6dp shadow), plus the explicit
   content color above. The glass look is unchanged.
4. Settings lines: ALL HorizontalDividers removed from the secondary
   screens (Settings, Accounts, Privacy, AI, Rules, Integrations,
   Cleanup, Analytics, Automation, Privacy Center) — cards separate
   sections now; nothing left to render as a line across a card.
   Drawer dividers + email-content VerticalDivider untouched.
- Version bumped to 1.0.13 (versionCode 14) for the APK delivery.

## 2026-10-16 ~15:30 IST — "Mailstack Expressive" redesign (1.0.14 APK)
User verdict on Breeze: the glass UI looked disturbing. Complete redesign
in the Material 3 Expressive (Pixel) language, per user approval:
- REMOVED: cold-launch splash animation (both modes, deleted
  SplashScreen.kt + splash drawables + MainActivity wiring) and the
  full-bleed photo background (deleted BreezeBackground + artwork +
  assets/backgrounds backups). App opens straight into content.
- REMOVED: all glass — Glass.kt deleted; GlassCard -> MoCard, a solid
  tonal surfaceContainer card, 28dp radius, no border, no shadow.
  Mail/search rows are flat rows on the background (Gmail-style), not
  cards. Cards reserved for grouped content (Home, Settings, messages).
- NEW color: vivid violet brand (#5238E8 light / #B9A8FF dark), fixed
  palettes in both modes; DYNAMIC COLOR OFF (wallpaper palettes broke
  uniformity). Dark mode is deep violet-tinted charcoal (#131120 bg,
  #1A1826 surface) — never pure black — with near-white lavender text.
- Expressive type (display up to 64sp) + shapes (cards 28dp, pill
  buttons/chips, 24dp FAB). Nav bar: solid surfaceContainer with a
  secondaryContainer pill indicator. Search: solid surfaceContainerHigh
  pill. Scaffolds/top bars: solid background/surface (no transparency).
- Warning pills: solid amber containers, no borders. Dividers stay
  removed (1.0.13). 1.0.13 fixes (compact rows etc.) are included in
  this tree; the unapproved 1.0.13 push must NOT be approved after
  this — it would revert redesigned files.
- Version bumped to 1.0.14 (versionCode 15).

## 2026-10-16 ~16:00 IST — Tab-flicker fix + Material You dynamic color (1.0.15 APK)
User report (screenshots): white/gray flash + reload feeling on tab
switches; asked for wallpaper-adaptive theming like Pixel UI.
- Root cause of the flash: the NavHost fade-through (exit 200ms +
  enter delayed 120ms) left a gap where both screens were
  semi-transparent, exposing the platform window background — and the
  platform theme was Theme.Material.LIGHT (white bg, no night
  variant). Same cause as the 2026-10-12 Connect-screen incident.
- Fix: primary tab switches now use NO transition (instant, like
  Gmail/Pixel) — no gap, no flash, no reload feeling. Tab state was
  already preserved via saveState/restoreState; only the animation
  made it feel like a reload. Pushed routes keep the fade-through.
- Window background is now synced to the active scheme's background
  (SideEffect in MailOrganizerTheme), so no white backdrop can ever
  show through. (A DayNight platform theme was tried and reverted —
  DayNight is AppCompat-only, not a platform style; the Compose sync
  covers the first frame.)
- Material You dynamic color ON (reverses the 1.0.14 off decision, per
  explicit user request): Android 12+ uses dynamicLight/DarkColorScheme
  from the wallpaper; below 12 the violet palettes are the fallback.
  ThemeMode.SYSTEM follows the system (system-dark -> dynamic dark).
- Version bumped to 1.0.15 (versionCode 16).

## 2026-10-16 ~16:35 IST — OAuth "Continue loops back to Chrome" fix (NO APK — user approval pending)
User report: Connect -> Chrome sign-in -> app shows "Connected as
<email>" + Continue -> tapping Continue sends them back to Google
sign-in in Chrome instead of staying in the app.
- Root causes (all in the Connect flow):
  1. MainActivity never consumed the OAuth redirect intent: the stored
     intent survives Activity recreation, so a later onCreate re-handled
     the same single-use authorization code; the re-exchange failed
     (invalid_grant, code already used) and flipped the UI
     Success -> Error -> "Try Again" -> back to Chrome.
  2. handleOAuthResult had no dedup: a twice-delivered redirect was
     exchanged twice.
  3. ConnectAccountViewModel's 2s onForegrounded reset flipped
     Connecting -> Idle while the token exchange was still running, so
     the UI showed "Connect with Google" mid-exchange and invited a
     second tap that opened Chrome again.
- Fix (5 files): MainActivity consumes the redirect (setIntent(empty)
  after an OAuth redirect; non-OAuth intents untouched);
  GoogleAuthRepository dedups identical (state, code) deliveries under
  a Mutex and returns the first handling's result (dedupKeyFor is a
  pure, unit-tested function); Connect flow drops the timeout reset —
  the Connecting state now has an explicit Cancel button (reset()).
- Version NOT bumped and NO APK built — user said no APK until they
  say okay. When they approve, bump (1.0.16) + build + deliver.

## 2026-10-16 ~16:50 IST — Gmail-style infinite scroll (NO APK — user approval pending)
User report: the mail list ends with a "Load more" button; they want
Gmail behavior — automatic loading, no button, no waiting at the end.
- New behavior: when the user scrolls past the halfway mark of the
  loaded list, the next page starts loading, so it is already there by
  the time they reach the end. A small "Loading more…" spinner shows
  at the list end while a page is in flight. Page size stays 50
  (bounded paging preserved); the 50% mark is the trigger, not a
  doubling page size.
- Threads list: "Load more" button removed; LazyColumn gains
  listState + snapshotFlow prefetch trigger (shouldPrefetchMore, pure
  + unit-tested). Messages lists (Starred/Promotions/Social/Spam/
  company) gain the same trigger + a hasMore the VM never exposed —
  they used to stop silently at the first 50 rows with no way to see
  more.
- MailboxContent.Threads/Messages now carry the limit they were built
  for. loadMore() is guarded: no-op when hasMore is false or when a
  bigger page is already being fetched (visible limit != requested
  limit), so rapid scroll events can't stack requests. isLoadingMore
  is derived from content limit vs requested limit — no extra flag.
- Tests: 7 threshold tests + 3 VM tests (triple loadMore = one bump;
  exhaustion stops paging; starred paginates). Version NOT bumped, NO
  APK built — user said no APK until they say okay.

## 2026-10-16 ~17:05 IST — Jelly-bubble bottom-nav animation (NO APK — user approval pending)
User asked for an animated bottom-bar bubble: the highlight should
slide to the newly selected tab with a squeeze + jiggle, like a
toggle-switch ball being pulled from one side to the other, and the
page switch should stay in sync with it. Researched the references:
squash-and-stretch (Disney's 12 principles — stretch along travel,
squash on momentum change) driven by underdamped spring() +
Animatable, with the icon tint following the pill.
- MoNavBar rewritten: ONE pill (Material 3 active-indicator size,
  64x32dp, secondaryContainer) instead of a per-item indicator. Its
  left/right edges are two independent Animatables: the leading edge
  runs a stiff spring (stiffness 550, fast start), the trailing edge a
  softer one (300) so it lags — that lag stretches the bubble
  mid-flight. Both dampingRatio 0.70, so the bubble overshoots
  slightly and shakes into place. Vertical squash is derived from the
  stretch (wider -> flatter, min 0.80). The icon tints the moment the
  bubble covers it, so bubble + icon read as one unit.
- The bar is hoisted ABOVE the NavHost in AppNavGraph (one instance,
  shown only on the 5 primary destinations; pushed routes keep the
  full screen). Per-screen bars remount on every tab switch, so their
  pill could only snap, never slide. The 5 screens lost only their
  bottomBar block (their onNavigate params stay — used elsewhere);
  MailScreen's now-unused onNavigatePrimary param was removed.
- Page switch stays instant (the 1.0.15 no-flicker fix) and fires on
  the same tap as the bubble — in sync, no lag, no slide.
- Tests: 8 new MoNavBarTest tests for the pure math (edges, spring
  stiffness ordering, squash + clamp, icon-cover rule). Compose
  2026.06.01 (ui 1.12.1) has NO positionInParent/boundsInParent on
  LayoutCoordinates — item centres are computed from the measured bar
  width (5 equal-weight items).
- Version NOT bumped, NO APK built — user said no APK until they say okay.

## 2026-10-16 ~17:15 IST — Appearance jelly bubble + Analytics chip fix (NO APK — user approval pending)
Two user-reported items, one push.
1) Appearance redesign (screenshot): the System/Light/Dark segmented
control gets the SAME bubble as the bottom nav. The bubble physics
was extracted into a shared component so both places are identical
by construction: ui/components/JellyBubble.kt (JellyBubbleState +
JellyBubble composable + pure helpers). MoNavBar now renders through
it; behaviour unchanged. The Appearance control is a custom capsule
(48dp, 1dp outline border, no dividers — a sliding thumb + divider
lines fight each other, every sliding-thumb reference drops them):
the pill floats 4dp inside, slides with squash-stretch, and the
checkmark + label tint switch the moment the bubble covers the
segment (checkmark slot always laid out so the label never jumps).
The theme itself still flips instantly on tap; the pill animates
over the new theme's colours. Stock SegmentedButton removed.
Tests: MoNavBarTest keeps a destinations-order test; the bubble
tests moved to JellyBubbleTest (8 moved + 2 new segment-geometry
tests). No MCP delete tool exists, so the old test file stays as a
real destinations test instead of a dangling reference.
2) Analytics & Insights chip fix (screenshot): the "All
synchronized" range chip wrapped into 3 lines ("All / synchroniz /
ed") because the fixed Row squeezed all four chips into the screen
width. That label also read as a sync status, not a time range.
Fix: renamed to "All time" (matches Today / Last 7 / Last 30 days;
the ViewModel comment already called it that) and the chip row now
scrolls horizontally, with labels pinned to one line so a chip can
never stack its text. Meaning: stats for all mail ever synced.
- Version NOT bumped, NO APK built — user said no APK until they say okay.

## 2026-10-16 ~17:35 IST — Mailstack 1.0.16 delivered (user said "give me the apk")
APK: Mailstack-1.0.16.apk, 17,091,132 bytes, package
com.greninjaop.mailorganizer, versionName 1.0.16, versionCode 17,
stable cert SHA-1 07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05.
Contents (all pushed + byte-verified before the build): Gmail-style
infinite scroll (no Load more button, prefetch at halfway, footer
spinner); jelly-bubble bottom-nav indicator + Appearance picker
(shared squash-stretch pill); Analytics range chip "All
synchronized" -> "All time" + scrolling chip row. Tests at build
time: 965 run / 957 pass (same 8 env-broken). No on-device
verification possible from the sandbox — user confirms on phone.

## 2026-10-16 ~17:40 IST — Bottom bar geometry fix (NO APK — user approval pending)
User screenshot from 1.0.16: bottom-bar icons looked bigger than
before and the bar sat on top of the Android system navigation
buttons, colliding with taps. Root cause: in the MoNavBar rewrite,
navigationBarsPadding() was placed INSIDE the fixed 80dp height, so
the system-nav inset squeezed the bar's content and the whole bar
slid down onto the system buttons (Compose doesn't clip, so the
squeezed icons spilled and looked oversized). Fix: the padding is
now OUTSIDE the fixed height, exactly like Material 3
NavigationBar's windowInsets — total bar height 80dp + nav inset,
content clear above the system nav. Icon size (24dp), pill
(64x32dp), label, and elevation (0dp) were already identical to
the old bar; only the geometry was wrong. Animation untouched.

## 2026-10-16 ~17:45 IST — Square tap ripple removed from pill controls (NO APK — user approval pending)
User screenshot (Appearance, Dark tapped): besides the grey pill, a
square highlight appeared on tap. That was the default clickable
ripple — a rectangle filling the tapped segment/tab. The user wants
only the pill. Fix: both custom pill controls (MoNavBar tabs and
the Appearance segmented control) now use clickable(indication =
null) — the sliding pill itself is the tap feedback. Standard M3
components elsewhere keep their normal ripples; only these two
pill controls changed.

## 2026-10-16 ~17:50 IST — Mailstack 1.0.17 delivered (user said "give me apk")
APK: Mailstack-1.0.17.apk, 17,091,132 bytes, package
com.greninjaop.mailorganizer, versionName 1.0.17, versionCode 18,
stable cert SHA-1 07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05.
Contents: bottom-bar geometry fix (nav-bar padding outside the
fixed 80dp height — bar sits clear above the system nav buttons);
square tap ripple removed from the pill controls (nav tabs +
Appearance picker). Tests: 965 run / 957 pass (same 8 env-broken).
Verification note: the first 1.0.17 builds looked stale because
only the primary classes.dex was compared — the app's classes live
in secondary dex files (multidex). The delivered APK was verified
per-dex: 5 secondary dex files differ from 1.0.16, and MoNavBarKt
in classes8.dex references the new clickable overload. Lesson:
compare ALL classes*.dex, never just classes.dex.
No on-device verification possible from the sandbox.
## 2026-10-16 ~18:15 IST — Dynamic top bar + directional slides + skeleton + 3 fixes (not yet in an APK)
Source push (no APK — user did not ask for one). Changes:
1. Hoisted top bar (MoPrimaryTopBar, above the NavHost like the bottom
bar): one search pill stays mounted across tab switches. On Mail the
hamburger slides in from the left and the pill shrinks rightward to
make room; leaving Mail it slides out left and the pill expands. The
pill never enters from the right. Same 350ms spec as the page slide
and the jelly bubble, so all three land together. Mail's pill edits
the mailbox filter; other tabs' pill holds a local query and opens
Search on submit. Mail's drawer is opened via a drawer-open signal
(the hamburger lives in the hoisted bar, the drawer in MailScreen).
MoAppTopBar deleted. The 5 primary screens lost their per-screen
top bars and set contentWindowInsets=0 (pushed routes keep theirs).
2. Directional tab slides: content enters from the side its tab sits
on (tab to the right slides in from the right, and vice versa),
opaque (no fade/scale) so the 1.0.15 flash cannot return. Pushed
routes keep fade-through.
3. Skeleton loading (Boneyard-style: skeleton mirrors the real rows'
geometry — same 88dp height, padding, 40dp avatar, 3 text lines —
hand-rolled shimmer, no library). Mail list now has 3 states:
content / empty / skeleton (loading).
4. Edge fade: mail list dissolves gently (28dp, subtle) at top+bottom
while scrolling, only where more content exists in that direction —
replaces the hard clip that read as a black bar cutting rows off.
5. Dark logos: in dark mode, artwork logos sit on a white circular
backing (black Apple logo now visible). Light mode unchanged.
Monochrome bundled logos keep their tonal circle.
6. Mail bubble lag: on a tab-switch entry to Mail, the heavy list
defers 300ms behind the skeleton so the bubble/slide/pill run
without competing against list composition. Popping back from a
thread shows the list immediately.
Tests: 968 run / 960 pass (same 8 known env failures: Robolectric
DB/sync tests needing network). New: TabSlideDirectionTest (3 tests).
No on-device verification possible from the sandbox.

## 2026-10-16 ~18:18 IST — Mailstack 1.0.18 delivered (user: "rename the APK properly, increment after each release")
APK: Mailstack-1.0.18.apk, 17,107,516 bytes, package
com.greninjaop.mailorganizer, versionName 1.0.18, versionCode 19,
stable cert SHA-1 07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05.
Contents (all pushed earlier same day, byte-verified): hoisted
dynamic top bar (search pill reshapes as the hamburger slides in
from the left on Mail); directional tab slides (content enters from
its tab's side, opaque, 350ms, synced with the bubble); skeleton
loading for the mail list (content / empty / skeleton); subtle
top+bottom edge fade on the mail list; white circular backing for
artwork logos in dark mode; Mail list defers 300ms past tab-switch
transitions so the bubble never lags. Tests at push time: 968 run /
960 pass (same 8 known env failures). APK verified: aapt badging
(1.0.18/code 19), apksigner cert SHA-1 matches the stable key,
MoPrimaryTopBar + edgeFade present in DEX, 6 secondary dex files
differ from 1.0.17. Naming rule restated: every release increments
the version — never reuse a version name.
No on-device verification possible from the sandbox.

## 2026-10-16 ~18:35 IST — reference.html created (animation stock + living design reference)
New file at repo root: `reference.html`. Purpose: the user no longer
re-explains animations — they say "Use the bubble animation from
reference.html" and the agent reads the specs here. Contents:
(1) Bubble Animation with the Flow — live clickable 5-tab demo
(two-edge spring jelly: leading 550 / trailing 300 / damping 0.70),
annotated SVG diagram with hand-drawn red arrows, zoomed
frame-by-frame strip (start → stretch → mid-flight → catch-up →
settle), handwritten scribble notes, exact spec table.
(2) Scroll Edge Fade — live scrollable mail list demo (28dp fade,
only where more content waits), annotated diagram, zoomed frames,
scribble notes, spec table. (3) Design Reference — the whole UI:
layout wireframe, colours (violet #5238E8 / #B9A8FF, dark bg
#131120, surfaces), type & shape, components, full motion table,
standing rules. Maintenance rule (binding): when a component
changes in the app, update the Design Reference section; when a
component is removed from the app, remove it here too — but NEVER
remove an animation section, even if its button is gone. Verified:
JS syntax clean, 11/11 functional assertions pass in a Node harness (the earlier '12' counted the assert helper itself)
(pill settles on target, stretches mid-flight, fade masks correct at
top/middle/bottom), all 18 SVGs parse, CSS braces balanced. Live
browser render check could not run (browser blocks localhost);
verification was done via the harness + static checks instead.


## 2026-10-16 ~18:40 IST — reference.html moved into android/ + split into design-reference/
User: keep the page inside the Android folder, with a "design reference"
folder for its assets. New layout (repo-root reference.html DELETED):
- `android/reference.html` — the page (links to the assets below)
- `android/design-reference/reference.css` — all styles
- `android/design-reference/animations.js` — bubble spring + scroll-fade
Folder named `design-reference` (kebab-case, no space — cleaner for
URLs/paths) per the user's "or something like that". The page content
is unchanged; only the CSS/JS were extracted verbatim into the two
asset files. Re-verified after the split: 11/11 harness assertions
pass against the new animations.js; HTML has no inline style/script
blocks. Maintenance rule unchanged (Design Reference section tracks
the app; animation sections are never removed).


## 2026-10-16 ~19:15 IST — Two 1.0.18 on-phone fixes (user screenshots)
1) Search pill touched the screen edges when the hamburger retracted:
the hoisted top-bar Row had NO horizontal padding, so on non-Mail tabs
the pill started at x=0. Fix: `padding(horizontal = 16.dp)` on the
Row — the pre-hoist bar was a Material3 TopAppBar, which carries this
inset itself. Pill + hamburger now always sit 16dp in from both
edges, matching the older version. Reshape animation unchanged.
2) Drawer flickered open/closed when switching Mail -> Home: the
drawer-open signal was a StateFlow<Int> counter, and StateFlow replays
its last value to every new collector — so every time Mail re-entered
composition after any hamburger tap, the LaunchedEffect saw
signal > 0 and opened the drawer by itself. Fix: one-shot
SharedFlow<Unit>(replay = 0, extraBufferCapacity = 1); hamburger does
tryEmit(Unit); MailScreen collects in LaunchedEffect and opens only
on a fresh tap. reference.html updated too (top-bar 16dp margin +
one-shot drawer event). Tests: 968 run / 960 pass (same 8 known env
failures). No APK built (user did not ask for one).

- **2026-10-16 (reference.html §6 + standing animation rule):** STANDING RULE (user): whenever the user talks about ANY animation — complex or simple, built or just an idea, used in this project or not — the animation study goes into `android/reference.html` as a new section (live demo + annotated diagram + zoomed frames + scribble notes + specs). Animations studied there are a reusable library for this project or another project. Never skip adding it. Recorded in reference.html's header comment + MEMORY. New §6 "Scroll-Hide Bars — Top Slides Up, Bottom Slides Down": scroll down → top bar slides UP out + bottom bar slides DOWN out (hide: LinearOutSlowIn, fast→slow); scroll up → both slide back (show: FastOutSlowIn, slow→fast→land); pure translate, NO fade, no spring/overshoot/shake; 250ms; list expands to fill freed space; at scroll 0 bars always show. Studied in reference.html only — NOT yet implemented in the app. Harness: 25/25 pass. Pushed + byte-verified (reference.html, reference.css, animations.js).
- **2026-10-16 (Gmail-fidelity drawer, hoisted):** Drawer rebuilt to match the user's Gmail screenshots. One `ModalNavigationDrawer` hoisted above the whole Scaffold in `AppNavGraph.kt`, so the sheet + scrim cover the top bar and bottom bar too (Gmail) — the old per-screen drawer only covered content below the top bar. New `ui/mail/MailDrawer.kt`: 48dp rows, 14sp labels (labelLarge), 24dp icons, 12dp icon gap, 12dp horizontal inset, full-width selected pill (secondaryContainer, 28dp radius), compact account header (40dp avatar/icon, titleSmall + bodySmall) + one full-width divider, no gaps between rows. No trailing counts — no confirmed unread-count source; never fabricated. No section headers added — Mailstack has no label sections. Rows built from M3 primitives with M3 tokens (NavigationDrawerItem fixes 56dp and offers no padding param — deliberate, documented deviation for Gmail density). Icons stay Filled (material-icons-core has no Outlined set; adding extended was not asked for). Hamburger opens via `DrawerState` directly (SharedFlow event flow removed — it replayed and reopened the drawer on tab switches). Edge-swipe is Mail-only (`gesturesEnabled = isMail`) so the mailbox drawer can't open from another tab. `MailScreen.kt`: drawer + dead imports removed, `modifier` wired into Scaffold, doc comment fixed. reference.html drawer entry updated. Compile green; tests 968 run / 960 pass (same 8 known env failures). All 4 files pushed + byte-verified. No APK built (user did not ask for one).

- **2026-10-16 (Mailstack 1.0.19 APK delivered):** `Mailstack-1.0.19.apk`, 17,107,516 bytes, versionCode 20, stable cert SHA-1 07:31:05:EF:…:7D:05 (same byte size as 1.0.18 by coincidence — verified as a new build: manifest says 1.0.19/code 20, MailDrawerKt present in classes10.dex). Contains: Gmail-fidelity hoisted drawer (covers top + bottom bars, 48dp rows, 14sp labels, selected pill), top-bar 16dp edge margin, DrawerState hamburger (no SharedFlow), plus everything in 1.0.18 (hoisted top bar, directional tab slides, skeleton, edge fade, dark logo backing, 300ms Mail defer). NOT in the app yet: scroll-hide-bars (§6) — studied in reference.html only. Build green; tests 968 run / 960 pass (same 8 known env failures). versionCode/versionName bumped in app/build.gradle.kts + pushed.

- **2026-10-16 (drawer auto-open + freeze fix, 1.0.20 APK):** 1.0.19 bug (user-reported): entering Mail auto-opened the drawer; leaving Mail froze the app (scrim stuck, taps blocked). ROOT CAUSE: the hoisted drawer's content was conditionally composed (`if (mailState != null)`). ModalNavigationDrawer's anchors derive from the sheet's measured width — entering Mail made the width jump 0 → ~320dp, so the closed offset (0, when empty) mapped to the OPEN anchor (auto-open); leaving Mail collapsed the anchors, the offset went out of bounds, and the scrim stuck over the screen. FIX (AppNavGraph.kt): drawer content is ALWAYS composed — `MailDrawer(state = mailState ?: MailScreenState())` — so the width/anchors never change; the drawer now only opens on a hamburger tap or edge swipe. Plus a defensive `LaunchedEffect(isMail) { if (!isMail) drawerState.close() }`. Standing lesson: NEVER conditionally compose ModalNavigationDrawer content. `Mailstack-1.0.20.apk`, 17,107,516 bytes, versionCode 21, stable cert, sha256 6e0254c4…. Compile green; tests 968 run / 960 pass (same 8 known env failures).

- **2026-10-16 (skeleton only on true first load, 1.0.21 APK):** User: skeleton showed on EVERY Mail tab entry. Cause: a 300ms `deferList` (added to protect the bubble animation) held the skeleton on every tab-switch entry even though the mailbox was already cached in the ViewModel. Fix: removed the defer entirely — `deferList` param, `listReady` state, `mailEnteredViaTab`/`previousRoute` tracking, and the 300ms delay are all gone (MailScreen.kt, AppNavGraph.kt). Skeleton now shows only when content is `Loading` (nothing cached yet = true first load); cached tab switches show the list immediately. reference.html "Mail list on tab entry" row updated. `Mailstack-1.0.21.apk`, 17,107,516 bytes, versionCode 22, stable cert, sha256 12c1752b…. Compile green; tests 968 run / 960 pass (same 8 known env failures).

- **2026-10-16 (Instagram-style pull-to-refresh, 1.0.22 APK):** User (with screenshot): wanted Instagram's threshold-sensitive pull-to-refresh on the mail list — pull down, "release" signal, smooth easy pull, release reloads the whole page — and the top-bar reload button removed; plus the top-right envelope icon was confusing (should be the accounts section with the account's DP). Fixes: (1) one hoisted PullToRefreshBox over the whole mailbox (every state — list, empty, error, skeleton — can pull) with a custom pill indicator: filling arc + "Pull to refresh" / "Release to refresh" / "Refreshing…" (alpha follows the pull, invisible at rest), release past the threshold reloads the full mailbox. (2) Top-bar reload button removed — the gesture is the reload. (3) Top-bar account button now shows the account's DP in every mode: unified inbox shows the DEFAULT account's DP (earliest created) instead of the envelope icon; no accounts → envelope kept as the Accounts entry point. reference.html §7 study added (draggable demo + diagram + frames + notes + specs) and §5 top-bar/pull rows updated. `Mailstack-1.0.22.apk`, 17,107,516 bytes, versionCode 23, stable cert, sha256 458506a2…. Compile green; tests 968 run / 960 pass (same 8 known env failures).

- **2026-10-16 (Gmail-style account switcher sheet, 1.0.23 APK):** User (with Gmail screenshots): wanted the top bar to look like Gmail's — no three-dots menu on the other tabs (DP moves to the far right, pill resizes into the freed space) — and tapping the DP to show Gmail's account sheet (collapse arrow, account list, Add + Settings at the bottom). Fixes: (1) NEW AccountSwitcherSheet (ModalBottomSheet, any tab): header = current selection (single account, or "All Accounts" with the default DP for unified) + collapse arrow; the list shows the OTHER accounts (current is the header, never repeated) + an "All Accounts" row when a single account is current; tapping a row switches the whole app (same active-selection preference the Accounts screen writes) and closes the sheet. Collapse is animated (expand/shrink + fade), opens expanded like Gmail. Bottom: "Add another account" (→ Connect flow) + "Settings" (→ Settings page). Gmail's "Manage accounts on this device" / "Manage your Google Account" rows NOT copied (Google-Account features we don't have). No unread counts on the rows (no real source — never fabricated). (2) Three-dots overflow menu removed from ALL tabs (Gmail parity; the user's "three dots only on Mail" read as the hamburger/three-lines, which stays Mail-only — the overflow's destinations were already reachable inside Settings: Accounts, Privacy, Integrations, Rules, Cleanup, Analytics, AI, Automation). reference.html §8 study added (interactive demo + diagram + frames + notes + specs) and §5 top-bar rows updated. `Mailstack-1.0.23.apk`, 17,140,284 bytes, versionCode 24, stable cert, sha256 075caca1…. Compile green; tests 968 run / 960 pass (same 8 known env failures).

## 2026-10-16 (late) — Mailstack 1.0.24 (DELIVERED): DP uniformity + demo accounts never sync + Settings panel replaces bottom sheet

Three fixes in one release (`Mailstack-1.0.24.apk`, 17,140,284 bytes, versionCode 25, stable cert SHA-1 07:31:05:EF:…:7D:05, sha256 b42b0e6f…).

1. **Top-bar DP: one uniform 40dp on every tab.** Mail used 32dp (MoSpacing.xxl) while the other tabs fell back to the avatar's 24dp (MoSpacing.xl) default, so the DP visibly enlarged on Mail and shrank elsewhere. One shared `avatarSize = MoSpacing.xxxl` (40dp = the mail row avatar's size) is now used in every branch (Mail single, Mail unified, non-Mail, plus the no-accounts envelope fallback). The user's "50% bigger" estimate assumed Mail was ~27dp; 40dp matches the email avatars exactly (32dp x 1.5 = 48dp would have overshot them), so the email-avatar size won. reference.html §5 Top bar row updated.

2. **Demo accounts never sync; refresh finishes immediately when nothing is syncable.** The seeded fixture accounts (`fixture-acct-alex`, `fixture-acct-priya`) + Add-Local test accounts (`local-*`) were enabled + permanently "stale" (never successfully synced), so every app start / resume / connectivity recovery AND every manual refresh attempted a sync for them — "refreshing every time". New `AccountRecord.isDemoAccount()` (id starts with `fixture-` or `local-`; real Google accounts, even TOKEN_EXPIRED ones, never match) is excluded in: `BatteryConsciousSyncScheduler.syncAllEnabled` + `checkAndSyncStaleAccounts` + `syncAccountNow` (fast no-op Success), `AccountSyncWorker.doWork` (fast no-op Success — never Failure/Retry, so WorkManager won't retry a sync that can never succeed), and `MailViewModel.refresh()` (fast path before any spinner: demo-only → the same "Gmail isn't connected yet — sync will work once you connect an account." guidance the old NotConfigured failure gave; single demo account → same message) + `syncAllAccountsManually`. The existing `MailViewModelTest` ("refresh without gmail connection reports honestly") pins that message, so the fast path reuses it rather than inventing a "Demo data" message. reference.html §7 "Demo accounts" row added.

3. **Settings panel replaces the bottom-sheet account switcher.** The user rejected the 1.0.23 bottom sheet: the panel must come from the RIGHT side and FILL the screen, and it must BE the whole Settings page (not a "Settings" button leading to a separate page). `AccountSwitcherSheet.kt` deleted (fix = remove). Tapping the DP on any tab now navigates to the SETTINGS destination, which slides in from the right / out to the right, full-screen (`MoMotion.panelSlide`, 350ms FastOutSlowIn; per-destination transitions on SETTINGS + Settings-aware NavHost globals: the source stays put while the panel slides over it; drilling deeper, the panel exits left and the deeper screen enters from the right; popping back reverses it). Top of the panel = the expandable account section (`AccountSection` in SettingsScreens.kt): collapsed it is a single layer — the current account (DP 40dp + name + email; unified → default DP + "All Accounts"), NO "Accounts" label (the account itself IS the section, like Gmail's header); tapping the header/chevron expands the signed-in list inline (expand/shrink + fade, pushing the settings below down): "All Accounts (Unified Inbox)" when a single account is current, the other accounts (DP 32dp + name + email), "Add an account" (→ Connect). Tapping an account switches the app's selection and closes the panel. Then a divider, then the unchanged settings rows (Privacy, Rules, Integrations, Cleanup, Analytics, AI, Automation, Appearance, About). No "Manage your Google Account" / storage rows, no unread counts. `MoPrimaryTopBar.onOpenAccounts` renamed to `onOpenSettings`. reference.html: §5 rows updated, §8 marked superseded (retained as the reusable bottom-sheet animation study — animation sections are never removed), §9 new Settings-panel study (live demo + diagram + frames + notes + specs).

Compile green; tests 968 run / 960 pass (same 8 known env failures: AccountDaoTest, AccountIsolationTest, MailDaoTest, MigrationTest, MultiAccountUnifiedInboxTest, SearchIndexStoreTest, RepositoryTest, SyncCoordinatorTest). All changed files byte-verified against remote; sheet deletion confirmed. No on-device verification (sandbox has no emulator / phone access).

## 2026-10-16 (late) — Integrations: Gmail row removed; real Google product logos; no letter badges (in main, NOT in an APK yet)

User's logic: the app IS a Gmail app — signing in IS Gmail — so Gmail must not appear as an "integration" needing a separate connection. Fixed in main; no APK built for this (user's standing rule: APK only on explicit request).

- **Gmail row removed.** `AppContainer.integrationManager` now registers only Calendar + Tasks. `GmailIntegrationAdapter` is unwired (kept, with a KDoc note + its adapter tests, as Gmail's honest capability/status model — it is not shown as an integration row). `IntegrationsViewModelTest` updated: registry holds 2 integrations, asserts no Gmail snapshot, Gmail-connect test removed (1 test fewer: 967 run / 959 pass, same 8 env failures).
- **Real product logos, no letters.** The old `IntegrationBadge` was a circle with the integration's first letter ("G"/"C"/"T"). Now each row (list + detail header) shows the integration's ORIGINAL product logo, resolved by product domain through the same logo pipeline mail rows use (bundled → ICONS-ASSETS → favicon at 128px): `IntegrationId.logoDomain()` maps Calendar → `calendar.google.com`, Tasks → `tasks.google.com`. A future integration adds one line there and its real logo follows. `CompanyLogo` gained `showLetterFallback = false` (integrations use it): while the logo loads / can't be fetched, the fallback is a plain tonal circle — NO letter, ever. Mail rows keep their letter-avatar fallback (unchanged default).
- **Auth model, verified from code (this is the answer given to the user's sign-in questions):** Sign-in IS a connection to Google itself — OAuth 2.0 in the system browser (Google's sign-in page; the app never sees the Google password). Sign in once per account; tokens persist encrypted; the app does NOT ask you to sign in every time you open it. With multiple accounts, you add each once (Connect → Google's account chooser → pick → consent); switching is via the DP panel, no re-login. Initial sign-in requests ONLY `gmail.readonly` + openid/email/profile. Calendar (`calendar.events`) and Tasks (`tasks`) scopes are requested INCREMENTALLY: enabling Calendar/Tasks opens a Google consent screen for JUST that permission on the SAME account (no password, no new account, `include_granted_scopes=true` keeps the Gmail grant) — the app cannot silently grant itself those scopes, Google requires one explicit Allow. After that, the same stored per-account tokens cover Gmail + Calendar + Tasks. "Installing" an integration = tapping Connect in Settings → Integrations (no separate app to download — Calendar/Tasks are built-in API integrations, not separate apps).

## 2026-10-16 (late) — Integrations logos + DP flicker/centring + Settings footer (in main, NOT in an APK yet — user: batch all bug fixes into one APK at the end)

1. **Integrations: Gmail row removed; real product logos.** `AppContainer.integrationManager` registers only Calendar + Tasks. `GmailIntegrationAdapter` unwired (kept + tested as Gmail's honest capability/status model). `IntegrationBadge` (letter circle "G"/"C"/"T") replaced by the integration's ORIGINAL product logo: `IntegrationId.logoDomain()` maps Calendar → `calendar.google.com`, Tasks → `tasks.google.com`, resolved through the mail-row logo pipeline (bundled → ICONS-ASSETS → favicon 128px). `CompanyLogo` gained `showLetterFallback = false` (integrations use it): while loading / unresolvable, a plain tonal circle — NO letter, ever. Future integrations add one `logoDomain()` line. `IntegrationsViewModelTest` updated (2 integrations, no Gmail snapshot).

2. **Top-bar DP: no flicker; centred.** Root cause of the flicker: the DP sat inside `AnimatedContent(targetState = isMail)` with a fade crossfade, so it faded out/in on EVERY tab switch even though the avatar never changed (both branches resolved to the same account: unified → default/earliest, single → selected). Fix: one `AccountAvatar` from the single `account` source on every tab, no animation; the `mailState` param is gone from the top bar. Centring: the DP now sits in a fixed 64dp trailing region spanning pill-end → screen edge, centred inside it (Row's right padding removed; pill width unchanged: old 48dp button + 16dp padding = 64dp region). Gap pill → avatar = gap avatar → screen edge = 12dp each side (was 4dp / 20dp). The nested `clickable` inside the IconButton (double handler) is gone too.

3. **Settings: Gmail-style footer at the very bottom.** Small centred links "Privacy Policy • Terms of Service" (each opens its document) + a small centred About paragraph ("Mailstack — your Gmail, organized on your device.") beneath. The About card is removed from the Settings list. The Privacy Center row is KEPT — it is functional (data inventory, OAuth scopes, security hardening, delete-account / clear-all), not a policy document. The dead `PrivacyScreen` doc (no callers) became `PrivacyPolicyScreen` (title "Privacy Policy"); new `TermsScreen` + `PRIVACY_POLICY` / `TERMS` destinations. Both drill from Settings, so the NavHost's global transition slides them in from the right. The weighted spacer pushes the footer to the bottom when content is short. reference.html §5 + §9 updated.

Compile green; tests 967 run / 959 pass (same 8 known env failures). All changed files byte-verified. No APK (user: wait until all bugs are fixed, then one APK).

## 2026-10-17 — Settings-panel jump fixed (overlaid bars) + WhatsApp shift + scroll-hide bars built + "Action required" chip removed (in main, NOT in an APK yet — user: batch all bug fixes into one APK at the end)

1. **Settings-panel jump FIXED — root cause was structural, not the panel.** Opening Settings made `showBars` false, so the outer Scaffold dropped its top + bottom bars, the Scaffold's content padding changed, and the whole NavHost re-laid-out — the primary page jumped UP (search box vanished, content moved to the top). Fix (AppNavGraph.kt): the Scaffold is GONE. A full-screen `Box` holds a full-screen `NavHost` (never padded, never resized) + the bars OVERLAID on top (AnimatedVisibility: fade + slide −1/12, MoMotion.panelFade/panelSlide). Each primary destination pads itself (new `PrimaryContent`: top = statusBar + 64dp, bottom = navBar + 80dp — animated 0..1 by the scroll-hide offset). Pushed routes (Settings, thread, …) are full-screen with their own top bars and do NOT take the primary padding. Bar visibility changes can now never move content. The drawer still wraps the whole Box, so the sheet + scrim cover the bars too.

2. **WhatsApp shift (§10).** While the panel slides in from the right (full width, 350ms FastOutSlowIn), the underlying primary screen slides slightly LEFT — 1/12 of the width, same 350ms spec, so they land together. No fade, no scale on the underlying page — pure slide. Closing: the panel slides out right, the page slides back to its exact original spot. The bars shift with the page (they live in the same AnimatedVisibility). NavHost globals: exitTransition (primary → SETTINGS) = slideOut { −width/12 }; popEnterTransition (SETTINGS → primary) = slideIn { −width/12 }. Deeper drills (Settings → Privacy, …) keep their own full-width slides; the 1/12 shift applies ONLY to the primary → Settings step. reference.html: new §10 study (live demo + diagram + frames + notes + specs); §9 Panel spec updated; §5 Settings panel + Motion rows updated.

3. **Scroll-hide bars BUILT (§6 — was a study, now in the app).** Scroll DOWN → top bar slides UP out + bottom bar slides DOWN out; scroll UP → both slide back. Pure translate, NO fade. Hide = LinearOutSlowIn (fast→slow), show = FastOutSlowIn (slow→fast→land), 250ms. The list expands into the freed space (PrimaryContent padding animates 0..1 with the bars), so the edge fade reaches the screen's very top / bottom. Entering a tab always shows the bars. Implementation: one NestedScrollConnection on the outer Box observes the scroll direction of whatever list is inside the NavHost (it never consumes — the list still scrolls normally); a single 0..1 offset (updateTransition, 250ms, per-direction specs) drives BOTH the bars' slide-out offsets (top = −(statusBar+64dp), bottom = +(navBar+80dp)) AND the content padding. reference.html §6 marked "In the app since 2026-10-17" + fade-to-edges + mechanism; §2 + §5 updated.

4. **"Action required" chip REMOVED from the Mail list (user: "I don't even know what the purpose of that button is" — the Actions tab covers it).** Removed: the `ActionRequiredFilterChip` call from MailScreen's header + the chip composable (PriorityVisuals.kt) + the whole Mail-screen filter (MailViewModel: `actionRequiredOnly` StateFlow, the pipeline combine, the `observeByCategory(ACTION_REQUIRED)` branch, `buildCategoryContent`, `toggleActionRequired()`, the state/ContentKey fields, the setDestination reset) + `EmptyKind.NO_ACTION_REQUIRED` + its empty state. The Search screen's own "Action required" filter is UNTOUCHED (different context — search filtering). The Actions tab / action-required classification / analytics counts are untouched. STANDING RULE (user, recorded here because it was stated as a rule): when the user says remove a UI element, the agent must automatically readjust everything that depended on its position — the fade, the spacing, the centring — without being told. Here: removing the chip lifted the list + its fade up to sit directly below the search box (nothing between the search box and the first row), and the fade now reaches the screen edges when the bars hide (§6). reference.html §2 (fade lifts + fade-to-edges) + §5 Filter chips row updated.

5. **DP centring rule recorded in reference.html (§3, rough figure).** The 40dp DP sits centred in a fixed 64dp trailing region: pill → DP = DP → screen edge = 12dp. The pill's width never changes to make room. §3 demo + diagram + specs updated to the current UI (the refresh + overflow icons were removed from the demo — the DP is the only trailing action on every tab; the §3 demo's action-group swap is gone). §5 wireframe updated (no more refresh/envelope glyphs — hamburger + pill + DP circle).

Compile green (from-scratch `--rerun-tasks`); tests 967 run / 959 pass (same 8 known env failures: AccountDaoTest, AccountIsolationTest, MailDaoTest, MigrationTest, MultiAccountUnifiedInboxTest, SearchIndexStoreTest, RepositoryTest, SyncCoordinatorTest). All changed files byte-verified against remote. No on-device verification (sandbox has no emulator / phone access). No APK.

## 2026-10-17 — Mailstack 1.0.25 APK delivered (versionCode 26)

User asked: "Release apk". Built `:app:assembleDebug` (debug build type, signed with the stable debug keystore — the OAuth client is bound to that cert's SHA-1, so the key never changes). versionName 1.0.25 / versionCode 26 (never reusing 1.0.24). Verified: package com.greninjaop.mailorganizer, label "Mailstack", signing cert SHA-1 07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05 (matches the stable key), 17,140,280 bytes, SHA-256 e00e39d1b287567ab4a86eae087467cf513d33266d70e61087f8e8e0ba5b27f9. Delivered as `Mailstack-1.0.25.apk` with sideload instructions. Contains: Settings-panel jump fix (overlaid bars), WhatsApp shift, scroll-hide bars, "Action required" chip removal, reference.html §10 + §6 implemented + §2/§3/§5 updates. On-device confirmation pending (sandbox has no emulator / phone access).
