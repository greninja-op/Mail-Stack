//! Phase 04 — Desktop navigation destinations (Design A shell IA).

use serde::{Deserialize, Serialize};

/// Mailbox destinations. Companies are never destinations.
///
/// Sidebar rail (Design A): Inbox / Starred / Spam (+ Categories surface).
/// List-pane category tabs: Primary / Promotional / Social.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum NavDestination {
    AllInbox,
    Primary,
    Promotional,
    Social,
    Spam,
    Starred,
    /// Empty until snooze is stored — shown so the rail matches the desktop IA.
    Snoozed,
    Sent,
    Drafts,
}

impl NavDestination {
    /// All filterable destinations (shortcuts / demos still use the full set).
    pub const ALL: [NavDestination; 9] = [
        Self::AllInbox,
        Self::Primary,
        Self::Promotional,
        Self::Social,
        Self::Spam,
        Self::Starred,
        Self::Snoozed,
        Self::Sent,
        Self::Drafts,
    ];

    /// Desktop rail. Compose is a CTA, not a destination.
    pub const SIDEBAR: [NavDestination; 6] = [
        Self::AllInbox,
        Self::Starred,
        Self::Snoozed,
        Self::Sent,
        Self::Drafts,
        Self::Spam,
    ];

    /// Category tabs hosted in the list panel when browsing Inbox.
    pub const LIST_TABS: [NavDestination; 3] = [Self::Primary, Self::Promotional, Self::Social];

    pub fn label(self) -> &'static str {
        match self {
            Self::AllInbox => "Inbox",
            Self::Primary => "Primary",
            Self::Promotional => "Promotional",
            Self::Social => "Social",
            Self::Spam => "Spam",
            Self::Starred => "Starred",
            Self::Snoozed => "Snoozed",
            Self::Sent => "Sent",
            Self::Drafts => "Drafts",
        }
    }

    pub fn shortcut_digit(self) -> Option<char> {
        match self {
            Self::AllInbox => Some('1'),
            Self::Primary => Some('2'),
            Self::Promotional => Some('3'),
            Self::Social => Some('4'),
            Self::Spam => Some('5'),
            Self::Starred => Some('6'),
            Self::Snoozed | Self::Sent | Self::Drafts => None,
        }
    }

    /// True when this destination is shown via Inbox + list category tabs.
    pub fn is_inbox_family(self) -> bool {
        matches!(
            self,
            Self::AllInbox | Self::Primary | Self::Promotional | Self::Social
        )
    }
}

/// Which primary workspace surface is showing (mail vs dedicated Categories).
/// Categories is not a mailbox destination — it lives below the mail rail.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize, Default)]
pub enum WorkspaceSurface {
    #[default]
    Mail,
    Categories,
}

/// Overlay / slide-in panes that are not mailbox destinations.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum OverlayPane {
    None,
    Settings,
    PrivacyCenter,
    Search,
    ImportFlow,
    AccountSwitcher,
    Compose,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct NavigationState {
    pub destination: NavDestination,
    pub surface: WorkspaceSurface,
    pub overlay: OverlayPane,
    /// 0.0 = closed, 1.0 = fully open (settings slide).
    pub settings_progress: f32,
}

impl Default for NavigationState {
    fn default() -> Self {
        Self {
            destination: NavDestination::AllInbox,
            surface: WorkspaceSurface::Mail,
            overlay: OverlayPane::None,
            settings_progress: 0.0,
        }
    }
}

impl NavigationState {
    pub fn select(&mut self, dest: NavDestination) {
        self.destination = dest;
        self.surface = WorkspaceSurface::Mail;
        // Navigation never reloads the page — content swaps in place.
    }

    /// Sidebar Inbox — keep category tab if already in inbox family, else Primary.
    pub fn open_inbox(&mut self) {
        if !self.destination.is_inbox_family() {
            self.destination = NavDestination::Primary;
        }
        self.surface = WorkspaceSurface::Mail;
    }

    pub fn open_categories(&mut self) {
        self.surface = WorkspaceSurface::Categories;
    }

    pub fn open_settings(&mut self) {
        self.overlay = OverlayPane::Settings;
    }

    pub fn open_compose(&mut self) {
        self.overlay = OverlayPane::Compose;
    }

    pub fn close_overlay(&mut self) {
        self.settings_progress = 0.0;
        self.overlay = OverlayPane::None;
    }

    /// Kept for call sites / tests — progress is snap open/closed (no width animation).
    pub fn tick_settings_slide(&mut self, _dt: f32, _duration_secs: f32, opening: bool) {
        self.settings_progress = if opening { 1.0 } else { 0.0 };
        if !opening {
            self.overlay = OverlayPane::None;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sidebar_has_three_mail_destinations() {
        assert_eq!(NavDestination::SIDEBAR.len(), 6);
        assert_eq!(NavDestination::AllInbox.label(), "Inbox");
        assert_eq!(NavDestination::LIST_TABS.len(), 3);
    }

    #[test]
    fn select_does_not_clear_overlay_by_default() {
        let mut n = NavigationState::default();
        n.open_settings();
        n.select(NavDestination::Primary);
        assert_eq!(n.destination, NavDestination::Primary);
        assert_eq!(n.overlay, OverlayPane::Settings);
        assert_eq!(n.surface, WorkspaceSurface::Mail);
    }

    #[test]
    fn categories_surface_is_separate_from_mailbox() {
        let mut n = NavigationState::default();
        n.open_categories();
        assert_eq!(n.surface, WorkspaceSurface::Categories);
        n.select(NavDestination::Starred);
        assert_eq!(n.surface, WorkspaceSurface::Mail);
    }

    #[test]
    fn compose_overlay_opens() {
        let mut n = NavigationState::default();
        n.open_compose();
        assert_eq!(n.overlay, OverlayPane::Compose);
    }
}
