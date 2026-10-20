//! Phase 06 + 13–21 — Rust core integration boundary for the Windows shell.
//!
//! UI consumes desktop-core models/use-cases through this bridge.
//! Native Windows APIs stay in `adapters/`. UI never builds SQL.

use desktop_core::account::AccountId;
use desktop_core::actions::{ActionEngine, ActionProposal, DisconnectedCalendar, DisconnectedTasks};
use desktop_core::cancel::CancelToken;
use desktop_core::category::{CategoryGroupId, CategoryGroupKind, CategoryGroupModel};
use desktop_core::conversation::{
    assess_conversation, ConversationAssessment, ConversationSignals,
};
use desktop_core::html::{SanitizePolicy, SanitizedHtml};
use desktop_core::mailbox::MessagePresentation;
use desktop_core::multi_account::{assert_may_sync, ReceivingAccountIndicator};
use desktop_core::persistence::{AccountRecord, LocalStore, MemoryStore};
use desktop_core::priority::{
    assess_action_required, assess_priority, ActionRequiredAssessment, MailListBadgeFlags,
    PriorityAssessment, PriorityLevel, PrioritySignals,
};
use desktop_core::search::{build_fts_match, SearchDebouncer, SearchQuery};
use desktop_core::sync::{SyncPhase, SyncProgress, SyncProgressSnapshot};
use desktop_core::temporal::{extract_temporal, TemporalExtractionInput, TemporalItem};

use crate::redacting_log;

/// Narrow façade the UI calls — no business logic duplication in views.
pub struct CoreBridge {
    pub cancel: CancelToken,
    pub search_debounce: SearchDebouncer,
    pub actions: ActionEngine,
    /// Demo local store — seeded accounts are `is_demo` and never sync.
    pub store: MemoryStore,
}

impl Default for CoreBridge {
    fn default() -> Self {
        Self::new()
    }
}

impl CoreBridge {
    pub fn new() -> Self {
        redacting_log::init(log::LevelFilter::Info);
        let store = MemoryStore::new();
        // Seed a demo account that must never sync.
        let _ = LocalStore::upsert_account(
            &store,
            &AccountRecord {
                account_id: AccountId::new("acct-demo").unwrap(),
                email: "you@example.com".into(),
                display_name: "You".into(),
                is_demo: true,
                enabled: true,
            },
        );
        Self {
            cancel: CancelToken::new(),
            search_debounce: SearchDebouncer::new(),
            actions: ActionEngine::new(),
            store,
        }
    }

    pub fn sanitize_mail_html(&self, raw: &str) -> SanitizedHtml {
        SanitizedHtml::from_untrusted(raw, SanitizePolicy::default())
    }

    /// Structured search → escaped FTS MATCH. UI never builds SQL.
    pub fn fts_match_for(&self, query: &SearchQuery) -> Option<String> {
        build_fts_match(query)
    }

    pub fn mail_list_badges(&self, level: PriorityLevel) -> MailListBadgeFlags {
        // Compile-time: no Action Required chip in the mail list.
        debug_assert!(!MailListBadgeFlags::supports_action_required_chip());
        MailListBadgeFlags::for_priority(level)
    }

    pub fn assess_priority_for(
        &self,
        account_raw: &str,
        message_id: &str,
        subject: &str,
        snippet: &str,
    ) -> Option<PriorityAssessment> {
        let account_id = AccountId::new(account_raw)?;
        Some(assess_priority(
            account_id,
            message_id,
            &PrioritySignals {
                subject: subject.into(),
                snippet: snippet.into(),
                ..Default::default()
            },
        ))
    }

    pub fn assess_action_required_for(
        &self,
        account_raw: &str,
        message_id: &str,
        subject: &str,
        snippet: &str,
    ) -> Option<ActionRequiredAssessment> {
        let account_id = AccountId::new(account_raw)?;
        Some(assess_action_required(
            account_id,
            message_id,
            &PrioritySignals {
                subject: subject.into(),
                snippet: snippet.into(),
                ..Default::default()
            },
        ))
    }

    pub fn assess_conversation_for(
        &self,
        account_raw: &str,
        thread_id: &str,
        signals: &ConversationSignals,
    ) -> Option<ConversationAssessment> {
        let account_id = AccountId::new(account_raw)?;
        Some(assess_conversation(account_id, thread_id, signals))
    }

