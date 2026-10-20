//! Action Engine: propose → user confirms → nothing external automatic.
//!
//! Calendar/Tasks traits use the same account tokens from one sign-in.

use crate::account::{AccountId, AccountScoped};
use crate::error::{CoreError, CoreResult};
use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::sync::Mutex;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ActionType {
    Meeting,
    Deadline,
    Task,
    CalendarEvent,
    GmailMarkRead,
    GmailArchive,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum ActionStatus {
    Proposed,
    Confirmed,
    Executed,
    Failed,
    NotConnected,
    Cancelled,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ActionProposal {
    pub account_id: AccountId,
    pub message_id: String,
    pub action_type: ActionType,
    pub target_key: String,
    pub title: String,
    pub preview: String,
    pub status: ActionStatus,
}

impl AccountScoped for ActionProposal {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

impl ActionProposal {
    /// Deterministic dedup id: messageId|actionType|targetKey
    pub fn dedup_id(&self) -> String {
        format!(
            "{}|{:?}|{}",
            self.message_id, self.action_type, self.target_key
        )
    }

    pub fn thread_dedup_key(action_type: ActionType, target_key: &str) -> String {
        format!("{:?}|{}", action_type, target_key)
    }
}

/// Calendar adapter — same account grant; never fake success when disconnected.
pub trait CalendarService: Send + Sync {
    fn is_connected(&self, account: &AccountId) -> bool;
    fn create_event(
        &self,
        account: &AccountId,
        title: &str,
        start_ms: i64,
        end_ms: Option<i64>,
        ical_uid: &str,
    ) -> CoreResult<String>;
}

/// Tasks adapter — marker-in-notes + execution store for idempotency.
pub trait TasksService: Send + Sync {
    fn is_connected(&self, account: &AccountId) -> bool;
    fn create_task(
        &self,
        account: &AccountId,
        title: &str,
        notes_marker: &str,
        due_ms: Option<i64>,
    ) -> CoreResult<String>;
}

#[derive(Default)]
pub struct ActionEngine {
    seen: Mutex<HashSet<String>>,
}

impl ActionEngine {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn propose(&self, proposal: ActionProposal) -> CoreResult<ActionProposal> {
        let mut g = self
            .seen
            .lock()
            .map_err(|_| CoreError::Internal("action lock".into()))?;
        let id = proposal.dedup_id();
        if g.contains(&id) {
            return Err(CoreError::InvalidArgument(format!(
                "duplicate action {id}"
            )));
        }
        g.insert(id);
        Ok(ActionProposal {
            status: ActionStatus::Proposed,
            ..proposal
        })
    }

    /// User confirms — external effects only after this gate.
    pub fn confirm(
        &self,
        mut proposal: ActionProposal,
        calendar: Option<&dyn CalendarService>,
        tasks: Option<&dyn TasksService>,
    ) -> CoreResult<ActionProposal> {
        proposal.status = ActionStatus::Confirmed;
        match proposal.action_type {
            ActionType::CalendarEvent | ActionType::Meeting => {
                let Some(cal) = calendar else {
                    proposal.status = ActionStatus::NotConnected;
                    return Ok(proposal);
                };
                if !cal.is_connected(&proposal.account_id) {
                    proposal.status = ActionStatus::NotConnected;
                    return Ok(proposal);
                }
                cal.create_event(
                    &proposal.account_id,
                    &proposal.title,
                    0,
                    None,
                    &proposal.dedup_id(),
                )?;
                proposal.status = ActionStatus::Executed;
            }
            ActionType::Task | ActionType::Deadline => {
                let Some(t) = tasks else {
                    proposal.status = ActionStatus::NotConnected;
                    return Ok(proposal);
                };
                if !t.is_connected(&proposal.account_id) {
                    proposal.status = ActionStatus::NotConnected;
                    return Ok(proposal);
                }
                t.create_task(
                    &proposal.account_id,
                    &proposal.title,
                    &proposal.dedup_id(),
                    None,
                )?;
                proposal.status = ActionStatus::Executed;
            }
            ActionType::GmailMarkRead | ActionType::GmailArchive => {
                // Gmail writes stay behind confirmation; adapters wire later.
                proposal.status = ActionStatus::Confirmed;
            }
        }
        Ok(proposal)
    }
}

/// Stub calendar that reports disconnected — confirm must not fake success.
#[derive(Default)]
pub struct DisconnectedCalendar;

impl CalendarService for DisconnectedCalendar {
    fn is_connected(&self, _account: &AccountId) -> bool {
        false
    }

    fn create_event(
        &self,
        _account: &AccountId,
        _title: &str,
        _start_ms: i64,
        _end_ms: Option<i64>,
        _ical_uid: &str,
    ) -> CoreResult<String> {
        Err(CoreError::PlatformUnavailable("calendar not connected".into()))
    }
}

/// Stub tasks that reports disconnected — confirm must not fake success.
#[derive(Default)]
pub struct DisconnectedTasks;

impl TasksService for DisconnectedTasks {
    fn is_connected(&self, _account: &AccountId) -> bool {
        false
    }

    fn create_task(
        &self,
        _account: &AccountId,
        _title: &str,
        _notes_marker: &str,
        _due_ms: Option<i64>,
    ) -> CoreResult<String> {
        Err(CoreError::PlatformUnavailable("tasks not connected".into()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn confirm_reports_not_connected() {
        let eng = ActionEngine::new();
        let a = AccountId::new("a").unwrap();
        let p = eng
            .propose(ActionProposal {
                account_id: a,
                message_id: "m".into(),
                action_type: ActionType::Meeting,
                target_key: "k".into(),
                title: "Meet".into(),
                preview: "preview".into(),
                status: ActionStatus::Proposed,
            })
            .unwrap();
        let cal = DisconnectedCalendar;
        let out = eng.confirm(p, Some(&cal), None).unwrap();
        assert_eq!(out.status, ActionStatus::NotConnected);
    }

    #[test]
    fn dedup_rejects_duplicate() {
        let eng = ActionEngine::new();
        let a = AccountId::new("a").unwrap();
        let p = ActionProposal {
            account_id: a,
            message_id: "m".into(),
            action_type: ActionType::Task,
            target_key: "k".into(),
            title: "T".into(),
            preview: String::new(),
            status: ActionStatus::Proposed,
        };
        eng.propose(p.clone()).unwrap();
        assert!(eng.propose(p).is_err());
    }
}
