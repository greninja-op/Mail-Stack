//! Application shell — Design A layout on Design B / ThemeTokens colours.
//!
//! Interim egui host (WinUI 3 packaging path later). Not a CLI.

use eframe::egui;

use crate::app_state::AppState;
use crate::brand;
use crate::bridge::CoreBridge;
use crate::capability_profile::{detect_profile, AccessibilityHooks, CapabilityProfile};
use crate::identity::AppIdentity;
use crate::interaction::{handle_shortcuts_with, InteractionState};
use crate::navigation::{NavDestination, OverlayPane, WorkspaceSurface};
use crate::tokens::{self, ThemeMode, ThemeTokens, AVATAR_SIZE};
use crate::ui::{
    categories, compose, conversation, import_flow, mail_list, privacy_center, search, settings,
};
use crate::workspace::{WorkspaceLayout, WorkspaceLoadState};

/// Runs the Mailstack Windows GUI (blocks until the window closes).
pub fn run(identity: AppIdentity) -> eframe::Result<()> {
    let bridge = CoreBridge::new();
    log::info!(
        "starting {} v{} (Design A egui shell)",
        identity.display_name,
        identity.version
    );

    let mut state = AppState::default();
    let cores = std::thread::available_parallelism()
        .map(|n| n.get())
        .unwrap_or(4);
    let a11y = AccessibilityHooks::from_app(state.theme_mode, state.reduced_motion, 1.0);
    let profile = detect_profile(&a11y, cores);
    let _ = CapabilityProfile::fps_claim_from_refresh_hz(120);
    if !profile.allow_expressive_motion {
        state.motion = tokens::MotionTier::Essential;
    }

    let layout = state.layout.clone();
    let mut viewport = egui::ViewportBuilder::default()
        .with_inner_size([layout.window_width, layout.window_height])
        .with_min_inner_size([960.0, 600.0])
        .with_title(identity.display_name)
        .with_decorations(false);
    if let Some(icon) = brand::window_icon() {
        viewport = viewport.with_icon(icon);
    }

    let options = eframe::NativeOptions {
        viewport,
        ..Default::default()
    };

    eframe::run_native(
        identity.display_name,
        options,
        Box::new(move |cc| {
            let theme = state.theme();
            tokens::apply_egui_visuals(&cc.egui_ctx, &theme);
            let logo = brand::upload_logo_texture(&cc.egui_ctx);
            Ok(Box::new(MailstackApp {
                identity,
                state,
                bridge,
                profile,
                a11y,
                interaction: InteractionState::default(),
                applied_theme: theme.mode,
                logo,
            }))
        }),
    )
}

struct MailstackApp {
    #[allow(dead_code)]
    identity: AppIdentity,
    state: AppState,
    bridge: CoreBridge,
    profile: CapabilityProfile,
    a11y: AccessibilityHooks,
    interaction: InteractionState,
    /// Only re-apply egui visuals when this changes (prevents per-frame shake).
    applied_theme: ThemeMode,
    logo: Option<egui::TextureHandle>,
}

impl eframe::App for MailstackApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        let theme = self.state.theme();
        if theme.mode != self.applied_theme {
            tokens::apply_egui_visuals(ctx, &theme);
            self.applied_theme = theme.mode;
        }

        self.a11y = AccessibilityHooks::from_app(
            self.state.theme_mode,
            self.state.reduced_motion,
            self.a11y.large_text_scale,
        );
        let preferred = if self.profile.allow_expressive_motion {
            tokens::MotionTier::Standard
        } else {
            tokens::MotionTier::Essential
        };
        self.state.motion = self.a11y.effective_motion(preferred);

        // No per-frame layout mutation: settings width is fixed; bars stay mounted.
        let dt = ctx.input(|i| i.stable_dt);
        self.state.tick(dt);

        handle_shortcuts_with(ctx, &mut self.state, &mut self.interaction);
        if self.interaction.windows.take_new_window_request() {
            log::info!("multi-window: new window requested (host deferred)");
        }

        let screen = ctx.screen_rect();
        let w = screen.width();
        let h = screen.height();
        if (self.state.layout.window_width - w).abs() > 0.5 {
            self.state.layout.window_width = w;
        }
        if (self.state.layout.window_height - h).abs() > 0.5 {
            self.state.layout.window_height = h;
        }

        // Bars always visible — never remount TopBottomPanel based on scroll.
        self.state.bars_visible = true;

        draw_chrome(ctx, &mut self.state, &theme);
        draw_sidebar(ctx, &mut self.state, &theme);
        settings::show_settings_panel(ctx, &mut self.state, &theme);
        privacy_center::show_privacy_panel(ctx, &mut self.state, &theme);
        draw_workspace(ctx, &mut self.state, &theme, &self.bridge);
        if self.state.nav.overlay == OverlayPane::Search {
            egui::Area::new(egui::Id::new("search_host"))
                .anchor(egui::Align2::CENTER_TOP, egui::vec2(0.0, 56.0))
                .show(ctx, |ui| {
                    search::show_search_overlay(ui, &mut self.state, &theme);
                });
        }
        show_account_switcher(ctx, &mut self.state, &theme);
        compose::show_compose_window(ctx, &mut self.state, &theme);
        import_flow::show_import_flow_window(ctx, &mut self.state, &theme, &self.bridge);

        if self.state.load == WorkspaceLoadState::Launching {
            self.state.load = WorkspaceLoadState::Ready;
        }
    }
}

