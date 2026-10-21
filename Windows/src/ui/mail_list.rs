//! Flat mail rows matching Android Material 3 ThreadRow / MailRowLayout.
//!
//! Design A inbox pane: category tabs (Primary | Promotional | Social),
//! mini-toolbar, flat rows with checkbox / avatar / unread weight /
//! priority badge / star. Selected = `row_selected` + optional brand accent bar.
//! No Action Required chip. No app emojis.

use desktop_core::priority::{MailListBadgeFlags, PriorityLevel};
use egui::{Align, Color32, FontId, Label, Pos2, RichText, Sense, Shape, Stroke, Vec2};

use crate::app_state::{AppState, MailRowModel};
use crate::navigation::NavDestination;
use crate::tokens::{
    self, ThemeTokens, ACCOUNT_INDICATOR_SIZE, AVATAR_SIZE, MAIL_ROW_MIN_HEIGHT, MAIL_ROW_PAD_H,
    MAIL_ROW_PAD_V,
};

const STAR_SLOT: f32 = 28.0;
const CHECK_SLOT: f32 = 24.0;
const TIME_RESERVE: f32 = 72.0;
const ACCENT_BAR_W: f32 = 3.0;

pub fn show_mail_list(ui: &mut egui::Ui, state: &mut AppState, theme: &ThemeTokens) {
    egui::Frame::none()
        .inner_margin(egui::Margin::symmetric(tokens::space::SM, tokens::space::XS))
        .show(ui, |ui| {
            show_category_tabs(ui, state, theme);
            ui.add_space(tokens::space::XXS);
            let count = state.filtered_messages().len();
            show_mini_toolbar(ui, state, theme, count);
        });

    let rows: Vec<MailRowModel> = state.filtered_messages().into_iter().cloned().collect();

    if rows.is_empty() {
        ui.vertical_centered(|ui| {
            ui.add_space(tokens::space::HUGE);
            ui.label(
                RichText::new("Nothing here")
                    .size(tokens::type_size::TITLE_MD)
                    .color(theme.text_primary),
            );
            ui.add_space(tokens::space::XS);
            ui.label(
                RichText::new("This folder has no messages.")
                    .size(tokens::type_size::BODY)
                    .color(theme.text_secondary),
            );
        });
        return;
    }

    let selected = state.layout.selected_message_id.clone();
    egui::ScrollArea::vertical()
        .id_salt("mail_list_scroll")
        .auto_shrink([false, false])
        .show(ui, |ui| {
            for row in &rows {
                let is_sel = selected.as_deref() == Some(row.id.as_str());
                match show_mail_row(ui, row, theme, is_sel) {
                    RowAction::Select => state.select_message(row.id.clone()),
                    RowAction::ToggleStar => state.toggle_star(&row.id),
                    RowAction::None => {}
                }
            }
        });
}

fn show_category_tabs(ui: &mut egui::Ui, state: &mut AppState, theme: &ThemeTokens) {
    // Design A: outline pills; active = primary_container + brand text.
    if !state.nav.destination.is_inbox_family() {
        return;
    }
    ui.horizontal(|ui| {
        ui.spacing_mut().item_spacing.x = tokens::space::XXS;
        for dest in NavDestination::LIST_TABS {
            let active = state.nav.destination == dest
                || (state.nav.destination == NavDestination::AllInbox
                    && dest == NavDestination::Primary);
            let label = RichText::new(dest.label())
                .size(tokens::type_size::LABEL_LG)
                .color(if active {
                    theme.brand
                } else {
                    theme.text_secondary
                })
                .strong_if(active);
            let fill = if active {
                theme.primary_container
            } else {
                Color32::TRANSPARENT
            };
            let btn = egui::Button::new(label)
                .fill(fill)
                .rounding(ThemeTokens::pill_rounding())
                .stroke(egui::Stroke::NONE);
            if ui.add(btn).clicked() {
                state.nav.select(dest);
                state.layout.selected_destination = dest;
            }
        }
    });
}

trait StrongIf {
    fn strong_if(self, on: bool) -> Self;
}

impl StrongIf for RichText {
    fn strong_if(self, on: bool) -> Self {
        if on {
            self.strong()
        } else {
            self
        }
    }
}

