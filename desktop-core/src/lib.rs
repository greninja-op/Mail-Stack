//! Mailstack shared desktop contracts (platform-neutral).
//!
//! Phases 02 + 07–25: persistence, credentials, OAuth/PKCE, Gmail adapter,
//! sync orchestration, mailbox presentation, intelligence, search, rules,
//! temporal/conversation, Action Engine, multi-account, background, privacy,
//! optional AI, automation, and capability queries.
//! macOS (and Linux plans) reuse this crate.

#![forbid(unsafe_code)]

pub mod account;
pub mod actions;
pub mod ai;
pub mod automation;
pub mod background;
pub mod cancel;
pub mod capability;
pub mod category;
pub mod classification;
pub mod company;
pub mod conversation;
pub mod error;
pub mod gmail;
pub mod html;
pub mod logo;
pub mod mailbox;
pub mod multi_account;
pub mod oauth;
pub mod pacing;
pub mod persistence;
pub mod platform;
pub mod priority;
pub mod privacy;
pub mod rules;
pub mod search;
pub mod sync;
pub mod sync_engine;
pub mod temporal;

pub use account::{AccountId, AccountScoped};
pub use actions::{
    ActionEngine, ActionProposal, ActionStatus, ActionType, CalendarService, DisconnectedCalendar,
    DisconnectedTasks, TasksService,
};
pub use ai::{
    ai_suggestion_badge, minimize_for_ai, summary_advisory_notice, validate_ai_output,
    wrap_prompt_boundary, AiCapability, AiRouter, AiSettings, AiStructuredOutput, DataMinimizer,
    MinimizedAiInput, MAX_BODY_CHARS, MAX_MODEL_CONFIDENCE,
};
pub use automation::{
    reject_email_authored_automation, AutomationActionKind, AutomationEngine, AutomationProposal,
    AutomationRule, AutomationTrigger, CIRCUIT_BREAKER_THRESHOLD,
};
pub use background::{
    build_notification, clamp_periodic_interval, should_run_lifecycle_work, user_message_for_failure,
    BackgroundScheduler, BackgroundWorkKind, BackgroundWorkRequest, Connectivity, FailureClass,
    MemoryBackgroundScheduler, NotificationDeduper, MIN_PERIODIC_INTERVAL, STALENESS_THRESHOLD,
};
pub use cancel::{CancelToken, Cancelled};
pub use capability::{
    base_sign_in_capabilities, AccountCapabilities, CapabilityRegistry, IntegrationCapability,
    PerformanceProfile, PerformanceTier,
};
pub use category::{CategoryGroupId, CategoryGroupKind, CategoryGroupModel};
pub use classification::{
    bind_result, classify_deterministic, ClassificationResult, ClassificationSignals,
};
pub use company::{
    company_key_from_sender, filter_keys_in_category, sort_companies_for_filter, CompanyKey,
    CompanyPinStore, CompanyRecord,
};
pub use conversation::{
    assess_conversation, is_follow_up_candidate, ConversationAssessment, ConversationSignals,
    ConversationState, FOLLOW_UP_CANDIDATE_DAYS, STALE_THRESHOLD_DAYS,
};
pub use error::{CoreError, CoreResult};
pub use gmail::{
    classify_provider_error, get_message_never_skip, GmailApi, GmailApiErrorKind, GmailMessage,
    GmailMessageRef, GmailProfile, ListPage, MockGmail,
};
pub use html::{SanitizedHtml, SanitizePolicy};
pub use logo::{LogoPipelineInput, SenderDomain};
pub use mailbox::{AttachmentMeta, MessageId, MessagePresentation, ThreadId, ThreadPresentation};
pub use multi_account::{
    assert_may_sync, unified_inbox_join, ReceivingAccountIndicator, UnifiedInboxRow,
};
pub use oauth::{
    build_auth_url, default_scopes, generate_pkce, AuthUrlRequest, OAuthClientConfig, PendingPkce,
    PendingPkceStore, PkcePair, SCOPE_CALENDAR_EVENTS, SCOPE_EMAIL, SCOPE_GMAIL_MODIFY,
    SCOPE_GMAIL_READONLY, SCOPE_OPENID, SCOPE_PROFILE, SCOPE_TASKS,
};
pub use pacing::{
    CursorState, PaceDecision, RequestPacer, ThrottlePolicy, TokenBucketPacer,
    DEFAULT_REQUESTS_PER_SECOND,
};
pub use persistence::{
    migrations, schema_forbids_tokens, AccountRecord, LocalStore, MemoryStore, Migration,
    StoredMessage,
};
pub use platform::{
    BrowserOpen, CredentialStore, MemoryCredentialStore, NotificationRequest, NotificationService,
    SecureSecret,
};
pub use priority::{
    assess_action_required, assess_priority, ActionRequiredAssessment, MailListBadgeFlags,
    PriorityAssessment, PriorityLevel, PrioritySignals,
};
pub use privacy::{
    data_inventory, launch_safe_link, sanitize_log_line, validate_external_url, DataInventoryEntry,
    LocalPurgePlan, PrivacyCenterModel, PrivacyCenterTab, UrlSafety,
};
pub use rules::{
    find_conflicts, first_matching_rule, resolve_category, resolve_with_optional_ai,
    CorrectionField, CorrectionStore, DecisionSource, EffectiveDecision, RuleAction, RuleCondition,
    UserCorrection, UserRule,
};
pub use search::{
    build_fts_match, escape_fts_term, SearchDebouncer, SearchQuery, SEARCH_DEBOUNCE,
};
pub use sync::{SyncPhase, SyncProgress, SyncProgressSnapshot};
pub use sync_engine::{
    classify_sync_error, sync_error_user_message, ImportFlowState, SyncCoordinator, SyncErrorClass,
};
pub use temporal::{
    extract_temporal, TemporalConfidence, TemporalExtractionInput, TemporalItem, TemporalItemType,
};