/// Compact top toolbar: search pill, refresh, settings, avatar.
/// No large content logo / brand wordmark (OS icon + sidebar mark only).
fn draw_chrome(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    // Custom title bar (native decorations off) so the caption matches the canvas.
    egui::TopBottomPanel::top("top_chrome")
        .exact_height(52.0)
        .frame(
            egui::Frame::none()
                .fill(theme.bg)
                .inner_margin(egui::Margin::symmetric(12.0, 8.0)),
        )
        .show(ctx, |ui| {
            ui.horizontal_centered(|ui| {
                let drag = ui.allocate_response(
                    egui::vec2(88.0, 28.0),
                    egui::Sense::click_and_drag(),
                );
                ui.painter().text(
                    drag.rect.left_center(),
                    egui::Align2::LEFT_CENTER,
                    "Mail Stack",
                    egui::FontId::proportional(13.0),
                    theme.text_primary,
                );
                if drag.double_clicked() {
                    let maximized = ui.input(|i| i.viewport().maximized.unwrap_or(false));
                    ui.ctx()
                        .send_viewport_cmd(egui::ViewportCommand::Maximized(!maximized));
                } else if drag.drag_started() {
                    ui.ctx().send_viewport_cmd(egui::ViewportCommand::StartDrag);
                }
                ui.add_space(tokens::space::SM);
                // Trailing cluster is reserved first so search cannot push it off the window.
                let trailing_w = 340.0;
                let search_w = (ui.available_width() - trailing_w).clamp(120.0, 480.0);
                egui::Frame::none()
                    .fill(theme.surface)
                    .rounding(ThemeTokens::pill_rounding())
                    .stroke(theme.hairline())
                    .inner_margin(egui::Margin::symmetric(12.0, 4.0))
                    .show(ui, |ui| {
                        ui.set_width(search_w);
                        let resp = ui.add(
                            egui::TextEdit::singleline(&mut state.search_text)
                                .hint_text("Search mail…")
                                .desired_width(search_w - 8.0)
                                .frame(false),
                        );
                        if resp.changed() {
                            state.on_search_input();
                        }
                    });

                ui.allocate_ui_with_layout(
                    egui::vec2(trailing_w, 36.0),
                    egui::Layout::right_to_left(egui::Align::Center),
                    |ui| {
                    let ctx = ui.ctx().clone();
                    window_btn(ui, theme, "✕", move || {
                        ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                    });
                    let ctx = ui.ctx().clone();
                    let maximized = ui.input(|i| i.viewport().maximized.unwrap_or(false));
                    window_btn(ui, theme, "□", move || {
                        ctx.send_viewport_cmd(egui::ViewportCommand::Maximized(!maximized));
                    });
                    let ctx = ui.ctx().clone();
                    window_btn(ui, theme, "—", move || {
                        ctx.send_viewport_cmd(egui::ViewportCommand::Minimized(true));
                    });
                    ui.add_space(tokens::space::SM);
                    let (rect, resp) = ui.allocate_exact_size(
                        egui::vec2(AVATAR_SIZE, AVATAR_SIZE),
                        egui::Sense::click(),
                    );
                    ui.painter().circle_filled(
                        rect.center(),
                        AVATAR_SIZE / 2.0,
                        theme.primary_container,
                    );
                    let letter = state
                        .active_account()
                        .map(|a| {
                            a.display_name
                                .chars()
                                .next()
                                .unwrap_or('M')
                                .to_ascii_uppercase()
                        })
                        .unwrap_or('M');
                    ui.painter().text(
                        rect.center(),
                        egui::Align2::CENTER_CENTER,
                        letter.to_string(),
                        egui::FontId::proportional(12.0),
                        theme.on_primary_container,
                    );
                    if resp.clicked() {
                        state.nav.overlay = OverlayPane::AccountSwitcher;
                    }
                    resp.on_hover_text("Accounts");

                    ui.add_space(tokens::space::XS);
                    m3_icon_button(ui, theme, "Settings", || {
                        state.nav.open_settings();
                    });
                    m3_icon_button(ui, theme, "Refresh", || {
                        log::info!("refresh requested (stub)");
                    });
                },
                );
            });
        });
}

