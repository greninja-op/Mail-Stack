//! Search overlay — structured query UI (300ms debounce). UI never builds SQL.
//! Flat mail-style result chrome on background (no cards for rows).

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
        .frame(
            egui::Frame::window(&ui.ctx().style())
                .fill(theme.surface_elevated)
                .rounding(tokens::ThemeTokens::card_rounding())
                .inner_margin(egui::Margin::same(tokens::space::MD)),
        )
        .show(ui.ctx(), |ui| {
            ui.label(
                RichText::new("Search mail")
                    .size(tokens::type_size::TITLE_MD)
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

            // Flat preview hits — same 3-line hierarchy as mail list (no AR chip).
            let q = state.search_text.to_lowercase();
            if !q.is_empty() {
                ui.add_space(tokens::space::XS);
                ui.separator();
                for row in state.messages.iter().filter(|m| {
                    m.subject.to_lowercase().contains(&q)
                        || m.sender_name.to_lowercase().contains(&q)
                        || m.snippet.to_lowercase().contains(&q)
                }) {
                    ui.horizontal(|ui| {
                        ui.label(
                            RichText::new(&row.sender_name)
                                .size(tokens::type_size::TITLE_MD)
                                .color(theme.text_primary),
                        );
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.label(
                                RichText::new(&row.time_label)
                                    .size(tokens::type_size::LABEL)
                                    .color(theme.text_secondary),
                            );
                        });
                    });
                    ui.label(
                        RichText::new(&row.subject)
                            .size(tokens::type_size::BODY)
                            .color(theme.text_primary),
                    );
                    ui.label(
                        RichText::new(&row.snippet)
                            .size(tokens::type_size::BODY)
                            .color(theme.text_secondary),
                    );
                    ui.add_space(tokens::space::XS);
                }
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
