//! Phase 03 + 13–21 — Application state: accounts, mailbox sample, overlays.

use std::time::Instant;

use desktop_core::priority::PriorityLevel;
use desktop_core::search::{SearchDebouncer, SearchQuery};

use crate::navigation::{NavDestination, NavigationState, OverlayPane};
use crate::tokens::{MotionTier, ThemeMode, ThemeTokens};
use crate::workspace::{WorkspaceLayout, WorkspaceLoadState};

/// Demo / hydrate mail row (Phase 14 presentation model).
/// Carries raw sender + receiving-account for logo pipeline and unified inbox.
#[derive(Clone, Debug)]
pub struct MailRowModel {
    pub id: String,
    pub thread_id: String,
    pub account_id: String,
    pub sender_name: String,
    /// Raw sender email — required for logo / company domain detection.
    pub sender_email: String,
    pub sender_domain: Option<String>,
    pub is_company: bool,
    pub subject: String,
    pub snippet: String,
    pub time_label: String,
    pub unread: bool,
    /// Message star — distinct from company pin.
    pub starred: bool,
    pub category: NavDestination,
    /// Receiving-account indicator (not sender).
    pub account_label: String,
    pub body_sanitized: String,
    pub priority: PriorityLevel,
    /// "Why this priority?" — from core, never invented in UI.
    pub priority_explanation: Option<String>,
}

#[derive(Clone, Debug)]
pub struct AccountSummary {
    pub id: String,
    pub email_display: String,
    pub display_name: String,
    pub is_demo: bool,
}

#[derive(Clone, Debug)]
pub struct AppState {
    pub nav: NavigationState,
    pub layout: WorkspaceLayout,
    pub load: WorkspaceLoadState,
    pub theme_mode: ThemeMode,
    pub motion: MotionTier,
    pub accounts: Vec<AccountSummary>,
    pub active_account_idx: usize,
    /// When true, mail list joins all enabled accounts (presentation-only).
    pub unified_inbox: bool,
    pub messages: Vec<MailRowModel>,
    /// Free-text field that feeds structured [`SearchQuery`] — never SQL.
    pub search_text: String,
    pub search_query: SearchQuery,
    pub search_debounce: SearchDebouncer,
    pub search_last_input: Option<Instant>,
    pub bars_visible: bool,
    pub last_scroll_y: f32,
    pub import_progress_pct: u8,
    pub import_done: bool,
    pub reduced_motion: bool,
    /// Privacy Center selected tab (phase 23).
    pub privacy_tab: Option<desktop_core::privacy::PrivacyCenterTab>,
    /// Local-only purge requested from Privacy Center (never touches Gmail).
    pub privacy_purge_requested: bool,
}

impl Default for AppState {
    fn default() -> Self {
        Self {
            nav: NavigationState::default(),
            layout: WorkspaceLayout::default(),
            load: WorkspaceLoadState::Ready,
            theme_mode: ThemeMode::Dark,
            motion: MotionTier::Standard,
            accounts: vec![
                AccountSummary {
                    id: "acct-demo".into(),
                    email_display: "you@example.com".into(),
                    display_name: "You".into(),
                    is_demo: true,
                },
                AccountSummary {
                    id: "acct-work".into(),
                    email_display: "you@work.example".into(),
                    display_name: "Work".into(),
                    is_demo: true,
                },
            ],
            active_account_idx: 0,
            unified_inbox: true,
            messages: sample_messages(),
            search_text: String::new(),
            search_query: SearchQuery::default(),
            search_debounce: SearchDebouncer::new(),
            search_last_input: None,
            bars_visible: true,
            last_scroll_y: 0.0,
            import_progress_pct: 0,
            import_done: false,
            reduced_motion: false,
            privacy_tab: None,
            privacy_purge_requested: false,
        }
    }
}

impl AppState {
    pub fn theme(&self) -> ThemeTokens {
        match self.theme_mode {
            ThemeMode::Light => ThemeTokens::light(),
            ThemeMode::Dark => ThemeTokens::dark(),
            ThemeMode::HighContrast => ThemeTokens::high_contrast(),
        }
    }

    pub fn motion_effective(&self) -> MotionTier {
        if self.reduced_motion {
            MotionTier::Reduced
        } else {
            self.motion
        }
    }

    pub fn active_account(&self) -> Option<&AccountSummary> {
        self.accounts.get(self.active_account_idx)
    }

