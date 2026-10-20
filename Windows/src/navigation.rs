//! Phase 04 — Desktop navigation destinations (design.md sidebar).

use serde::{Deserialize, Serialize};

/// Top-level mailbox destinations. Companies are never destinations.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum NavDestination {
    AllInbox,
    Primary,
    Promotional,
    Social,
    Spam,
    Starred,
}

impl NavDestination {
    pub const ALL: [NavDestination; 6] = [
        Self::AllInbox,
        Self::Primary,
        Self::Promotional,
        Self::Social,
        Self::Spam,
        Self::Starred,
    ];

    pub fn label(self) -> &'static str {
        match self {
            Self::AllInbox => "All Inbox",
            Self::Primary => "Primary",
            Self::Promotional => "Promotional",
            Self::Social => "Social",
            Self::Spam => "Spam",
            Self::Starred => "Starred",
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
            // unreachable for ALL but keep exhaustive
        }
    }
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
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct NavigationState {
    pub destination: NavDestination,
    pub overlay: OverlayPane,
    /// 0.0 = closed, 1.0 = fully open (settings slide).
    pub settings_progress: f32,
}

impl Default for NavigationState {
    fn default() -> Self {
        Self {
            destination: NavDestination::AllInbox,
            overlay: OverlayPane::None,
            settings_progress: 0.0,
        }
    }
}

impl NavigationState {
    pub fn select(&mut self, dest: NavDestination) {
        self.destination = dest;
        // Navigation never reloads the page — content swaps in place.
    }

    pub fn open_settings(&mut self) {
        self.overlay = OverlayPane::Settings;
    }

    pub fn close_overlay(&mut self) {
        // Keep settings_progress so the slide-out can animate; panel clears overlay at 0.
        if self.overlay != OverlayPane::Settings {
            self.settings_progress = 0.0;
        }
        self.overlay = OverlayPane::None;
    }

    pub fn tick_settings_slide(&mut self, dt: f32, duration_secs: f32, opening: bool) {
        if duration_secs <= 0.0 {
            self.settings_progress = if opening { 1.0 } else { 0.0 };
            return;
        }
        let step = dt / duration_secs;
        if opening {
            self.settings_progress = (self.settings_progress + step).min(1.0);
        } else {
            self.settings_progress = (self.settings_progress - step).max(0.0);
            if self.settings_progress <= 0.0 {
                self.overlay = OverlayPane::None;
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sidebar_has_six_destinations() {
        assert_eq!(NavDestination::ALL.len(), 6);
        assert_eq!(NavDestination::AllInbox.label(), "All Inbox");
    }

    #[test]
    fn select_does_not_clear_overlay_by_default() {
        let mut n = NavigationState::default();
        n.open_settings();
        n.select(NavDestination::Primary);
        assert_eq!(n.destination, NavDestination::Primary);
        assert_eq!(n.overlay, OverlayPane::Settings);
    }
}
