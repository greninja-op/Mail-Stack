//! Phase 26 — Keyboard shortcuts, menus, context menus, selection,
//! multi-window hooks, and focus management.

use egui::{Key, KeyboardShortcut, Modifiers};

use crate::app_state::AppState;
use crate::capability_profile::AccessibilityHooks;
use crate::navigation::{NavDestination, OverlayPane};

/// Application-level shortcut map (Fluent / desktop conventions).
pub struct ShortcutMap;

impl ShortcutMap {
    pub fn refresh() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::CTRL, Key::R)
    }

    pub fn search() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::CTRL, Key::F)
    }

    pub fn settings() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::CTRL, Key::Comma)
    }

    pub fn star() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::NONE, Key::S)
    }

    pub fn escape() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::NONE, Key::Escape)
    }

    pub fn next_mail() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::NONE, Key::J)
    }

    pub fn prev_mail() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::NONE, Key::K)
    }

    pub fn select_all() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::CTRL, Key::A)
    }

    pub fn new_window() -> KeyboardShortcut {
        KeyboardShortcut::new(Modifiers::CTRL, Key::N)
    }
}

/// Focus target within the mail workspace (logical order; no traps).
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum FocusTarget {
    Sidebar,
    MailList,
    Conversation,
    Inspector,
    Overlay,
}

impl FocusTarget {
    pub fn cycle(self, forward: bool) -> Self {
        let order = [
            Self::Sidebar,
            Self::MailList,
            Self::Conversation,
            Self::Inspector,
            Self::Overlay,
        ];
        let idx = order.iter().position(|t| *t == self).unwrap_or(0);
        if forward {
            order[(idx + 1) % order.len()]
        } else {
            order[(idx + order.len() - 1) % order.len()]
        }
    }

    pub fn narrator_label(self) -> &'static str {
        match self {
            Self::Sidebar => "Mailbox destinations",
            Self::MailList => "Message list",
            Self::Conversation => "Conversation",
            Self::Inspector => "Inspector",
            Self::Overlay => "Overlay pane",
        }
    }
}

/// Selection model: single or multi (Shift/Ctrl).
#[derive(Clone, Debug, Default)]
pub struct SelectionState {
    pub primary_id: Option<String>,
    pub multi_ids: Vec<String>,
}

impl SelectionState {
    pub fn clear(&mut self) {
        self.primary_id = None;
        self.multi_ids.clear();
    }

    pub fn select_single(&mut self, id: String) {
        self.primary_id = Some(id.clone());
        self.multi_ids = vec![id];
    }

    pub fn toggle_multi(&mut self, id: String) {
        if let Some(pos) = self.multi_ids.iter().position(|x| x == &id) {
            self.multi_ids.remove(pos);
            if self.primary_id.as_deref() == Some(id.as_str()) {
                self.primary_id = self.multi_ids.first().cloned();
            }
        } else {
            self.multi_ids.push(id.clone());
            self.primary_id = Some(id);
        }
    }

    pub fn count(&self) -> usize {
        self.multi_ids.len()
    }
}

/// Deferred multi-window request (egui viewport open is host-specific).
#[derive(Clone, Debug, Default)]
pub struct MultiWindowState {
    pub pending_new_window: bool,
    pub restore_bounds: Option<(f32, f32, f32, f32)>,
}

impl MultiWindowState {
    pub fn request_new_window(&mut self) {
        self.pending_new_window = true;
    }

    pub fn take_new_window_request(&mut self) -> bool {
        let v = self.pending_new_window;
        self.pending_new_window = false;
        v
    }
}

/// Per-frame interaction context carried by the shell.
#[derive(Clone, Debug)]
pub struct InteractionState {
    pub focus: FocusTarget,
    pub selection: SelectionState,
    pub windows: MultiWindowState,
    pub context_menu_message_id: Option<String>,
}

impl Default for InteractionState {
    fn default() -> Self {
        Self {
            focus: FocusTarget::MailList,
            selection: SelectionState::default(),
            windows: MultiWindowState::default(),
            context_menu_message_id: None,
        }
    }
}

