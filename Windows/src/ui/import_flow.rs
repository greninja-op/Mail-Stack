//! Import flow chrome — honest progress (caps 99% until Done).
//!
//! Hardcoded Android ImportScreens dark palette in both themes (audit §8).

use egui::{Color32, FontId, RichText, Rounding, Sense, Vec2};

use crate::app_state::AppState;
use crate::bridge::CoreBridge;
use crate::navigation::OverlayPane;
use crate::tokens::{self, import_palette, ThemeTokens, CARD_RADIUS};

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
                .fill(import_palette::CARD)
                .rounding(Rounding::same(CARD_RADIUS))
                .inner_margin(egui::Margin::same(tokens::space::XL)),
        )
        .show(ctx, |ui| {
            ui.set_min_width(420.0);

            // Gmail mark left · Mailstack mark right (letter marks until assets).
            ui.horizontal(|ui| {
                paint_mark(ui, "G", import_palette::AVATAR_BLUE, import_palette::ON_SURFACE);
                ui.add_space(tokens::space::SM);
                ui.label(
                    RichText::new("Gmail")
                        .color(import_palette::ON_SURFACE)
                        .strong()
                        .size(tokens::type_size::BODY),
                );
                ui.add_space(tokens::space::MD);
                ui.label(
                    RichText::new("—")
                        .color(import_palette::ON_SURFACE_MUTED)
                        .size(tokens::type_size::BODY),
                );
                ui.add_space(tokens::space::MD);
                paint_mark(ui, "M", tokens::BRAND_DARK, import_palette::ON_SURFACE);
                ui.add_space(tokens::space::SM);
                ui.label(
                    RichText::new("Mailstack")
                        .color(tokens::BRAND_DARK)
                        .strong()
                        .size(tokens::type_size::BODY),
                );
            });

            ui.add_space(tokens::space::LG);
            ui.label(
                RichText::new(stage)
                    .color(import_palette::ON_SURFACE)
                    .size(tokens::type_size::TITLE_MD),
            );

            // Only the Mailstack ring is the progress bar (conduit ≠ progress).
            let (rect, _) = ui.allocate_exact_size(Vec2::splat(64.0), Sense::hover());
            let painter = ui.painter();
            painter.circle_stroke(
                rect.center(),
                28.0,
                egui::Stroke::new(3.0_f32, import_palette::RING_TRACK),
            );
            if snap.display_percent > 0 {
                painter.circle_stroke(
                    rect.center(),
                    28.0,
                    egui::Stroke::new(3.0_f32, import_palette::CONNECTED_BLUE),
                );
            }

            ui.label(
                RichText::new(format!("{}%", snap.display_percent))
                    .color(import_palette::ON_SURFACE)
                    .size(tokens::type_size::TITLE_MD),
            );
            ui.label(
                RichText::new("Conduit pulse is not a progress bar — only this ring is.")
                    .size(tokens::type_size::LABEL)
                    .color(import_palette::ON_SURFACE_MUTED),
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

fn paint_mark(ui: &mut egui::Ui, letter: &str, fill: Color32, fg: Color32) {
    let (r, _) = ui.allocate_exact_size(Vec2::splat(40.0), Sense::hover());
    ui.painter().circle_filled(r.center(), 20.0, fill);
    ui.painter().text(
        r.center(),
        egui::Align2::CENTER_CENTER,
        letter,
        FontId::proportional(18.0),
        fg,
    );
}
