//! Settings slide-in pane.
//!
//! Fixed-width panel (`SETTINGS_WIDTH`) — never animate SidePanel width every
//! frame (that reflows the workspace and feels like UI shake). Theme toggles
//! update `AppState.theme_mode`; shell applies egui visuals only on mode change.

use egui::RichText;

use crate::app_state::AppState;
use crate::navigation::OverlayPane;
use crate::tokens::{self, ThemeMode, ThemeTokens, SETTINGS_WIDTH};

pub fn show_settings_panel(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    let opening = state.nav.overlay == OverlayPane::Settings;
    if !opening && state.nav.settings_progress <= 0.01 {
        return;
    }

    // Fixed-width panel — open/close is instantaneous; progress is bookkeeping only.
    // Reduced motion also forces instant (no slide tween of width).
    if opening {
        state.nav.settings_progress = 1.0;
    } else {
        state.nav.settings_progress = 0.0;
        return;
    }

    egui::SidePanel::right("settings_panel")
        .exact_width(SETTINGS_WIDTH)
        .resizable(false)
        .frame(
            egui::Frame::none()
                .fill(theme.surface_elevated)
                .rounding(tokens::ThemeTokens::card_rounding())
                .inner_margin(egui::Margin::same(tokens::space::MD)),
        )
        .show(ctx, |ui| {
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

            ui.label(
                RichText::new("Appearance")
                    .strong()
                    .color(theme.text_primary),
            );
            ui.label(
                RichText::new("Theme applies to every screen immediately.")
                    .size(tokens::type_size::BODY_SM)
                    .color(theme.text_secondary),
            );
            ui.add_space(tokens::space::XS);
            ui.horizontal(|ui| {
                theme_mode_chip(ui, theme, state, ThemeMode::Light, "Light");
                theme_mode_chip(ui, theme, state, ThemeMode::Dark, "Dark");
                theme_mode_chip(ui, theme, state, ThemeMode::HighContrast, "High contrast");
            });

            ui.add_space(tokens::space::SM);
            let mut reduced = state.reduced_motion;
            if ui
                .checkbox(&mut reduced, "Reduce motion")
                .on_hover_text("Disables panel/message motion; hover/selected fills stay instant.")
                .changed()
            {
                state.reduced_motion = reduced;
                if reduced {
                    state.motion = tokens::MotionTier::Reduced;
                } else {
                    state.motion = tokens::MotionTier::Standard;
                }
            }

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

fn theme_mode_chip(
    ui: &mut egui::Ui,
    theme: &ThemeTokens,
    state: &mut AppState,
    mode: ThemeMode,
    label: &str,
) {
    let selected = state.theme_mode == mode;
    // Previous-frame hover from persistent id (instant fill; no width animation).
    let id = ui.make_persistent_id(("theme_chip", label));
    let hovered = ui.ctx().data(|d| d.get_temp::<bool>(id).unwrap_or(false));
    let fill = if selected {
        theme.primary_container
    } else if hovered {
        theme.row_hover
    } else {
        theme.surface_container_low
    };
    let text_color = if selected {
        theme.on_primary_container
    } else {
        theme.text_secondary
    };
    let resp = egui::Frame::none()
        .fill(fill)
        .rounding(ThemeTokens::pill_rounding())
        .inner_margin(egui::Margin::symmetric(tokens::space::MD, tokens::space::XS))
        .show(ui, |ui| {
            ui.label(
                RichText::new(label)
                    .size(tokens::type_size::LABEL_LG)
                    .color(text_color),
            );
        })
        .response
        .interact(egui::Sense::click());
    ui.ctx()
        .data_mut(|d| d.insert_temp(id, resp.hovered()));
    if resp.clicked() {
        state.theme_mode = mode;
    }
}
