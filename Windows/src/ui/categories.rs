//! Category group cards (single-cavity, 28px radius) — design.md.

use egui::{Color32, RichText, Rounding, Sense, Vec2};

use crate::bridge::CoreBridge;
use crate::tokens::{self, ThemeTokens, ACCENT_NEEDS_ATTENTION, CARD_RADIUS};
use desktop_core::category::CategoryGroupKind;

pub fn show_category_panel(
    ui: &mut egui::Ui,
    theme: &ThemeTokens,
    bridge: &CoreBridge,
    account_id: &str,
) {
    ui.label(
        RichText::new("Companies in this category")
            .size(tokens::type_size::LABEL_LG)
            .color(theme.text_secondary),
    );
    ui.label(
        RichText::new("Pin ≠ star · single-cavity cards · no company sidebar destinations")
            .size(tokens::type_size::LABEL)
            .color(theme.text_secondary),
    );
    ui.add_space(tokens::space::SM);

    for group in bridge.demo_category_groups(account_id) {
        let accent = accent_for(group.kind, theme);
        let fill = Color32::from_rgba_unmultiplied(accent.r(), accent.g(), accent.b(), 36);
        let (rect, _resp) =
            ui.allocate_exact_size(Vec2::new(ui.available_width(), 72.0), Sense::hover());
        ui.painter()
            .rect_filled(rect, Rounding::same(CARD_RADIUS), fill);

        ui.allocate_ui_at_rect(rect.shrink2(Vec2::splat(tokens::space::MD)), |ui| {
            ui.with_layout(egui::Layout::left_to_right(egui::Align::Center), |ui| {
                let (circle, _) = ui.allocate_exact_size(Vec2::splat(28.0), Sense::hover());
                ui.painter().circle_filled(
                    circle.center(),
                    14.0,
                    Color32::from_rgba_unmultiplied(accent.r(), accent.g(), accent.b(), 36),
                );
                ui.painter().circle_filled(circle.center(), 5.0, accent);
                ui.add_space(tokens::space::SM);
                ui.vertical(|ui| {
                    ui.label(
                        RichText::new(&group.title)
                            .size(tokens::type_size::TITLE_SM)
                            .color(theme.text_primary),
                    );
                    ui.label(
                        RichText::new(format!("{} messages", group.member_ids.len()))
                            .size(tokens::type_size::LABEL)
                            .color(theme.text_secondary),
                    );
                });
            });
        });
        ui.add_space(tokens::space::SM);
    }
}

fn accent_for(kind: CategoryGroupKind, theme: &ThemeTokens) -> Color32 {
    match kind {
        CategoryGroupKind::NeedsAttention => ACCENT_NEEDS_ATTENTION,
        CategoryGroupKind::Promotions => tokens::ACCENT_PROMOTIONS,
        CategoryGroupKind::Updates => tokens::ACCENT_UPDATES,
        CategoryGroupKind::WorkAndLearning => tokens::ACCENT_WORK_LEARNING,
        CategoryGroupKind::EverythingElse => theme.brand,
    }
}
