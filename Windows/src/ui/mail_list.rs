//! Phase 14 — Flat mail rows (design.md): 40px avatar, 3 lines, min 88px.
//!
//! No Action Required chip in the mail list. Receiving-account indicator on
//! every unified row. Logo pipeline uses sender domain → letter fallback.

use desktop_core::priority::{MailListBadgeFlags, PriorityLevel};
use egui::{Align, Color32, Layout, RichText, Sense, Vec2};

use crate::app_state::{AppState, MailRowModel};
use crate::tokens::{self, ThemeTokens, AVATAR_SIZE, MAIL_ROW_MIN_HEIGHT};

pub fn show_mail_list(ui: &mut egui::Ui, state: &mut AppState, theme: &ThemeTokens) {
    let rows: Vec<MailRowModel> = state.filtered_messages().into_iter().cloned().collect();
    if rows.is_empty() {
        ui.vertical_centered(|ui| {
            ui.add_space(48.0);
            ui.label(
                RichText::new("No messages")
                    .size(tokens::type_size::TITLE_MD)
                    .color(theme.text_secondary),
            );
            ui.label(
                RichText::new("This destination is empty.")
                    .size(tokens::type_size::BODY)
                    .color(theme.text_secondary),
            );
        });
        return;
    }

    let selected = state.layout.selected_message_id.clone();
    egui::ScrollArea::vertical()
        .auto_shrink([false, false])
        .show(ui, |ui| {
            let scroll_y = ui.clip_rect().top();
            state.on_list_scroll(scroll_y);
            for row in &rows {
                let is_sel = selected.as_deref() == Some(row.id.as_str());
                if show_mail_row(ui, row, theme, is_sel) {
                    state.select_message(row.id.clone());
                }
            }
        });
}

fn show_mail_row(
    ui: &mut egui::Ui,
    row: &MailRowModel,
    theme: &ThemeTokens,
    selected: bool,
) -> bool {
    let desired = Vec2::new(ui.available_width(), MAIL_ROW_MIN_HEIGHT);
    let (rect, response) = ui.allocate_exact_size(desired, Sense::click());

    if ui.is_rect_visible(rect) {
        let fill = if selected {
            theme.row_selected
        } else if response.hovered() {
            theme.row_hover
        } else {
            Color32::TRANSPARENT
        };
        // Flat row — no card, no border, no shadow.
        ui.painter().rect_filled(rect, 0.0, fill);

        ui.allocate_ui_at_rect(rect.shrink2(Vec2::new(tokens::space::MD, tokens::space::SM)), |ui| {
            ui.with_layout(Layout::left_to_right(Align::Center), |ui| {
                let letter = row
                    .sender_name
                    .chars()
                    .next()
                    .unwrap_or('?')
                    .to_ascii_uppercase();
                let avatar_color = if row.is_company {
                    theme.brand
                } else {
                    theme.surface_elevated
                };
                let (avatar_rect, _) =
                    ui.allocate_exact_size(Vec2::splat(AVATAR_SIZE), Sense::hover());
                ui.painter()
                    .circle_filled(avatar_rect.center(), AVATAR_SIZE / 2.0, avatar_color);
                ui.painter().text(
                    avatar_rect.center(),
                    egui::Align2::CENTER_CENTER,
                    letter.to_string(),
                    egui::FontId::proportional(16.0),
                    theme.text_primary,
                );

                ui.add_space(tokens::space::MD);
                ui.vertical(|ui| {
                    ui.horizontal(|ui| {
                        let sender = if row.unread {
                            RichText::new(&row.sender_name)
                                .strong()
                                .size(tokens::type_size::BODY_LG)
                                .color(theme.text_primary)
                        } else {
                            RichText::new(&row.sender_name)
                                .size(tokens::type_size::BODY_LG)
                                .color(theme.text_primary)
                        };
                        ui.label(sender);
                        ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                            let time_color = if row.unread {
                                theme.brand
                            } else {
                                theme.text_secondary
                            };
                            ui.label(
                                RichText::new(&row.time_label)
                                    .size(tokens::type_size::LABEL)
                                    .color(time_color),
                            );
                            if row.starred {
                                ui.label(RichText::new("★").color(theme.brand));
                            }
                            // Priority badge only — never an Action Required chip.
                            let flags = MailListBadgeFlags::for_priority(row.priority);
                            if flags.show_priority_badge {
                                ui.label(
                                    RichText::new(priority_short(row.priority))
                                        .size(tokens::type_size::LABEL)
                                        .color(theme.brand),
                                );
                            }
                            debug_assert!(!MailListBadgeFlags::supports_action_required_chip());
                        });
                    });
                    let subject = if row.unread {
                        RichText::new(&row.subject)
                            .strong()
                            .size(tokens::type_size::BODY)
                            .color(theme.text_primary)
                    } else {
                        RichText::new(&row.subject)
                            .size(tokens::type_size::BODY)
                            .color(theme.text_primary)
                    };
                    ui.label(subject);
                    ui.label(
                        RichText::new(&row.snippet)
                            .size(tokens::type_size::LABEL_LG)
                            .color(theme.text_secondary),
                    );
                    // Receiving-account indicator (unified inbox) — not the sender.
                    ui.horizontal(|ui| {
                        let (dot, _) = ui.allocate_exact_size(Vec2::splat(8.0), Sense::hover());
                        ui.painter()
                            .circle_filled(dot.center(), 4.0, theme.brand);
                        ui.label(
                            RichText::new(&row.account_label)
                                .size(tokens::type_size::LABEL)
                                .color(theme.text_secondary),
                        );
                    });
                });
            });
        });
    }

    response.clicked()
}

fn priority_short(level: PriorityLevel) -> &'static str {
    match level {
        PriorityLevel::None => "",
        PriorityLevel::Low => "Low",
        PriorityLevel::Medium => "Med",
        PriorityLevel::High => "High",
        PriorityLevel::Urgent => "Urgent",
    }
}