/// Apply consumed shortcuts for this frame. Returns true if handled.
pub fn handle_shortcuts(ctx: &egui::Context, state: &mut AppState) -> bool {
    let mut ix = InteractionState::default();
    // Sync selection from layout so j/k still move the highlighted row.
    if let Some(id) = state.layout.selected_message_id.clone() {
        ix.selection.select_single(id);
    }
    handle_shortcuts_with(ctx, state, &mut ix)
}

/// Full shortcut handler with interaction state (focus / multi-select / windows).
pub fn handle_shortcuts_with(
    ctx: &egui::Context,
    state: &mut AppState,
    ix: &mut InteractionState,
) -> bool {
    let mut handled = false;
    ctx.input_mut(|i| {
        if i.consume_shortcut(&ShortcutMap::search()) {
            state.nav.overlay = OverlayPane::Search;
            ix.focus = FocusTarget::Overlay;
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::settings()) {
            state.nav.open_settings();
            ix.focus = FocusTarget::Overlay;
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::escape()) {
            state.nav.close_overlay();
            ix.focus = FocusTarget::MailList;
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::star()) {
            state.toggle_star_selected();
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::refresh()) {
            // Explicit refresh only — never velocity-triggered.
            state.load = crate::workspace::WorkspaceLoadState::Ready;
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::next_mail()) {
            move_selection(state, ix, 1);
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::prev_mail()) {
            move_selection(state, ix, -1);
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::select_all()) {
            let ids: Vec<String> = state
                .filtered_messages()
                .iter()
                .map(|m| m.id.clone())
                .collect();
            ix.selection.multi_ids = ids.clone();
            ix.selection.primary_id = ids.first().cloned();
            handled = true;
        }
        if i.consume_shortcut(&ShortcutMap::new_window()) {
            ix.windows.request_new_window();
            handled = true;
        }
        // Tab cycles focus (Shift+Tab reverse) when not in a text field.
        if i.modifiers.shift && i.key_pressed(Key::Tab) {
            ix.focus = ix.focus.cycle(false);
            handled = true;
        } else if i.key_pressed(Key::Tab) && !i.modifiers.any() {
            ix.focus = ix.focus.cycle(true);
            handled = true;
        }
        for dest in NavDestination::ALL {
            if let Some(d) = dest.shortcut_digit() {
                let key = match d {
                    '1' => Key::Num1,
                    '2' => Key::Num2,
                    '3' => Key::Num3,
                    '4' => Key::Num4,
                    '5' => Key::Num5,
                    '6' => Key::Num6,
                    _ => continue,
                };
                let sc = KeyboardShortcut::new(Modifiers::CTRL, key);
                if i.consume_shortcut(&sc) {
                    state.nav.select(dest);
                    state.layout.selected_destination = dest;
                    ix.focus = FocusTarget::MailList;
                    handled = true;
                }
            }
        }
    });
    handled
}

fn move_selection(state: &mut AppState, ix: &mut InteractionState, delta: i32) {
    let ids: Vec<String> = state
        .filtered_messages()
        .iter()
        .map(|m| m.id.clone())
        .collect();
    if ids.is_empty() {
        return;
    }
    let cur = state
        .layout
        .selected_message_id
        .as_ref()
        .and_then(|id| ids.iter().position(|x| x == id))
        .unwrap_or(0);
    let next = (cur as i32 + delta).clamp(0, ids.len() as i32 - 1) as usize;
    state.select_message(ids[next].clone());
    ix.selection.select_single(ids[next].clone());
    ix.focus = FocusTarget::MailList;
}

/// Context menu actions for a mail row.
pub fn show_mail_context_menu(
    ui: &mut egui::Ui,
    state: &mut AppState,
    ix: &mut InteractionState,
    message_id: &str,
) {
    ui.menu_button("⋯", |ui| {
        if ui.button("Open").clicked() {
            state.select_message(message_id.to_owned());
            ix.selection.select_single(message_id.to_owned());
            ix.focus = FocusTarget::Conversation;
            ui.close_menu();
        }
        if ui.button("Star / Unstar").clicked() {
            state.select_message(message_id.to_owned());
            state.toggle_star_selected();
            ui.close_menu();
        }
        if ui.button("Copy subject").clicked() {
            if let Some(m) = state.messages.iter().find(|m| m.id == message_id) {
                ui.ctx().copy_text(m.subject.clone());
            }
            ui.close_menu();
        }
    });
}