    pub fn extract_temporal_for(
        &self,
        account_raw: &str,
        message_id: &str,
        subject: &str,
        snippet: &str,
        received_at_ms: i64,
    ) -> Vec<TemporalItem> {
        let Some(account_id) = AccountId::new(account_raw) else {
            return Vec::new();
        };
        extract_temporal(
            account_id,
            message_id,
            &TemporalExtractionInput {
                subject: subject.into(),
                snippet: snippet.into(),
                body_text: String::new(),
                received_at_ms,
            },
        )
    }

    /// Confirming when Calendar/Tasks are disconnected reports NotConnected — never fake success.
    pub fn confirm_action_demo(&self, proposal: ActionProposal) -> desktop_core::CoreResult<ActionProposal> {
        self.actions.confirm(
            proposal,
            Some(&DisconnectedCalendar),
            Some(&DisconnectedTasks),
        )
    }

    pub fn receiving_indicator(&self, account_raw: &str) -> Option<ReceivingAccountIndicator> {
        let id = AccountId::new(account_raw)?;
        let rec = LocalStore::get_account(&self.store, &id).ok().flatten()?;
        Some(ReceivingAccountIndicator::from_account(&rec))
    }

    /// Demo accounts must never sync.
    pub fn may_sync_demo_account(&self, account_raw: &str) -> bool {
        let Some(id) = AccountId::new(account_raw) else {
            return false;
        };
        match LocalStore::get_account(&self.store, &id) {
            Ok(Some(rec)) => assert_may_sync(&rec).is_ok(),
            _ => false,
        }
    }

    pub fn presentation_logo_domain(msg: &MessagePresentation) -> Option<String> {
        msg.logo_input()
            .detect_sender_domain()
            .map(|d| d.as_str().to_owned())
    }

    pub fn demo_category_groups(&self, account_raw: &str) -> Vec<CategoryGroupModel> {
        let Some(account_id) = AccountId::new(account_raw) else {
            return Vec::new();
        };
        vec![
            CategoryGroupModel {
                account_id: account_id.clone(),
                id: CategoryGroupId::new("needs").unwrap(),
                kind: CategoryGroupKind::NeedsAttention,
                title: "Needs attention".into(),
                member_ids: vec!["m1".into()],
                expanded: true,
            },
            CategoryGroupModel {
                account_id: account_id.clone(),
                id: CategoryGroupId::new("promo").unwrap(),
                kind: CategoryGroupKind::Promotions,
                title: "Promotions".into(),
                member_ids: vec!["m3".into()],
                expanded: false,
            },
            CategoryGroupModel {
                account_id,
                id: CategoryGroupId::new("updates").unwrap(),
                kind: CategoryGroupKind::Updates,
                title: "Updates".into(),
                member_ids: vec![],
                expanded: false,
            },
        ]
    }

    pub fn sync_snapshot_demo(&self, processed: u64, total: u64, done: bool) -> SyncProgressSnapshot {
        let mut progress = SyncProgress::start(total);
        progress.set_phase(SyncPhase::Fetching);
        progress.set_processed(processed);
        if done {
            progress.mark_done();
        }
        progress.snapshot()
    }

    pub fn is_cancelled(&self) -> bool {
        self.cancel.is_cancelled()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sanitize_strips_scriptish_content() {
        let b = CoreBridge::new();
        let html = b.sanitize_mail_html("<p>Hi</p><script>alert(1)</script>");
        assert!(html.text.contains("Hi") || !html.text.is_empty());
        assert!(!html.text.to_ascii_lowercase().contains("<script"));
    }

    #[test]
    fn sync_demo_caps_running_at_99() {
        let b = CoreBridge::new();
        let snap = b.sync_snapshot_demo(100, 100, false);
        assert_eq!(snap.display_percent, 99);
        let done = b.sync_snapshot_demo(100, 100, true);
        assert_eq!(done.display_percent, 100);
    }

    #[test]
    fn demo_account_never_syncs() {
        let b = CoreBridge::new();
        assert!(!b.may_sync_demo_account("acct-demo"));
    }

    #[test]
    fn fts_is_structured_not_raw_sql() {
        let b = CoreBridge::new();
        let q = SearchQuery {
            text: Some(r#"hi" OR 1=1"#.into()),
            ..Default::default()
        };
        let m = b.fts_match_for(&q).unwrap();
        assert!(m.starts_with('"') || m.contains("\"hi\"\""));
        assert!(!m.to_ascii_lowercase().contains("select "));
    }

    #[test]
    fn no_ar_chip_in_mail_list_flags() {
        let b = CoreBridge::new();
        let flags = b.mail_list_badges(PriorityLevel::High);
        assert!(flags.show_priority_badge);
        assert!(!MailListBadgeFlags::supports_action_required_chip());
    }
}
