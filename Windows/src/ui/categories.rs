//! Category group cards — Android `CategoriesScreen` / MoCard parity.
//!
//! Single-cavity `surfaceContainer` cards at 28px radius (not accent-tinted
//! slabs). Header vertically centred: 40px accent circle @ 14% alpha + icon
//! tint, title, counts/subtitle, chevron. Expanded members are full-width
//! rows inside the same card.
//!
//! Mount only on `WorkspaceSurface::Categories` — never dump into the
//! Design A conversation / reading pane.

use egui::{Color32, RichText, Rounding, Sense, Vec2};

use crate::app_state::AppState;
use crate::bridge::CoreBridge;
use crate::tokens::{self, accent_tint, ThemeTokens, CATEGORY_ICON_SIZE};
use desktop_core::category::{CategoryGroupKind, CategoryGroupModel};

/// Shell entry — shown only on the dedicated Categories workspace surface
/// (never dumped into the conversation detail pane).
pub fn show_category_groups(
    ui: &mut egui::Ui,
    state: &mut AppState,
    theme: &ThemeTokens,
    bridge: &CoreBridge,
) {
    let account_id = state
        .active_account()
        .map(|a| a.id.clone())
        .unwrap_or_else(|| "acct-demo".into());
    show_category_panel(ui, theme, bridge, &account_id, state);
}

pub fn show_category_panel(
    ui: &mut egui::Ui,
    theme: &ThemeTokens,
    bridge: &CoreBridge,
    account_id: &str,
    state: &mut AppState,
) {
    ui.label(
        RichText::new("Categories")
            .size(tokens::type_size::TITLE_MD)
            .color(theme.text_primary),
    );
    ui.label(
        RichText::new("Pin is not star. Company filters stay inside a category.")
            .size(tokens::type_size::BODY_SM)
            .color(theme.text_secondary),
    );
    ui.add_space(tokens::space::XS);

    let groups = bridge.demo_category_groups(account_id);
    for group in groups {
        show_group_card(ui, theme, state, &group);
        ui.add_space(tokens::space::XS);
    }
}

fn show_group_card(
    ui: &mut egui::Ui,
    theme: &ThemeTokens,
    state: &mut AppState,
    group: &CategoryGroupModel,
) {
    let accent = theme.group_accent(group.kind);
    let id = ui.make_persistent_id(("cat_group", group.id.as_str()));
    let mut expanded = ui
        .ctx()
        .data_mut(|d| d.get_temp::<bool>(id).unwrap_or(group.expanded));

    let member_count = group.member_ids.len();
    let subtitle = group_subtitle(group.kind);

    // MoCard: solid surfaceContainer, 28px, no border / shadow / accent slab.
    let frame_resp = egui::Frame::none()
        .fill(theme.surface_elevated)
        .rounding(ThemeTokens::card_rounding())
        .inner_margin(egui::Margin::symmetric(tokens::space::MD, tokens::space::XS))
        .show(ui, |ui| {
            // Header — vertically centred icon / text / chevron.
            let header = ui.horizontal(|ui| {
                ui.with_layout(egui::Layout::left_to_right(egui::Align::Center), |ui| {
                    paint_accent_circle(ui, accent);
                    ui.add_space(tokens::space::MD);
                    ui.vertical(|ui| {
                        ui.label(
                            RichText::new(&group.title)
                                .size(tokens::type_size::TITLE_MD)
                                .color(theme.text_primary),
                        );
                        ui.label(
                            RichText::new(format!("{member_count} messages"))
                                .size(tokens::type_size::BODY_SM)
                                .color(theme.text_secondary),
                        );
                        ui.label(
                            RichText::new(subtitle)
                                .size(tokens::type_size::BODY_SM)
                                .color(theme.text_secondary),
                        );
                    });
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        let chevron = if expanded { "^" } else { "v" };
                        ui.label(
                            RichText::new(chevron)
                                .size(tokens::type_size::TITLE_MD)
                                .color(theme.text_secondary),
                        );
                    });
                });
            });

            if expanded {
                ui.add_space(tokens::space::XXS);
                if member_count == 0 {
                    ui.label(
                        RichText::new("No messages in this group yet.")
                            .size(tokens::type_size::BODY_SM)
                            .color(theme.text_secondary),
                    );
                } else {
                    for mid in &group.member_ids {
                        // Full-width member row inside same cavity (no nested card).
                        ui.horizontal(|ui| {
                            let label = member_label(state, mid);
                            ui.label(
                                RichText::new(&label)
                                    .size(tokens::type_size::BODY)
                                    .color(theme.text_primary),
                            );
                            ui.with_layout(
                                egui::Layout::right_to_left(egui::Align::Center),
                                |ui| {
                                    ui.label(
                                        RichText::new("1")
                                            .size(tokens::type_size::BODY)
                                            .color(theme.text_secondary),
                                    );
                                },
                            );
                        });
                    }
                }
            }

            header.response
        });

    // Clip hover to rounded card (MoCard clip-before-gesture).
    if frame_resp.response.hovered() {
        ui.painter().rect_filled(
            frame_resp.response.rect,
            Rounding::same(tokens::CARD_RADIUS),
            Color32::from_rgba_unmultiplied(
                theme.row_hover.r(),
                theme.row_hover.g(),
                theme.row_hover.b(),
                40,
            ),
        );
    }

    if frame_resp.inner.clicked() || frame_resp.response.clicked() {
        expanded = !expanded;
        ui.ctx().data_mut(|d| d.insert_temp(id, expanded));
    }
}

fn paint_accent_circle(ui: &mut egui::Ui, accent: Color32) {
    let (circle, _) = ui.allocate_exact_size(Vec2::splat(CATEGORY_ICON_SIZE), Sense::hover());
    ui.painter()
        .circle_filled(circle.center(), CATEGORY_ICON_SIZE / 2.0, accent_tint(accent));
    // Full-accent mark (Material icon tint stand-in) — geometric, not emoji.
    ui.painter().circle_filled(circle.center(), 5.0, accent);
}

fn group_subtitle(kind: CategoryGroupKind) -> &'static str {
    match kind {
        CategoryGroupKind::NeedsAttention => "Action required · Important · Security",
        CategoryGroupKind::Promotions => "Promotions · Newsletters",
        CategoryGroupKind::Updates => "Notifications · Receipts",
        CategoryGroupKind::WorkAndLearning => "Career · Education",
        CategoryGroupKind::EverythingElse => "Low value · Unclassified",
    }
}

fn member_label(state: &AppState, mid: &str) -> String {
    state
        .messages
        .iter()
        .find(|m| m.id == mid || m.thread_id == mid)
        .map(|m| {
            if m.subject.is_empty() {
                m.sender_name.clone()
            } else {
                m.subject.clone()
            }
        })
        .unwrap_or_else(|| mid.to_owned())
}
