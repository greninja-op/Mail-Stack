//! Phase 03–04 — Multi-column workspace geometry & restoration (Design A).

use serde::{Deserialize, Serialize};

use crate::navigation::NavDestination;

/// Restorable window + column layout (Phase 03 state restoration).
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct WorkspaceLayout {
    pub window_width: f32,
    pub window_height: f32,
    pub sidebar_width: f32,
    pub list_width: f32,
    pub show_inspector: bool,
    pub inspector_width: f32,
    /// Selected message id (opaque).
    pub selected_message_id: Option<String>,
    pub selected_destination: NavDestination,
}

impl Default for WorkspaceLayout {
    fn default() -> Self {
        Self {
            window_width: 1280.0,
            window_height: 800.0,
            sidebar_width: crate::tokens::SIDEBAR_WIDTH,
            list_width: 420.0,
            show_inspector: false,
            inspector_width: 280.0,
            selected_message_id: None,
            selected_destination: NavDestination::AllInbox,
        }
    }
}

impl WorkspaceLayout {
    pub const SIDEBAR_MIN: f32 = 180.0;
    pub const SIDEBAR_MAX: f32 = 320.0;
    pub const LIST_MIN: f32 = 280.0;
    pub const DETAIL_MIN: f32 = 320.0;
    pub const SPLITTER_W: f32 = 6.0;
    /// List and reader panels — larger than badges, smaller than a full pill.
    pub const PANEL_RADIUS: f32 = 16.0;
    /// Gap between list | reader surface panels (`space-4`).
    pub const PANEL_GAP: f32 = 16.0;
    /// Outer padding around the dual-panel host on the canvas.
    pub const CANVAS_PAD: f32 = 12.0;
    /// Below this window width, collapse central area to list XOR reader.
    pub const COLLAPSE_BELOW: f32 = 1100.0;

    /// Clamp sidebar after a drag (SidePanel reports the live width).
    pub fn set_sidebar_width(&mut self, w: f32) {
        self.sidebar_width = w.clamp(Self::SIDEBAR_MIN, Self::SIDEBAR_MAX);
    }

    /// True when the window is narrow enough to show only one central panel.
    pub fn is_two_pane_collapsed(&self) -> bool {
        self.window_width < Self::COLLAPSE_BELOW
    }

    /// List + detail widths for the central panel (sidebar is already excluded).
    pub fn allocate_mail_columns(&self, available: f32) -> ColumnWidths {
        let gap = Self::PANEL_GAP;
        let usable = (available - gap).max(0.0);
        let max_list = (usable - Self::DETAIL_MIN).max(Self::LIST_MIN);
        let list = self.list_width.clamp(Self::LIST_MIN, max_list);
        let detail = (usable - list).max(Self::DETAIL_MIN);
        ColumnWidths {
            sidebar: self.sidebar_width,
            list,
            detail,
            inspector: 0.0,
        }
    }

    /// Apply a horizontal drag delta to the list/detail splitter.
    pub fn resize_list_by(&mut self, delta_x: f32, available: f32) {
        let gap = Self::PANEL_GAP;
        let usable = (available - gap).max(0.0);
        let max_list = (usable - Self::DETAIL_MIN).max(Self::LIST_MIN);
        self.list_width = (self.list_width + delta_x).clamp(Self::LIST_MIN, max_list);
    }

    /// Legacy helper — prefers mail-column allocation (no double-count of sidebar).
    pub fn allocate_columns(&self, available: f32) -> ColumnWidths {
        self.allocate_mail_columns(available)
    }

    /// Shift content left by 1/12 while settings is open (design.md).
    pub fn settings_content_shift(&self, progress: f32) -> f32 {
        -(self.window_width / 12.0) * progress.clamp(0.0, 1.0)
    }
}

#[derive(Clone, Copy, Debug)]
pub struct ColumnWidths {
    pub sidebar: f32,
    pub list: f32,
    pub detail: f32,
    pub inspector: f32,
}

/// Screen-level load state (honest; no blank import).
#[derive(Clone, Debug, PartialEq, Eq, Default)]
pub enum WorkspaceLoadState {
    Launching,
    Loading,
    #[default]
    Ready,
    Empty,
    Offline,
    AuthRequired,
    Error { message: String },
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn allocate_keeps_detail_alive() {
        let w = WorkspaceLayout::default();
        let cols = w.allocate_mail_columns(900.0);
        assert!(cols.detail >= WorkspaceLayout::DETAIL_MIN);
        assert!(cols.list >= WorkspaceLayout::LIST_MIN);
    }

    #[test]
    fn resize_list_clamps() {
        let mut w = WorkspaceLayout::default();
        w.list_width = 380.0;
        w.resize_list_by(10_000.0, 900.0);
        assert!(w.list_width <= 900.0 - WorkspaceLayout::DETAIL_MIN);
        w.resize_list_by(-10_000.0, 900.0);
        assert!((w.list_width - WorkspaceLayout::LIST_MIN).abs() < 0.01);
    }

    #[test]
    fn settings_shift_scales_with_progress() {
        let w = WorkspaceLayout::default();
        assert!((w.settings_content_shift(0.0) - 0.0).abs() < f32::EPSILON);
        assert!(w.settings_content_shift(1.0) < 0.0);
    }

    #[test]
    fn collapse_threshold() {
        let mut w = WorkspaceLayout::default();
        w.window_width = 1099.0;
        assert!(w.is_two_pane_collapsed());
        w.window_width = 1100.0;
        assert!(!w.is_two_pane_collapsed());
    }
}
