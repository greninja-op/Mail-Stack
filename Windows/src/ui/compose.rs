//! Compose overlay — draft To / Subject / Body with local Send + Discard.
//!
//! Send does **not** talk to SMTP; it logs and clears the draft (or shows status).
//! Shell opens this via `OverlayPane::Compose`.

use egui::RichText;

use crate::app_state::AppState;
use crate::navigation::OverlayPane;
use crate::tokens::{self, ThemeTokens};

pub fn show_compose_window(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    if state.nav.overlay != OverlayPane::Compose {
        return;
    }

    let mut open = true;
    egui::Window::new("Compose")
        .open(&mut open)
        .collapsible(false)
        .resizable(true)
        .default_width(480.0)
        .default_height(420.0)
        .anchor(egui::Align2::CENTER_CENTER, egui::vec2(0.0, 0.0))
        .frame(
            egui::Frame::window(&ctx.style())
                .fill(theme.surface)
                .rounding(tokens::ThemeTokens::card_rounding())
                .inner_margin(egui::Margin::same(tokens::space::MD))
                .stroke(theme.hairline()),
        )
        .show(ctx, |ui| {
            ui.horizontal(|ui| {
                ui.heading(
                    RichText::new("New message")
                        .size(tokens::type_size::TITLE_MD)
                        .color(theme.text_primary),
                );
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    if ui.button("Close").clicked() {
                        state.nav.close_overlay();
                    }
                });
            });
            ui.add_space(tokens::space::XS);
            ui.separator();
            ui.add_space(tokens::space::SM);

            labeled_field(ui, theme, "To", |ui| {
                ui.add(
                    egui::TextEdit::singleline(&mut state.compose_to)
                        .desired_width(f32::INFINITY)
                        .hint_text("recipient@example.com"),
                );
            });
            ui.add_space(tokens::space::XS);
            labeled_field(ui, theme, "Subject", |ui| {
                ui.add(
                    egui::TextEdit::singleline(&mut state.compose_subject)
                        .desired_width(f32::INFINITY)
                        .hint_text("Subject"),
                );
            });
            ui.add_space(tokens::space::XS);
            ui.label(
                RichText::new("Body")
                    .size(tokens::type_size::LABEL_LG)
                    .color(theme.text_secondary),
            );
            ui.add(
                egui::TextEdit::multiline(&mut state.compose_body)
                    .desired_width(f32::INFINITY)
                    .desired_rows(10)
                    .hint_text("Write your message…"),
            );

            if let Some(status) = state.compose_status.as_ref() {
                ui.add_space(tokens::space::XS);
                ui.label(
                    RichText::new(status)
                        .size(tokens::type_size::BODY_SM)
                        .color(theme.status.success),
                );
            }

            ui.add_space(tokens::space::MD);
            ui.horizontal(|ui| {
                let send = egui::Button::new(
                    RichText::new("Send")
                        .size(tokens::type_size::LABEL_LG)
                        .color(theme.on_brand),
                )
                .fill(theme.brand)
                .rounding(ThemeTokens::pill_rounding())
                .min_size(egui::vec2(88.0, 36.0));
                if ui.add(send).clicked() {
                    state.submit_compose_local();
                }

                let discard = egui::Button::new(
                    RichText::new("Discard")
                        .size(tokens::type_size::LABEL_LG)
                        .color(theme.text_secondary),
                )
                .fill(theme.surface_container_low)
                .rounding(ThemeTokens::pill_rounding())
                .min_size(egui::vec2(88.0, 36.0));
                if ui.add(discard).clicked() {
                    state.clear_compose_draft();
                    state.nav.close_overlay();
                }
            });

            ui.add_space(tokens::space::XS);
            ui.label(
                RichText::new("Local draft only — Send does not deliver mail yet.")
                    .size(tokens::type_size::LABEL)
                    .color(theme.text_secondary),
            );
        });

    if !open {
        state.nav.close_overlay();
    }
}

fn labeled_field(
    ui: &mut egui::Ui,
    theme: &ThemeTokens,
    label: &str,
    add_contents: impl FnOnce(&mut egui::Ui),
) {
    ui.horizontal(|ui| {
        ui.set_min_width(64.0);
        ui.label(
            RichText::new(label)
                .size(tokens::type_size::LABEL_LG)
                .color(theme.text_secondary),
        );
        add_contents(ui);
    });
}
