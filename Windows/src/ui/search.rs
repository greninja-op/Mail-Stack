//! Search overlay — structured query UI (300ms debounce). UI never builds SQL.

use desktop_core::search::{build_fts_match, SearchDebouncer};
use egui::RichText;

use crate::app_state::AppState;
use crate::navigation::OverlayPane;
use crate::tokens::{self, ThemeTokens};

pub fn show_search_overlay(ui: &mut egui::Ui, state: &mut AppState, theme: &ThemeTokens) {
    if state.nav.overlay != OverlayPane::Search {
        return;
    }

    egui::Window::new("Search")
        .collapsible(false)
        .resizable(true)
        .default_width(420.0)
        .show(ui.ctx(), |ui| {
            ui.label(
                RichText::new("Search mail")
                    .size(tokens::type_size::TITLE_SM)
                    .color(theme.text_primary),
            );
            let resp = ui.text_edit_singleline(&mut state.search_text);
            if resp.changed() {
                state.on_search_input();
            }
            resp.request_focus();

            ui.label(
                RichText::new(format!(
                    "{}ms debounce · structured query · UI never builds SQL",
                    SearchDebouncer::debounce_ms()
                ))
                .size(tokens::type_size::LABEL)
                .color(theme.text_secondary),
            );

            if let Some(fts) = build_fts_match(&state.search_query) {
                ui.label(
                    RichText::new(format!("FTS (escaped): {fts}"))
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                );
            }

            ui.horizontal(|ui| {
                if ui.button("Close").clicked() {
                    state.nav.close_overlay();
                }
                if ui.button("Clear").clicked() {
                    state.search_text.clear();
                    state.rebuild_search_query();
                }
            });
        });
}
