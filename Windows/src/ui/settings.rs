//! Settings slide-in pane (design.md: 350ms, content shifts 1/12).

use egui::RichText;

use crate::app_state::AppState;
use crate::navigation::OverlayPane;
use crate::tokens::{self, ThemeMode, ThemeTokens, SETTINGS_WIDTH};

pub fn show_settings_panel(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    let opening = state.nav.overlay == OverlayPane::Settings;
    if !opening && state.nav.settings_progress <= 0.01 {
        return;
    }

    // Drive slide toward open/closed.
    let dur = state.motion_effective().settings_slide_secs();
    let dt = ctx.input(|i| i.stable_dt);
    state.nav.tick_settings_slide(dt, dur, opening);
    if state.nav.settings_progress > 0.0 {
        ctx.request_repaint();
    }

    let width = SETTINGS_WIDTH * state.nav.settings_progress.max(0.05);
    egui::SidePanel::right("settings_panel")
        .exact_width(width)
        .resizable(false)
        .frame(
            egui::Frame::none()
                .fill(theme.surface_elevated)
                .inner_margin(egui::Margin::same(16.0)),
        )
        .show(ctx, |ui| {
            if state.nav.settings_progress < 0.85 {
                return;
            }
            ui.horizontal(|ui| {
                ui.heading(
                    RichText::new("Settings")
                        .size(tokens::type_size::TITLE_LG)
                        .color(theme.text_primary),
                );
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    if ui.button("Close").clicked() {
                        state.nav.close_overlay();
                    }
                });
            });
            ui.separator();
            ui.label(RichText::new("Appearance").strong().color(theme.text_primary));
            ui.horizontal(|ui| {
                if ui
                    .selectable_label(state.theme_mode == ThemeMode::Dark, "Dark")
                    .clicked()
                {
                    state.theme_mode = ThemeMode::Dark;
                }
                if ui
                    .selectable_label(state.theme_mode == ThemeMode::Light, "Light")
                    .clicked()
                {
                    state.theme_mode = ThemeMode::Light;
                }
                if ui
                    .selectable_label(state.theme_mode == ThemeMode::HighContrast, "High contrast")
                    .clicked()
                {
                    state.theme_mode = ThemeMode::HighContrast;
                }
            });
            ui.checkbox(&mut state.reduced_motion, "Reduce motion");
            ui.add_space(tokens::space::LG);
            ui.label(RichText::new("Mailbox").strong().color(theme.text_primary));
            if ui.button("Show import experience…").clicked() {
                state.nav.overlay = OverlayPane::ImportFlow;
            }
            if ui.button("Privacy center…").clicked() {
                state.nav.overlay = OverlayPane::PrivacyCenter;
            }
            let show_inspector = &mut state.layout.show_inspector;
            ui.checkbox(show_inspector, "Show company inspector");

            ui.with_layout(egui::Layout::bottom_up(egui::Align::Center), |ui| {
                ui.label(
                    RichText::new(
                        "Mailstack for Windows — interim egui shell.\nWinUI 3 / Windows App SDK is the packaging path when installed.",
                    )
                    .size(tokens::type_size::LABEL)
                    .color(theme.text_secondary),
                );
                ui.label(
                    RichText::new("Privacy Policy · Terms of Service")
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                );
            });
        });
}