fn window_btn(ui: &mut egui::Ui, theme: &ThemeTokens, label: &str, on_click: impl FnOnce()) {
    let text = egui::RichText::new(label)
        .size(tokens::type_size::LABEL_LG)
        .color(theme.text_secondary);
    if ui
        .add(
            egui::Button::new(text)
                .fill(egui::Color32::TRANSPARENT)
                .min_size(egui::vec2(36.0, 28.0)),
        )
        .clicked()
    {
        on_click();
    }
}

fn m3_icon_button(ui: &mut egui::Ui, theme: &ThemeTokens, label: &str, on_click: impl FnOnce()) {
    let text = egui::RichText::new(label)
        .size(tokens::type_size::LABEL_LG)
        .color(theme.text_secondary);
    if ui
        .add(
            egui::Button::new(text)
                .fill(egui::Color32::TRANSPARENT)
                .rounding(ThemeTokens::pill_rounding())
                .min_size(egui::vec2(0.0, 32.0)),
        )
        .clicked()
    {
        on_click();
    }
}

fn draw_sidebar(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    let mut live_width = state.layout.sidebar_width;
    egui::SidePanel::left("sidebar")
        .default_width(state.layout.sidebar_width)
        .width_range(WorkspaceLayout::SIDEBAR_MIN..=WorkspaceLayout::SIDEBAR_MAX)
        .resizable(true)
        .frame(
            egui::Frame::none()
                .fill(theme.bg)
                .inner_margin(egui::Margin::symmetric(12.0, 12.0)),
        )
        .show(ctx, |ui| {
            live_width = ui.available_width() + 24.0;

            let compose = egui::RichText::new("Compose")
                .size(tokens::type_size::BODY_LG)
                .strong()
                .color(theme.on_brand);
            if ui
                .add(
                    egui::Button::new(compose)
                        .fill(theme.brand)
                        .rounding(ThemeTokens::pill_rounding())
                        .min_size(egui::vec2(ui.available_width(), 40.0)),
                )
                .clicked()
            {
                state.nav.open_compose();
            }

            ui.add_space(tokens::space::LG);
            ui.label(
                egui::RichText::new("Mail")
                    .size(tokens::type_size::LABEL_SM)
                    .color(theme.text_secondary),
            );
            ui.add_space(tokens::space::XXS);

            for dest in NavDestination::SIDEBAR {
                let selected = state.nav.surface == WorkspaceSurface::Mail
                    && match dest {
                        NavDestination::AllInbox => state.nav.destination.is_inbox_family(),
                        other => state.nav.destination == other,
                    };
                if sidebar_pill(ui, theme, dest.label(), selected) {
                    if dest == NavDestination::AllInbox {
                        state.nav.open_inbox();
                    } else {
                        state.nav.select(dest);
                    }
                    state.layout.selected_destination = state.nav.destination;
                }
                ui.add_space(tokens::space::XXS);
            }

            ui.add_space(tokens::space::LG);
            ui.label(
                egui::RichText::new("Organize")
                    .size(tokens::type_size::LABEL_SM)
                    .color(theme.text_secondary),
            );
            ui.add_space(tokens::space::XXS);
            let cats_selected = state.nav.surface == WorkspaceSurface::Categories;
            if sidebar_pill(ui, theme, "Categories", cats_selected) {
                state.nav.open_categories();
            }
        });
    state.layout.set_sidebar_width(live_width);
}

