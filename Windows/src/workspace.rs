//! Phase 03–04 — Multi-column workspace geometry & restoration.

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
            list_width: 380.0,
            show_inspector: false,
            inspector_width: 280.0,
            selected_message_id: None,
            selected_destination: NavDestination::AllInbox,
        }
    }
}

impl WorkspaceLayout {
    /// Adaptive column widths for the available content width.
    pub fn allocate_columns(&self, available: f32) -> ColumnWidths {
        let sidebar = self.sidebar_width.min(available * 0.35).max(160.0);
        let remaining = (available - sidebar).max(0.0);
        let inspector = if self.show_inspector && remaining > 700.0 {
            self.inspector_width.min(remaining * 0.3)
        } else {
            0.0
        };
        let list = self
            .list_width
            .min((remaining - inspector) * 0.55)
            .max(240.0)
            .min(remaining - inspector - 200.0)
            .max(200.0);
        let detail = (remaining - inspector - list).max(200.0);
        ColumnWidths {
            sidebar,
            list,
            detail,
            inspector,
        }
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
        let cols = w.allocate_columns(1200.0);
        assert!(cols.detail >= 200.0);
        assert!(cols.sidebar >= 160.0);
    }

    #[test]
    fn settings_shift_scales_with_progress() {
        let w = WorkspaceLayout::default();
        assert!((w.settings_content_shift(0.0) - 0.0).abs() < f32::EPSILON);
        assert!(w.settings_content_shift(1.0) < 0.0);
    }
}
