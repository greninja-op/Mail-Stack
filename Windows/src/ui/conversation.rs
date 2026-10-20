//! Phase 14 + 17/19 — Conversation / message detail viewer.
//!
//! Priority explainability + conversation intelligence card. HTML sanitized;
//! remote images never fetched.

use desktop_core::conversation::ConversationSignals;
use egui::RichText;

use crate::app_state::AppState;
use crate::bridge::CoreBridge;
use crate::tokens::{self, ThemeTokens};

pub fn show_conversation(
    ui: &mut egui::Ui,
    state: &mut AppState,
    theme: &ThemeTokens,
    bridge: &CoreBridge,
) {
    let Some(msg) = state.selected_message().cloned() else {
        ui.vertical_centered(|ui| {
            ui.add_space(64.0);
            ui.label(
                RichText::new("Select a conversation")
                    .size(tokens::type_size::TITLE_MD)
                    .color(theme.text_secondary),
            );
            ui.label(
                RichText::new("Choose a message from the list.")
                    .size(tokens::type_size::BODY)
                    .color(theme.text_secondary),
            );
        });
        return;
    };

    egui::ScrollArea::vertical()
        .auto_shrink([false, false])
        .show(ui, |ui| {
            ui.add_space(tokens::space::LG);
            ui.horizontal(|ui| {
                ui.heading(
                    RichText::new(&msg.subject)
                        .size(tokens::type_size::TITLE_LG)
                        .color(theme.text_primary),
                );
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    let star = if msg.starred { "★ Starred" } else { "☆ Star" };
                    if ui.button(star).clicked() {
                        state.toggle_star_selected();
                    }
                });
            });
            ui.label(
                RichText::new(format!("{} · {}", msg.sender_name, msg.time_label))
                    .size(tokens::type_size::BODY)
                    .color(theme.text_secondary),
            );
            if let Some(domain) = &msg.sender_domain {
                ui.label(
                    RichText::new(format!("via {domain}"))
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                );
            }
            ui.label(
                RichText::new(format!("Received as {}", msg.account_label))
                    .size(tokens::type_size::LABEL)
                    .color(theme.text_secondary),
            );

            if let Some(why) = &msg.priority_explanation {
                ui.add_space(tokens::space::SM);
                ui.label(
                    RichText::new(format!("Why this priority? {why}"))
                        .size(tokens::type_size::LABEL)
                        .color(theme.brand),
                );
            }

            // Conversation intelligence (7 states) — never invents "user forgot".
            if let Some(assess) = bridge.assess_conversation_for(
                &msg.account_id,
                &msg.thread_id,
                &ConversationSignals {
                    last_message_expects_reply: msg.subject.contains('?'),
                    has_sufficient_evidence: true,
                    negative_signal: msg
                        .sender_email
                        .contains("noreply")
                        || msg.category == crate::navigation::NavDestination::Promotional,
                    ..Default::default()
                },
            ) {
                ui.add_space(tokens::space::SM);
                ui.label(
                    RichText::new(format!("Conversation: {}", assess.state.display_label()))
                        .size(tokens::type_size::LABEL_LG)
                        .color(theme.text_primary),
                );
                ui.label(
                    RichText::new(&assess.explanation)
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                );
            }

            let temporal = bridge.extract_temporal_for(
                &msg.account_id,
                &msg.id,
                &msg.subject,
                &msg.snippet,
                0,
            );
            if !temporal.is_empty() {
                ui.add_space(tokens::space::SM);
                for item in &temporal {
                    ui.label(
                        RichText::new(format!(
                            "Temporal · {} — {}",
                            item.item_type.as_str(),
                            item.explanation
                        ))
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                    );
                }
            }

            ui.add_space(tokens::space::LG);
            ui.separator();
            ui.add_space(tokens::space::MD);

            // Sanitize before display — never fetch remote images.
            let sanitized = bridge.sanitize_mail_html(&msg.body_sanitized);
            ui.label(
                RichText::new(&sanitized.text)
                    .size(tokens::type_size::BODY_LG)
                    .color(theme.text_primary),
            );
            if sanitized.remote_images_blocked {
                ui.add_space(tokens::space::SM);
                ui.label(
                    RichText::new("Remote images blocked for privacy.")
                        .size(tokens::type_size::LABEL)
                        .color(theme.text_secondary),
                );
            }
        });
}