/// Secondary-click context menu content.
pub fn context_menu_for_message(ui: &mut egui::Ui, state: &mut AppState, message_id: &str) {
    if ui.button("Open in conversation").clicked() {
        state.select_message(message_id.to_owned());
        ui.close_menu();
    }
    if ui.button("Toggle star").clicked() {
        state.select_message(message_id.to_owned());
        state.toggle_star_selected();
        ui.close_menu();
    }
}

/// Top menu bar actions (Phase 26) — simple entry used by the shell.
pub fn show_menu_bar(ui: &mut egui::Ui, state: &mut AppState) {
    let mut ix = InteractionState::default();
    show_menu_bar_with(ui, state, &mut ix);
}

/// Menu bar with multi-window / selection hooks.
pub fn show_menu_bar_with(ui: &mut egui::Ui, state: &mut AppState, ix: &mut InteractionState) {
    egui::menu::bar(ui, |ui| {
        ui.menu_button("File", |ui| {
            if ui.button("New window").clicked() {
                ix.windows.request_new_window();
                ui.close_menu();
            }
            if ui.button("Settings…").clicked() {
                state.nav.open_settings();
                ui.close_menu();
            }
            if ui.button("Quit").clicked() {
                ui.ctx().send_viewport_cmd(egui::ViewportCommand::Close);
            }
        });
        ui.menu_button("Edit", |ui| {
            if ui.button("Find…").clicked() {
                state.nav.overlay = OverlayPane::Search;
                ui.close_menu();
            }
            if ui.button("Select all").clicked() {
                let ids: Vec<String> = state
                    .filtered_messages()
                    .iter()
                    .map(|m| m.id.clone())
                    .collect();
                ix.selection.multi_ids = ids.clone();
                ix.selection.primary_id = ids.first().cloned();
                ui.close_menu();
            }
        });
        ui.menu_button("View", |ui| {
            for dest in NavDestination::ALL {
                if ui
                    .selectable_label(state.nav.destination == dest, dest.label())
                    .clicked()
                {
                    state.nav.select(dest);
                    ui.close_menu();
                }
            }
            ui.separator();
            let _ = ui.checkbox(&mut state.layout.show_inspector, "Inspector");
        });
        ui.menu_button("Account", |ui| {
            if ui.button("Switch account…").clicked() {
                state.nav.overlay = OverlayPane::AccountSwitcher;
                ui.close_menu();
            }
            if ui.button("Privacy center…").clicked() {
                state.nav.overlay = OverlayPane::PrivacyCenter;
                ui.close_menu();
            }
        });
        ui.menu_button("Help", |ui| {
            ui.label("Mailstack Windows — egui interim shell");
            ui.label("WinUI 3 / WinAppSDK is the packaging path.");
            ui.label(AccessibilityHooks::label("Help", "menu"));
        });
    });
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn move_selection_clamps() {
        let mut s = AppState::default();
        let mut ix = InteractionState::default();
        move_selection(&mut s, &mut ix, 100);
        assert!(s.layout.selected_message_id.is_some());
    }

    #[test]
    fn focus_cycles() {
        let f = FocusTarget::Sidebar.cycle(true);
        assert_eq!(f, FocusTarget::MailList);
        assert_eq!(FocusTarget::Overlay.cycle(true), FocusTarget::Sidebar);
    }

    #[test]
    fn multi_select_toggle() {
        let mut sel = SelectionState::default();
        sel.toggle_multi("a".into());
        sel.toggle_multi("b".into());
        assert_eq!(sel.count(), 2);
        sel.toggle_multi("a".into());
        assert_eq!(sel.count(), 1);
    }

    #[test]
    fn new_window_request() {
        let mut w = MultiWindowState::default();
        w.request_new_window();
        assert!(w.take_new_window_request());
        assert!(!w.take_new_window_request());
    }
}