    /// Rebuild structured query from the text field (UI never builds SQL).
    pub fn rebuild_search_query(&mut self) {
        let text = self.search_text.trim();
        self.search_query = SearchQuery {
            text: if text.is_empty() {
                None
            } else {
                Some(text.to_owned())
            },
            account_ids: if self.unified_inbox {
                Vec::new()
            } else {
                self.active_account()
                    .and_then(|a| desktop_core::AccountId::new(&a.id))
                    .into_iter()
                    .collect()
            },
            ..Default::default()
        };
    }

    pub fn on_search_input(&mut self) {
        let now = Instant::now();
        self.search_last_input = Some(now);
        self.search_debounce.on_input(now);
        self.rebuild_search_query();
    }

    /// Apply debounce gate; returns true when the structured query should run.
    pub fn search_should_fire(&mut self) -> bool {
        self.search_debounce.should_fire(Instant::now())
    }

    pub fn filtered_messages(&self) -> Vec<&MailRowModel> {
        let q = self
            .search_query
            .text
            .as_deref()
            .unwrap_or("")
            .trim()
            .to_ascii_lowercase();
        let active_id = self.active_account().map(|a| a.id.as_str());
        self.messages
            .iter()
            .filter(|m| {
                if self.unified_inbox {
                    true
                } else {
                    active_id.map(|id| m.account_id == id).unwrap_or(true)
                }
            })
            .filter(|m| match self.nav.destination {
                NavDestination::AllInbox => {
                    m.category != NavDestination::Spam && m.category != NavDestination::Starred
                }
                NavDestination::Starred => m.starred,
                dest => {
                    m.category == dest
                        || (dest == NavDestination::Primary
                            && m.category == NavDestination::AllInbox)
                }
            })
            .filter(|m| {
                if q.is_empty() {
                    return true;
                }
                m.sender_name.to_ascii_lowercase().contains(&q)
                    || m.sender_email.to_ascii_lowercase().contains(&q)
                    || m.subject.to_ascii_lowercase().contains(&q)
                    || m.snippet.to_ascii_lowercase().contains(&q)
            })
            .collect()
    }

    pub fn selected_message(&self) -> Option<&MailRowModel> {
        let id = self.layout.selected_message_id.as_ref()?;
        self.messages.iter().find(|m| &m.id == id)
    }

    pub fn select_message(&mut self, id: String) {
        self.layout.selected_message_id = Some(id);
    }

    pub fn toggle_star_selected(&mut self) {
        if let Some(id) = self.layout.selected_message_id.clone() {
            if let Some(m) = self.messages.iter_mut().find(|m| m.id == id) {
                m.starred = !m.starred;
            }
        }
    }

    pub fn on_list_scroll(&mut self, y: f32) {
        let dy = y - self.last_scroll_y;
        if dy > 2.0 {
            self.bars_visible = false;
        } else if dy < -2.0 {
            self.bars_visible = true;
        }
        self.last_scroll_y = y;
    }

    pub fn tick(&mut self, dt: f32) {
        let opening = self.nav.overlay == OverlayPane::Settings
            || self.nav.settings_progress > 0.0 && self.nav.overlay == OverlayPane::Settings;
        let want_open = self.nav.overlay == OverlayPane::Settings;
        let dur = self.motion_effective().settings_slide_secs();
        if want_open || self.nav.settings_progress > 0.0 {
            self.nav.tick_settings_slide(dt, dur, want_open);
        }
        let _ = opening;
        // Drain debounce without building SQL in the UI.
        let _ = self.search_should_fire();
    }
}

