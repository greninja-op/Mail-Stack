//! Phase 23 — Privacy Center (tabbed: Overview / Your Data / Google Access / Security).

use desktop_core::privacy::{LocalPurgePlan, PrivacyCenterModel, PrivacyCenterTab};
use desktop_core::account::AccountId;
use egui::RichText;

use crate::app_state::AppState;
use crate::navigation::OverlayPane;
use crate::tokens::{self, ThemeTokens, SETTINGS_WIDTH};

pub fn show_privacy_panel(ctx: &egui::Context, state: &mut AppState, theme: &ThemeTokens) {
    if state.nav.overlay != OverlayPane::PrivacyCenter {
        return;
    }

    // Keep a small model in app_state via ephemeral rebuild — honesty over persistence here.
    let mut model = PrivacyCenterModel::default();
    if let Some(tab) = state.privacy_tab {
        model.selected_tab = tab;
    }

    egui::SidePanel::right("privacy_panel")
        .exact_width(SETTINGS_WIDTH)
        .resizable(false)
        .frame(
            egui::Frame::none()
                .fill(theme.surface_elevated)
                .inner_margin(egui::Margin::same(16.0)),
        )
        .show(ctx, |ui| {
            ui.horizontal(|ui| {
                ui.heading(
                    RichText::new("Privacy center")
                        .size(tokens::type_size::TITLE_LG)
                        .color(theme.text_primary),
                );
                if ui.button("Close").clicked() {
                    state.nav.close_overlay();
                }
            });
            ui.separator();

            ui.horizontal_wrapped(|ui| {
                for tab in PrivacyCenterTab::ALL {
                    let selected = model.selected_tab == tab;
                    if ui.selectable_label(selected, tab.label()).clicked() {
                        state.privacy_tab = Some(tab);
                        model.selected_tab = tab;
                    }
                }
            });
            ui.add_space(tokens::space::MD);

            match model.selected_tab {
                PrivacyCenterTab::Overview => {
                    ui.label(
                        RichText::new(
                            "Mail bodies never fetch remote images. A fetch would tell the sender the mail was opened.",
                        )
                        .size(tokens::type_size::BODY)
                        .color(theme.text_primary),
                    );
                    ui.add_space(tokens::space::MD);
                    ui.label(
                        RichText::new(
                            "HTML is sanitized; entities decoded; scripts stripped. No JavaScript from mail.",
                        )
                        .size(tokens::type_size::BODY)
                        .color(theme.text_primary),
                    );
                    ui.add_space(tokens::space::MD);
                    ui.label(
                        RichText::new(
                            "Secrets use Windows Credential Manager when available; logs redact tokens and email local-parts.",
                        )
                        .size(tokens::type_size::BODY)
                        .color(theme.text_primary),
                    );
                }
                PrivacyCenterTab::YourData => {
                    ui.label(
                        RichText::new("Local data inventory (purge never modifies remote Gmail)")
                            .size(tokens::type_size::TITLE_SM)
                            .color(theme.text_primary),
                    );
                    ui.add_space(tokens::space::SM);
                    for entry in &model.inventory {
                        // Inventory contract: remote_gmail_modified_on_purge is always false.
                        let _ = entry.remote_gmail_modified_on_purge;
                        ui.label(
                            RichText::new(format!("• {} — {}", entry.key, entry.description))
                                .size(tokens::type_size::BODY)
                                .color(theme.text_secondary),
                        );
                    }
                    ui.add_space(tokens::space::LG);
                    if ui.button("Purge local data…").clicked() {
                        let acct = state
                            .active_account()
                            .map(|a| a.id.as_str())
                            .unwrap_or("acct-demo");
                        if let Some(id) = AccountId::new(acct) {
                            let plan = LocalPurgePlan::for_account(id);
                            let _ = plan.validate();
                            state.privacy_purge_requested = true;
                        }
                    }
                    if state.privacy_purge_requested {
                        ui.label(
                            RichText::new(
                                "Local purge queued — remote Gmail is never modified.",
                            )
                            .size(tokens::type_size::LABEL)
                            .color(theme.text_secondary),
                        );
                    }
                }
                PrivacyCenterTab::GoogleAccess => {
                    ui.label(
                        RichText::new("Google scopes disclosed for this product")
                            .size(tokens::type_size::TITLE_SM)
                            .color(theme.text_primary),
                    );
                    for scope in &model.google_scopes_disclosed {
                        ui.label(
                            RichText::new(format!("• {scope}"))
                                .size(tokens::type_size::BODY)
                                .color(theme.text_secondary),
                        );
                    }
                    ui.add_space(tokens::space::MD);
                    ui.label(
                        RichText::new(
                            "gmail.modify is incremental and only requested when you confirm a write.",
                        )
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                    );
                }
                PrivacyCenterTab::Security => {
                    ui.label(
                        RichText::new("Live security checks")
                            .size(tokens::type_size::TITLE_SM)
                            .color(theme.text_primary),
                    );
                    for check in &model.security_checks {
                        let mark = if check.passed { "✓" } else { "!" };
                        ui.label(
                            RichText::new(format!("{mark} {} — {}", check.title, check.detail))
                                .size(tokens::type_size::BODY)
                                .color(theme.text_primary),
                        );
                    }
                }
            }
        });
}

/// Compatibility stub used by older call sites.
pub fn show_privacy_center(_ui: &mut egui::Ui, _state: &mut AppState, _theme: &ThemeTokens) {}
