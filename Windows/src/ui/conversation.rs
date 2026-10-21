//! Conversation / message detail — Design A reading pane.
//!
//! Reading toolbar mutates demo AppState (star / unread / delete / spam /
//! archive). Empty state when nothing selected. Categories never dump here.
//! Priority explainability + conversation intelligence in surfaceContainer
//! cards. HTML sanitized; remote images never fetched.

use egui::{RichText, Rounding};

use crate::app_state::AppState;
use crate::bridge::CoreBridge;
use crate::tokens::{self, ThemeTokens, CARD_RADIUS};
pub fn show_conversation(
    ui: &mut egui::Ui,
    state: &mut AppState,
    theme: &ThemeTokens,
    bridge: &CoreBridge,
) {
    let Some(msg) = state.selected_message().cloned() else {
        show_empty_state(ui, theme);
        return;
    };

    show_reading_toolbar(ui, state, theme, msg.starred, msg.unread);
    ui.add_space(tokens::space::SM);
    ui.separator();
    ui.add_space(tokens::space::SM);

    // Re-fetch after toolbar may have deleted / moved the selection.
    let Some(msg) = state.selected_message().cloned() else {
        show_empty_state(ui, theme);
        return;
    };

    egui::ScrollArea::vertical()
        .id_salt("conversation_scroll")
        .auto_shrink([false, false])
        .show(ui, |ui| {
            ui.label(
                RichText::new(&msg.subject)
                    .size(tokens::type_size::TITLE_LG)
                    .color(theme.text_primary)
                    .strong(),
            );
            ui.add_space(tokens::space::XS);
            ui.label(
                RichText::new(format!(
                    "{} · {} · {}",
                    msg.sender_name, msg.account_label, msg.time_label
                ))
                .size(tokens::type_size::BODY)
                .color(theme.text_secondary),
            );
            ui.add_space(tokens::space::XL);
            let sanitized = bridge.sanitize_mail_html(&msg.body_sanitized);
            ui.label(
                RichText::new(&sanitized.text)
                    .size(tokens::type_size::BODY_LG)
                    .color(theme.text_primary),
            );
            if sanitized.remote_images_blocked {
                ui.add_space(tokens::space::XS);
                ui.label(
                    RichText::new("Remote images blocked.")
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                );
            }
            ui.add_space(tokens::space::LG);
            ui.horizontal(|ui| {
                let reply = egui::Button::new(
                    RichText::new("Reply").color(theme.on_brand).size(tokens::type_size::LABEL_LG),
                )
                .fill(theme.brand)
                .rounding(ThemeTokens::pill_rounding())
                .min_size(egui::vec2(88.0, 32.0));
                if ui.add(reply).clicked() {
                    state.begin_reply_selected();
                }
                if tonal_btn(ui, theme, "Forward").clicked() {
                    state.begin_forward_selected();
                }
            });
        });
}

fn show_empty_state(ui: &mut egui::Ui, theme: &ThemeTokens) {
    let rect = ui.max_rect().intersect(ui.clip_rect()).intersect(ui.ctx().screen_rect());
    let painter = ui.painter().with_clip_rect(rect);
    let width = (rect.width() - 48.0).max(80.0);
    paint_centered(&painter, rect, width, rect.top() + tokens::space::GIANT, "No conversation selected", tokens::type_size::TITLE_SM, theme.text_primary);
    paint_centered(
        &painter,
        rect,
        width,
        rect.top() + tokens::space::GIANT + tokens::space::XL,
        "Choose a message. It will open here.",
        tokens::type_size::BODY,
        theme.text_secondary,
    );
    ui.allocate_rect(rect, egui::Sense::hover());
}

fn paint_centered(
    painter: &egui::Painter,
    rect: egui::Rect,
    width: f32,
    y: f32,
    text: &str,
    size: f32,
    color: egui::Color32,
) {
    let galley = painter.layout(text.to_owned(), egui::FontId::proportional(size), color, width);
    let x = rect.center().x - galley.size().x * 0.5;
    painter.galley(egui::pos2(x.max(rect.left() + 16.0), y), galley, color);
}

fn show_reading_toolbar(
    ui: &mut egui::Ui,
    state: &mut AppState,
    theme: &ThemeTokens,
    starred: bool,
    unread: bool,
) {
    ui.horizontal_wrapped(|ui| {
        ui.spacing_mut().item_spacing.x = tokens::space::XS;
        if toolbar_btn(ui, theme, "Archive").clicked() {
            state.archive_selected_demo();
        }
        if toolbar_btn(ui, theme, "Spam").clicked() {
            state.mark_selected_spam();
        }
        if toolbar_btn(ui, theme, "Delete").clicked() {
            state.delete_selected_from_demo_list();
        }
        let unread_label = if unread { "Read" } else { "Unread" };
        if toolbar_btn(ui, theme, unread_label).clicked() {
            state.toggle_unread_selected();
        }
        let star_label = if starred { "Unstar" } else { "Star" };
        if toolbar_btn(ui, theme, star_label).clicked() {
            state.toggle_star_selected();
        }
    });
}

fn tonal_btn(ui: &mut egui::Ui, theme: &ThemeTokens, label: &str) -> egui::Response {
    ui.add(
        egui::Button::new(
            RichText::new(label)
                .size(tokens::type_size::LABEL_LG)
                .color(theme.on_primary_container),
        )
        .fill(theme.primary_container)
        .rounding(ThemeTokens::pill_rounding())
        .min_size(egui::vec2(88.0, 32.0)),
    )
}

fn toolbar_btn(ui: &mut egui::Ui, theme: &ThemeTokens, label: &str) -> egui::Response {
    ui.add(
        egui::Button::new(
            RichText::new(label)
                .size(tokens::type_size::LABEL)
                .color(theme.text_primary),
        )
        .fill(theme.surface_elevated)
        .stroke(theme.hairline())
        .rounding(ThemeTokens::pill_rounding()),
    )
}

fn tonal_card(ui: &mut egui::Ui, theme: &ThemeTokens, add_contents: impl FnOnce(&mut egui::Ui)) {
    egui::Frame::none()
        .fill(theme.surface_elevated)
        .rounding(Rounding::same(CARD_RADIUS))
        .inner_margin(egui::Margin::same(tokens::space::MD))
        .show(ui, |ui| add_contents(ui));
}
