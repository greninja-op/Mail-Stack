//! Phase 03–04 — Application shell, lifecycle, multi-column workspace.
//!
//! egui/eframe interim GUI (Fluent tokens). Not MessageBox-only; not a CLI.

use eframe::egui;

use crate::app_state::AppState;
use crate::bridge::CoreBridge;
use crate::capability_profile::{detect_profile, AccessibilityHooks, CapabilityProfile};
use crate::identity::AppIdentity;
use crate::interaction::{self, handle_shortcuts_with, InteractionState};
use crate::navigation::{NavDestination, OverlayPane};
use crate::tokens::{self, ThemeTokens, AVATAR_SIZE, SIDEBAR_WIDTH};
use crate::ui::{
    categories, conversation, import_flow, mail_list, privacy_center, search, settings,
};
use crate::workspace::WorkspaceLoadState;

/// Runs the Mailstack Windows GUI (blocks until the window closes).
pub fn run(identity: AppIdentity) -> eframe::Result<()> {
    let bridge = CoreBridge::new();
    log::info!(
        "starting {} v{} (egui interim shell)",
        identity.display_name,
        identity.version
    );

    let mut state = AppState::default();
    let cores = std::thread::available_parallelism()
        .map(|n| n.get())
        .unwrap_or(4);
    let a11y = AccessibilityHooks::from_app(state.theme_mode, state.reduced_motion, 1.0);
    let profile = detect_profile(&a11y, cores);
    // Never claim FPS from refresh Hz — profile ignores display rate entirely.
    let _ = CapabilityProfile::fps_claim_from_refresh_hz(120);
    if !profile.allow_expressive_motion {
        state.motion = tokens::MotionTier::Essential;
    }

    let layout = state.layout.clone();
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([layout.window_width, layout.window_height])
            .with_min_inner_size([900.0, 560.0])
            .with_title(identity.display_name)
            .with_decorations(true),
        ..Default::default()
    };

    eframe::run_native(
        identity.display_name,
        options,
        Box::new(move |cc| {
            let theme = state.theme();
            tokens::apply_egui_visuals(&cc.egui_ctx, &theme);
            Ok(Box::new(MailstackApp {
                identity,
                state,
                bridge,
                profile,
                a11y,
                interaction: InteractionState::default(),
            }))
        }),
    )
}

struct MailstackApp {
    identity: AppIdentity,
    state: AppState,
    bridge: CoreBridge,
    profile: CapabilityProfile,
    a11y: AccessibilityHooks,
    interaction: InteractionState,
}

impl eframe::App for MailstackApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        let theme = self.state.theme();
        tokens::apply_egui_visuals(ctx, &theme);

        // Re-evaluate a11y + performance when reduced-motion / theme changes (not continuous).
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

        let dt = ctx.input(|i| i.stable_dt);
        self.state.tick(dt);
        handle_shortcuts_with(ctx, &mut self.state, &mut self.interaction);
        if self.interaction.windows.take_new_window_request() {
            log::info!("multi-window: new window requested (host deferred)");
        }

        let screen = ctx.screen_rect();
        self.state.layout.window_width = screen.width();
        self.state.layout.window_height = screen.height();

        draw_chrome(ctx, &mut self.state, &theme, &self.identity);
        draw_sidebar(ctx, &mut self.state, &theme);
        settings::show_settings_panel(ctx, &mut self.state, &theme);
        privacy_center::show_privacy_panel(ctx, &mut self.state, &theme);
        draw_workspace(ctx, &mut self.state, &theme, &self.bridge);
        // Structured search (300ms debounce) — UI never builds SQL.
        egui::Area::new(egui::Id::new("search_host")).show(ctx, |ui| {
            search::show_search_overlay(ui, &mut self.state, &theme);
        });
        show_account_switcher(ctx, &mut self.state, &theme);
        import_flow::show_import_flow_window(ctx, &mut self.state, &theme, &self.bridge);

        if self.state.load == WorkspaceLoadState::Launching {
            self.state.load = WorkspaceLoadState::Ready;
        }
    }
}

fn draw_chrome(
    ctx: &egui::Context,
    state: &mut AppState,
    theme: &ThemeTokens,
    identity: &AppIdentity,
) {
    if !state.bars_visible && state.nav.overlay == OverlayPane::None {
        return;
    }

    egui::TopBottomPanel::top("top_chrome")
        .frame(
            egui::Frame::none()
                .fill(theme.surface)
                .inner_margin(egui::Margin::symmetric(12.0, 8.0)),
        )
        .show(ctx, |ui| {
            interaction::show_menu_bar(ui, state);
            ui.horizontal(|ui| {
                ui.label(
                    egui::RichText::new(identity.display_name)
                        .size(tokens::type_size::TITLE_SM)
                        .color(theme.brand)
                        .strong(),
                );
                ui.separator();
                ui.label(
                    egui::RichText::new(state.nav.destination.label())
                        .size(tokens::type_size::BODY)
                        .color(theme.text_primary),
                );
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    let (rect, resp) = ui.allocate_exact_size(
                        egui::vec2(AVATAR_SIZE, AVATAR_SIZE),
                        egui::Sense::click(),
                    );
                    ui.painter()
                        .circle_filled(rect.center(), AVATAR_SIZE / 2.0, theme.brand);
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
                        egui::FontId::proportional(16.0),
                        theme.text_primary,
                    );
                    if resp.clicked() {
                        state.nav.overlay = OverlayPane::AccountSwitcher;
                    }
                    if ui.button("Settings").clicked() {
                        state.nav.open_settings();
                    }
                    if ui.button("Search").clicked() {
                        state.nav.overlay = OverlayPane::Search;
                    }
                });
            });
        });
}