fn sample_messages() -> Vec<MailRowModel> {
    vec![
        MailRowModel {
            id: "m1".into(),
            thread_id: "t1".into(),
            account_id: "acct-demo".into(),
            sender_name: "Acme Billing".into(),
            sender_email: "billing@mail.acme.example".into(),
            sender_domain: Some("acme.example".into()),
            is_company: true,
            subject: "Your invoice for October".into(),
            snippet: "Thanks for your business. Invoice #1042 is attached for review.".into(),
            time_label: "9:14 AM".into(),
            unread: true,
            starred: false,
            category: NavDestination::Primary,
            account_label: "you@example.com".into(),
            body_sanitized: "Thanks for your business. Invoice #1042 is ready.\n\n(Images blocked for privacy — never fetched.)".into(),
            priority: PriorityLevel::Medium,
            priority_explanation: Some("Review / reminder cues".into()),
        },
        MailRowModel {
            id: "m2".into(),
            thread_id: "t2".into(),
            account_id: "acct-demo".into(),
            sender_name: "Jordan Lee".into(),
            sender_email: "jordan@gmail.com".into(),
            sender_domain: None,
            is_company: false,
            subject: "Lunch tomorrow?".into(),
            snippet: "Are you free around noon near the office?".into(),
            time_label: "Yesterday".into(),
            unread: false,
            starred: true,
            category: NavDestination::Primary,
            account_label: "you@example.com".into(),
            body_sanitized: "Are you free around noon near the office?\n\n— Jordan".into(),
            priority: PriorityLevel::None,
            priority_explanation: None,
        },
        MailRowModel {
            id: "m3".into(),
            thread_id: "t3".into(),
            account_id: "acct-work".into(),
            sender_name: "ShopPulse".into(),
            sender_email: "deals@promo.shoppulse.example".into(),
            sender_domain: Some("shoppulse.example".into()),
            is_company: true,
            subject: "Weekend deals inside".into(),
            snippet: "Save on essentials this weekend only.".into(),
            time_label: "Mon".into(),
            unread: true,
            starred: false,
            category: NavDestination::Promotional,
            account_label: "you@work.example".into(),
            body_sanitized: "Save on essentials this weekend only.".into(),
            priority: PriorityLevel::Low,
            priority_explanation: Some("Informational cues".into()),
        },
        MailRowModel {
            id: "m4".into(),
            thread_id: "t4".into(),
            account_id: "acct-demo".into(),
            sender_name: "Circle Social".into(),
            sender_email: "notify@circle.example".into(),
            sender_domain: Some("circle.example".into()),
            is_company: true,
            subject: "Alex commented on your post".into(),
            snippet: "Nice photos from the hike!".into(),
            time_label: "Sun".into(),
            unread: false,
            starred: false,
            category: NavDestination::Social,
            account_label: "you@example.com".into(),
            body_sanitized: "Alex commented: Nice photos from the hike!".into(),
            priority: PriorityLevel::None,
            priority_explanation: None,
        },
        MailRowModel {
            id: "m5".into(),
            thread_id: "t5".into(),
            account_id: "acct-work".into(),
            sender_name: "Hiring Desk".into(),
            sender_email: "recruiting@corp.example".into(),
            sender_domain: Some("corp.example".into()),
            is_company: true,
            subject: "Interview ASAP — please respond".into(),
            snippet: "Action required: confirm your interview slot by Friday deadline.".into(),
            time_label: "8:02 AM".into(),
            unread: true,
            starred: false,
            category: NavDestination::Primary,
            account_label: "you@work.example".into(),
            body_sanitized: "Please confirm your interview slot by Friday.\n\nAction required.".into(),
            priority: PriorityLevel::Urgent,
            priority_explanation: Some("Deadline / urgency language detected".into()),
        },
    ]
}

#[cfg(test)]
mod tests {
    use super::*;
    use desktop_core::priority::MailListBadgeFlags;

    #[test]
    fn starred_filter_includes_starred_only() {
        let mut s = AppState::default();
        s.nav.destination = NavDestination::Starred;
        let rows = s.filtered_messages();
        assert!(!rows.is_empty());
        assert!(rows.iter().all(|m| m.starred));
    }

    #[test]
    fn row_preserves_sender_email_for_logo() {
        let s = AppState::default();
        let m = s.messages.iter().find(|m| m.id == "m1").unwrap();
        assert!(!m.sender_email.is_empty());
        assert!(m.sender_domain.is_some());
    }

    #[test]
    fn unified_shows_receiving_not_sender() {
        let s = AppState::default();
        let m = s.messages.iter().find(|m| m.id == "m5").unwrap();
        assert_eq!(m.account_label, "you@work.example");
        assert_ne!(m.account_label, m.sender_email);
    }

    #[test]
    fn no_ar_chip_contract() {
        assert!(!MailListBadgeFlags::supports_action_required_chip());
    }

    #[test]
    fn search_rebuilds_structured_query() {
        let mut s = AppState::default();
        s.search_text = "invoice".into();
        s.rebuild_search_query();
        assert_eq!(s.search_query.text.as_deref(), Some("invoice"));
    }
}