fn sidebar_pill(ui: &mut egui::Ui, theme: &ThemeTokens, label: &str, selected: bool) -> bool {
    let hover_id = ui.make_persistent_id(("sidebar_pill", label));
    let hovered = ui
        .ctx()
        .data(|d| d.get_temp::<bool>(hover_id).unwrap_or(false));
    let fill = if selected {
        theme.primary_container
    } else if hovered {
        theme.row_hover
    } else {
        egui::Color32::TRANSPARENT
    };
    let response = egui::Frame::none()
        .fill(fill)
        .rounding(ThemeTokens::pill_rounding())
        .inner_margin(egui::Margin::symmetric(12.0, 6.0))
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            let color = if selected {
                theme.on_primary_container
            } else {
                theme.text_secondary
            };
            let mut text = egui::RichText::new(label)
                .size(tokens::type_size::BODY)
                .color(color);
            if selected {
                text = text.strong();
            }
            ui.label(text);
        })
        .response
        .interact(egui::Sense::click());
    ui.ctx()
        .data_mut(|d| d.insert_temp(hover_id, response.hovered()));
    response.clicked()
}

fn draw_workspace(
    ctx: &egui::Context,
    state: &mut AppState,
    theme: &ThemeTokens,
    bridge: &CoreBridge,
) {
    egui::CentralPanel::default()
        .frame(
            egui::Frame::none()
                .fill(theme.bg)
                .inner_margin(egui::Margin::same(WorkspaceLayout::CANVAS_PAD)),
        )
        .show(ctx, |ui| {
            paint_columns(ui, state, theme, bridge);
        });
}

fn panel_frame(theme: &ThemeTokens) -> egui::Frame {
    egui::Frame::none()
        .fill(theme.surface_container_lowest)
        .rounding(egui::Rounding::same(WorkspaceLayout::PANEL_RADIUS))
        .stroke(theme.hairline())
        .inner_margin(egui::Margin::same(tokens::space::SM))
}

