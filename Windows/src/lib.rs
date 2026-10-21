//! Mailstack Windows library — egui interim shell modules.
//!
//! The `Mailstack` binary is a thin GUI entry that calls [`shell::run`].
//! Unit tests live here so `cargo test -p mailstack-windows --lib` works.

mod adapters;
mod app_state;
mod brand;
mod bridge;
mod capability_profile;
mod identity;
mod interaction;
mod navigation;
mod redacting_log;
pub mod shell;
mod tokens;
mod ui;
mod workspace;

pub use identity::{app_identity, AppIdentity};