fn show_mini_toolbar(
    ui: &mut egui::Ui,
    _state: &mut AppState,
    theme: &ThemeTokens,
    visible_count: usize,
) {
    ui.horizontal(|ui| {
        ui.with_layout(egui::Layout::left_to_right(Align::Center), |ui| {
            // Select-all stub — visual only for Design A.
            let mut select_all = false;
            let _ = ui.checkbox(&mut select_all, "");
            ui.label(
                RichText::new("Select all")
                    .size(tokens::type_size::LABEL)
                    .color(theme.text_secondary),
            );
            ui.add_space(tokens::space::SM);
            if ui
                .add(
                    egui::Button::new(
                        RichText::new("Refresh")
                            .size(tokens::type_size::LABEL)
                            .color(theme.text_secondary),
                    )
                    .fill(Color32::TRANSPARENT)
                    .stroke(theme.hairline())
                    .rounding(ThemeTokens::pill_rounding()),
                )
                .on_hover_text("Refresh (stub)")
                .clicked()
            {
                // Stub — no network refresh in demo shell.
            }
        });
        ui.with_layout(egui::Layout::right_to_left(Align::Center), |ui| {
            ui.label(
                RichText::new(format!(
                    "{visible_count} message{}",
                    if visible_count == 1 { "" } else { "s" }
                ))
                .size(tokens::type_size::LABEL)
                .color(theme.text_secondary),
            );
        });
    });
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum RowAction {
    None,
    Select,
    ToggleStar,
}

fn show_mail_row(
    ui: &mut egui::Ui,
    row: &MailRowModel,
    theme: &ThemeTokens,
    selected: bool,
) -> RowAction {
    let desired = Vec2::new(ui.available_width(), MAIL_ROW_MIN_HEIGHT);
    let (rect, response) = ui.allocate_exact_size(desired, Sense::click());

    let mut action = RowAction::None;

    if ui.is_rect_visible(rect) {
        let painter = ui.painter().with_clip_rect(rect);
        painter.rect_filled(rect, 0.0, theme.interaction_fill(response.hovered(), selected));
        if selected {
            painter.rect_filled(
                egui::Rect::from_min_size(rect.min, Vec2::new(ACCENT_BAR_W, rect.height())),
                0.0,
                theme.brand,
            );
        }

        // One line for sender / time / star, one line for the preview. Clipped to the row.
        let cy = rect.center().y;
        let check = egui::Rect::from_center_size(
            egui::pos2(rect.left() + 16.0, cy),
            Vec2::splat(14.0),
        );
        painter.rect_stroke(check, 3.0, Stroke::new(1.0, theme.outline));

        let avatar_c = egui::pos2(rect.left() + 44.0, cy);
        painter.circle_filled(avatar_c, AVATAR_SIZE / 2.0, theme.primary_container);
        painter.text(
            avatar_c,
            egui::Align2::CENTER_CENTER,
            initial_char(&row.sender_name).to_string(),
            FontId::proportional(12.0),
            theme.on_primary_container,
        );

        let text_left = rect.left() + 66.0;
        let star_x = rect.right() - 16.0;
        let time_right = star_x - 16.0;
        let sender_color = if row.unread {
            theme.text_primary
        } else {
            theme.text_secondary
        };
        let sender = painter.layout_no_wrap(
            row.sender_name.clone(),
            FontId::proportional(14.0),
            sender_color,
        );
        painter.galley(
            egui::pos2(text_left, rect.top() + 8.0),
            sender,
            sender_color,
        );

        let time_color = if row.unread {
            theme.brand
        } else {
            theme.text_secondary
        };
        painter.text(
            egui::pos2(time_right, rect.top() + 10.0),
            egui::Align2::RIGHT_TOP,
            &row.time_label,
            FontId::proportional(12.0),
            time_color,
        );

        let mut preview = if row.snippet.is_empty() {
            row.subject.clone()
        } else {
            format!("{} — {}", row.subject, row.snippet)
        };
        if preview.chars().count() > 64 {
            preview = format!("{}…", preview.chars().take(64).collect::<String>());
        }
        let flags = MailListBadgeFlags::for_priority(row.priority);
        let badge = if flags.show_priority_badge {
            priority_short(row.priority)
        } else {
            ""
        };
        let badge_reserve = if badge.is_empty() { 0.0 } else { 52.0 };
        let preview_clip = egui::Rect::from_min_max(
            egui::pos2(text_left, rect.top() + 28.0),
            egui::pos2(time_right - badge_reserve - 8.0, rect.bottom() - 4.0),
        );
        let preview_painter = painter.with_clip_rect(preview_clip);
        preview_painter.text(
            egui::pos2(text_left, rect.top() + 30.0),
            egui::Align2::LEFT_TOP,
            preview,
            FontId::proportional(12.0),
            theme.text_secondary,
        );
        if !badge.is_empty() {
            painter.text(
                egui::pos2(time_right, rect.top() + 30.0),
                egui::Align2::RIGHT_TOP,
                badge,
                FontId::proportional(11.0),
                theme.status.warning,
            );
        }

        paint_star(
            &painter,
            egui::pos2(star_x, rect.top() + 16.0),
            5.5,
            if row.starred {
                theme.tertiary
            } else {
                theme.outline
            },
            row.starred,
        );

        let star_hit = egui::Rect::from_center_size(
            egui::pos2(star_x, rect.top() + 16.0),
            Vec2::splat(20.0),
        );
        let star_resp = ui.interact(star_hit, ui.id().with(("star", &row.id)), Sense::click());
        if star_resp.clicked() {
            action = RowAction::ToggleStar;
        }
    }

    if action == RowAction::None && response.clicked() {
        action = RowAction::Select;
    }
    action
}

fn mail_emphasis(text: &str, unread: bool, size: f32, theme: &ThemeTokens) -> RichText {
    let mut rt = RichText::new(text).size(size).color(theme.text_primary);
    if unread {
        rt = rt.strong();
    }
    rt
}

/// Company domain → letter-in-primaryContainer (logo stand-in until assets land);
/// personal sender → letter on surfaceContainerHigh.
fn paint_sender_avatar(ui: &mut egui::Ui, theme: &ThemeTokens, row: &MailRowModel) {
    let letter = if row.is_company {
        row.sender_domain
            .as_deref()
            .and_then(|d| d.chars().next())
            .unwrap_or_else(|| initial_char(&row.sender_name))
            .to_ascii_uppercase()
    } else {
        initial_char(&row.sender_name)
    };
    let avatar_color = if row.is_company || row.sender_domain.is_some() {
        theme.primary_container
    } else {
        theme.surface_container_high
    };
    let (avatar_rect, _) = ui.allocate_exact_size(Vec2::splat(AVATAR_SIZE), Sense::hover());
    ui.painter()
        .circle_filled(avatar_rect.center(), AVATAR_SIZE / 2.0, avatar_color);
    ui.painter().text(
        avatar_rect.center(),
        egui::Align2::CENTER_CENTER,
        letter.to_string(),
        FontId::proportional(16.0),
        theme.text_primary,
    );
}

fn paint_account_indicator(ui: &mut egui::Ui, theme: &ThemeTokens, account_label: &str) {
    let letter = initial_char(account_label);
    let (r, _) = ui.allocate_exact_size(Vec2::splat(ACCOUNT_INDICATOR_SIZE), Sense::hover());
    ui.painter()
        .circle_filled(r.center(), ACCOUNT_INDICATOR_SIZE / 2.0, theme.primary_container);
    ui.painter().text(
        r.center(),
        egui::Align2::CENTER_CENTER,
        letter.to_string(),
        FontId::proportional(12.0),
        theme.text_primary,
    );
}

fn initial_char(s: &str) -> char {
    s.chars()
        .find(|c| c.is_alphanumeric())
        .unwrap_or('?')
        .to_ascii_uppercase()
}

/// Geometric star (Material star glyph stand-in — not an app emoji).
pub(crate) fn paint_star(
    painter: &egui::Painter,
    center: Pos2,
    outer: f32,
    color: Color32,
    filled: bool,
) {
    use std::f32::consts::PI;
    let inner = outer * 0.4;
    let mut pts = Vec::with_capacity(10);
    for i in 0..10 {
        let ang = -PI / 2.0 + (i as f32) * PI / 5.0;
        let rad = if i % 2 == 0 { outer } else { inner };
        pts.push(center + Vec2::new(ang.cos() * rad, ang.sin() * rad));
    }
    painter.add(Shape::Path(egui::epaint::PathShape {
        points: pts,
        closed: true,
        fill: if filled { color } else { Color32::TRANSPARENT },
        stroke: if filled {
            Stroke::NONE.into()
        } else {
            Stroke::new(1.0, color).into()
        },
    }));
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