fn paint_columns(
    ui: &mut egui::Ui,
    state: &mut AppState,
    theme: &ThemeTokens,
    bridge: &CoreBridge,
) {
    match &state.load {
        WorkspaceLoadState::Loading | WorkspaceLoadState::Launching => {
            ui.vertical_centered(|ui| {
                ui.add_space(80.0);
                ui.spinner();
                ui.label(egui::RichText::new("Loading…").color(theme.text_secondary));
            });
            return;
        }
        WorkspaceLoadState::Error { message } => {
            let message = message.clone();
            ui.vertical_centered(|ui| {
                ui.add_space(80.0);
                ui.label(
                    egui::RichText::new("Something went wrong")
                        .size(tokens::type_size::TITLE_MD)
                        .color(theme.text_primary),
                );
                ui.label(egui::RichText::new(message).color(theme.text_secondary));
                if ui.button("Try again").clicked() {
                    state.load = WorkspaceLoadState::Ready;
                }
            });
            return;
        }
        WorkspaceLoadState::Offline => {
            ui.vertical_centered(|ui| {
                ui.add_space(80.0);
                ui.label("Offline — showing last known mail when available.");
            });
            return;
        }
        WorkspaceLoadState::AuthRequired => {
            ui.vertical_centered(|ui| {
                ui.add_space(80.0);
                ui.label("Sign in required.");
            });
            return;
        }
        WorkspaceLoadState::Empty | WorkspaceLoadState::Ready => {}
    }

    if state.nav.surface == WorkspaceSurface::Categories {
        panel_frame(theme).show(ui, |ui| {
            egui::ScrollArea::vertical()
                .id_salt("categories_surface_scroll")
                .auto_shrink([false, false])
                .show(ui, |ui| {
                    categories::show_category_groups(ui, state, theme, bridge);
                });
        });
        return;
    }

    let available = ui.available_width();
    let height = ui.available_height();
    let collapsed = state.layout.is_two_pane_collapsed();
    let show_reader_only = collapsed && state.layout.selected_message_id.is_some();
    let show_list_only = collapsed && state.layout.selected_message_id.is_none();

    if show_reader_only {
        panel_frame(theme).show(ui, |ui| {
            ui.set_min_height(height - 4.0);
            if ui
                .add(
                    egui::Button::new(
                        egui::RichText::new("← Back to list")
                            .size(tokens::type_size::LABEL_LG)
                            .color(theme.brand),
                    )
                    .fill(egui::Color32::TRANSPARENT),
                )
                .clicked()
            {
                state.layout.selected_message_id = None;
            }
            ui.add_space(tokens::space::XS);
            conversation::show_conversation(ui, state, theme, bridge);
        });
        return;
    }

    if show_list_only {
        panel_frame(theme).show(ui, |ui| {
            ui.set_min_height(height - 4.0);
            paint_list_panel(ui, state, theme);
        });
        return;
    }

    // Size columns against the screen edge. The panel's max rect can report
    // a width that still includes the sidebar, which pushes the reader off-screen.
    let screen = ui.ctx().screen_rect();
    let row_w = (screen.right() - ui.max_rect().left() - WorkspaceLayout::CANVAS_PAD).max(0.0);
    let cols = state.layout.allocate_mail_columns(row_w);
    let gap = WorkspaceLayout::PANEL_GAP;
    let list_w = cols.list.min((row_w - gap - 240.0).max(220.0));
    let detail_w = (row_w - list_w - gap).max(0.0);

    ui.spacing_mut().item_spacing.x = 0.0;
    ui.horizontal_top(|ui| {
        ui.spacing_mut().item_spacing.x = 0.0;
        ui.allocate_ui_with_layout(
            egui::vec2(list_w, height),
            egui::Layout::top_down(egui::Align::Min),
            |ui| {
                panel_frame(theme).show(ui, |ui| {
                    ui.set_min_height(height - 4.0);
                    ui.set_max_width(list_w);
                    paint_list_panel(ui, state, theme);
                });
            },
        );

        // Resizable splitter in the panel gap.
        let (sep_rect, sep_resp) = ui.allocate_exact_size(
            egui::vec2(gap, height),
            egui::Sense::click_and_drag(),
        );
        let mid_x = sep_rect.center().x;
        let sep_color = if sep_resp.hovered() || sep_resp.dragged() {
            theme.brand
        } else {
            theme.divider
        };
        ui.painter().line_segment(
            [
                egui::pos2(mid_x, sep_rect.top() + 8.0),
                egui::pos2(mid_x, sep_rect.bottom() - 8.0),
            ],
            egui::Stroke::new(1.0_f32, sep_color),
        );
        if sep_resp.hovered() || sep_resp.dragged() {
            ui.ctx().set_cursor_icon(egui::CursorIcon::ResizeHorizontal);
        }
        if sep_resp.dragged() {
            state
                .layout
                .resize_list_by(sep_resp.drag_delta().x, available);
        }

        let detail_w = detail_w.min(ui.available_width());
        let reader_rect = egui::Rect::from_min_size(
            ui.next_widget_position(),
            egui::vec2(detail_w, height),
        );
        ui.allocate_ui_at_rect(reader_rect, |ui| {
            ui.set_clip_rect(reader_rect);
            panel_frame(theme).show(ui, |ui| {
                ui.set_min_height(height - 4.0);
                conversation::show_conversation(ui, state, theme, bridge);
            });
        });
    });
}

/// List panel host — category tabs live in `mail_list` (single strip).
fn paint_list_panel(ui: &mut egui::Ui, state: &mut AppState, theme: &ThemeTokens) {
    mail_list::show_mail_list(ui, state, theme);
}

fn show_account_switcher(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    if state.nav.overlay != OverlayPane::AccountSwitcher {
        return;
    }
    egui::Window::new("Accounts")
        .collapsible(false)
        .resizable(false)
        .anchor(egui::Align2::RIGHT_TOP, egui::vec2(-16.0, 56.0))
        .frame(
            egui::Frame::none()
                .fill(theme.surface_elevated)
                .rounding(ThemeTokens::card_rounding())
                .inner_margin(egui::Margin::same(16.0))
                .stroke(theme.hairline()),
        )
        .show(ctx, |ui| {
            ui.label(
                egui::RichText::new("Switch account")
                    .size(tokens::type_size::TITLE_SM)
                    .strong()
                    .color(theme.text_primary),
            );
            ui.add_space(tokens::space::SM);
            for (i, acct) in state.accounts.clone().into_iter().enumerate() {
                let selected = i == state.active_account_idx;
                let label = format!("{}  {}", acct.display_name, acct.email_display);
                if ui.selectable_label(selected, label).clicked() {
                    state.active_account_idx = i;
                    state.nav.overlay = OverlayPane::None;
                }
            }
            ui.separator();
            if ui.button("Close").clicked() {
                state.nav.overlay = OverlayPane::None;
            }
        });
}

pub fn shell_kind() -> &'static str {
    "design-a-egui"
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shell_kind_is_design_a() {
        assert!(shell_kind().contains("design-a"));
    }
}
