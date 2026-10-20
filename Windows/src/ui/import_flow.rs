//! Import flow chrome — honest progress (caps 99% until Done).

use egui::{Color32, RichText, Rounding};

use crate::app_state::AppState;
use crate::bridge::CoreBridge;
use crate::navigation::OverlayPane;
use crate::tokens::{self, ThemeTokens, CARD_RADIUS};

/// Hardcoded dark palette for the import card (design.md — both themes).
const IMPORT_BG: Color32 = Color32::from_rgb(0x1A, 0x17, 0x28);
const IMPORT_TEXT: Color32 = Color32::from_rgb(0xF5, 0xF3, 0xFA);

pub fn show_import_flow_window(
    ctx: &egui::Context,
    state: &mut AppState,
    _theme: &ThemeTokens,
    bridge: &CoreBridge,
) {
    if state.nav.overlay != OverlayPane::ImportFlow {
        return;
    }

    let snap = bridge.sync_snapshot_demo(
        state.import_progress_pct as u64,
        100,
        state.import_done,
    );
    let stage = if snap.done {
        "Done"
    } else {
        "Importing mail…"
    };

    egui::Window::new("Importing mail")
        .collapsible(false)
        .resizable(false)
        .anchor(egui::Align2::CENTER_CENTER, egui::vec2(0.0, 0.0))
        .frame(
            egui::Frame::none()
                .fill(IMPORT_BG)
                .rounding(Rounding::same(CARD_RADIUS))
                .inner_margin(egui::Margin::same(24.0)),
        )
        .show(ctx, |ui| {
            ui.set_min_width(420.0);
            ui.horizontal(|ui| {
                ui.label(RichText::new("Gmail").color(IMPORT_TEXT).strong());
                ui.label(RichText::new("  →  ").color(IMPORT_TEXT));
                ui.label(RichText::new("Mailstack").color(tokens::BRAND_DARK).strong());
            });
            ui.add_space(tokens::space::LG);
            ui.label(RichText::new(stage).color(IMPORT_TEXT));
            let (rect, _) =
                ui.allocate_exact_size(egui::vec2(64.0, 64.0), egui::Sense::hover());
            let painter = ui.painter();
            painter.circle_stroke(
                rect.center(),
                28.0,
                egui::Stroke::new(3.0, Color32::from_rgb(0x40, 0x38, 0x58)),
            );
            painter.circle_stroke(
                rect.center(),
                28.0,
                egui::Stroke::new(3.0, tokens::BRAND_DARK),
            );
            ui.label(
                RichText::new(format!("{}%", snap.display_percent))
                    .color(IMPORT_TEXT)
                    .size(tokens::type_size::TITLE_MD),
            );
            ui.label(
                RichText::new("Conduit pulse is not a progress bar — only this ring is.")
                    .size(tokens::type_size::LABEL)
                    .color(Color32::from_rgb(0xA8, 0xA0, 0xB8)),
            );
            ui.add_space(tokens::space::MD);
            ui.horizontal(|ui| {
                if !state.import_done {
                    if ui.button("Simulate +10%").clicked() {
                        state.import_progress_pct = (state.import_progress_pct + 10).min(99);
                    }
                    if ui.button("Mark done").clicked() {
                        state.import_done = true;
                        state.import_progress_pct = 100;
                    }
                }
                if ui
                    .add_enabled(state.import_done, egui::Button::new("Continue"))
                    .clicked()
                {
                    state.nav.close_overlay();
                }
                if ui.button("Not now").clicked() {
                    state.nav.close_overlay();
                }
            });
        });
}