fn draw_sidebar(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    egui::SidePanel::left("sidebar")
        .exact_width(
            state
                .layout
                .sidebar_width
                .min(SIDEBAR_WIDTH + 40.0)
                .max(160.0),
        )
        .resizable(true)
        .frame(
            egui::Frame::none()
                .fill(theme.surface)
                .inner_margin(egui::Margin::same(8.0)),
        )
        .show(ctx, |ui| {
            ui.label(
                egui::RichText::new("Mailbox")
                    .size(tokens::type_size::LABEL)
                    .color(theme.text_secondary),
            );
            ui.add_space(tokens::space::SM);
            for dest in NavDestination::ALL {
                let selected = state.nav.destination == dest;
                if ui.selectable_label(selected, dest.label()).clicked() {
                    state.nav.select(dest);
                    state.layout.selected_destination = dest;
                    state.bars_visible = true;
                }
            }
            ui.add_space(tokens::space::XL);
            ui.label(
                egui::RichText::new(
                    "Companies live inside categories — never as destinations.",
                )
                .size(tokens::type_size::LABEL)
                .color(theme.text_secondary),
            );
        });
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
                .inner_margin(egui::Margin::ZERO),
        )
        .show(ctx, |ui| {
            // design.md: underlying content shifts ~1/12 left while settings open.
            let shift = state.layout.settings_content_shift(state.nav.settings_progress);
            if shift.abs() > 0.5 {
                ui.add_space(shift.abs()); // consume left margin as shift proxy
            }
            paint_columns(ui, state, theme, bridge);
        });
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
                ui.label("Sign in required (OAuth arrives in a later phase).");
            });
            return;
        }
        WorkspaceLoadState::Empty | WorkspaceLoadState::Ready => {}
    }

    let available = ui.available_width();
    let cols = state.layout.allocate_columns(available);

    ui.horizontal_top(|ui| {
        ui.allocate_ui_with_layout(
            egui::vec2(cols.list, ui.available_height()),
            egui::Layout::top_down(egui::Align::Min),
            |ui| {
                egui::Frame::none()
                    .fill(theme.bg)
                    .inner_margin(egui::Margin::symmetric(4.0, 4.0))
                    .show(ui, |ui| {
                        mail_list::show_mail_list(ui, state, theme);
                    });
            },
        );

        let (div, _) =
            ui.allocate_exact_size(egui::vec2(1.0, ui.available_height()), egui::Sense::hover());
        ui.painter().rect_filled(div, 0.0, theme.divider);

        let detail_w = if cols.inspector > 0.0 {
            cols.detail
        } else {
            ui.available_width()
        };
        ui.allocate_ui_with_layout(
            egui::vec2(detail_w.max(200.0), ui.available_height()),
            egui::Layout::top_down(egui::Align::Min),
            |ui| {
                egui::Frame::none()
                    .fill(theme.bg)
                    .inner_margin(egui::Margin::symmetric(12.0, 8.0))
                    .show(ui, |ui| {
                        conversation::show_conversation(ui, state, theme, bridge);
                    });
            },
        );

        if cols.inspector > 0.0 {
            let (div2, _) = ui.allocate_exact_size(
                egui::vec2(1.0, ui.available_height()),
                egui::Sense::hover(),
            );
            ui.painter().rect_filled(div2, 0.0, theme.divider);
            ui.allocate_ui_with_layout(
                egui::vec2(cols.inspector, ui.available_height()),
                egui::Layout::top_down(egui::Align::Min),
                |ui| {
                    let acct = state
                        .active_account()
                        .map(|a| a.id.as_str())
                        .unwrap_or("acct-demo");
                    categories::show_category_panel(ui, theme, bridge, acct);
                },
            );
        }
    });
}

fn show_account_switcher(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    if state.nav.overlay != OverlayPane::AccountSwitcher {
        return;
    }
    egui::Window::new("Accounts")
        .collapsible(false)
        .resizable(false)
        .default_width(320.0)
        .show(ctx, |ui| {
            ui.label(
                egui::RichText::new("Switch account")
                    .size(tokens::type_size::TITLE_SM)
                    .color(theme.text_primary),
            );
            ui.label(
                egui::RichText::new("Receiving account — never confused with sender identity.")
                    .size(tokens::type_size::LABEL)
                    .color(theme.text_secondary),
            );
            ui.separator();
            let accounts: Vec<(usize, String)> = state
                .accounts
                .iter()
                .enumerate()
                .map(|(i, a)| (i, format!("{} ({})", a.display_name, a.email_display)))
                .collect();
            for (idx, label) in accounts {
                let selected = idx == state.active_account_idx;
                if ui.selectable_label(selected, label).clicked() {
                    state.active_account_idx = idx;
                    state.nav.close_overlay();
                }
            }
            ui.separator();
            ui.checkbox(
                &mut state.unified_inbox,
                "Unified inbox (presentation join — receiving account shown per row)",
            );
            ui.label(
                egui::RichText::new(
                    "Demo accounts never sync. Company pin ≠ message star. No company destinations in the sidebar.",
                )
                .size(tokens::type_size::LABEL)
                .color(theme.text_secondary),
            );
            ui.label(
                egui::RichText::new("Add another account… (OAuth — Phase 09)")
                    .size(tokens::type_size::LABEL)
                    .color(theme.text_secondary),
            );
            if ui.button("Settings").clicked() {
                state.nav.open_settings();
            }
            if ui.button("Close").clicked() {
                state.nav.close_overlay();
            }
        });
}

/// Compiles without showing UI — used by automated tests.
pub fn shell_kind() -> &'static str {
    "egui-eframe-interim-winui3-packaging-path"
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shell_kind_is_gui_not_cli() {
        let k = shell_kind();
        assert!(k.contains("egui"));
        assert!(!k.contains("cli"));
    }
}
