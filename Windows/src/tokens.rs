//! Phase 05 — Fluent-inspired visual tokens (design.md).
//!
//! Centralized colors, spacing, typography, shape, and motion.
//! Dark mode uses charcoal `#131120`, never pure black.

#![allow(dead_code)]

use egui::{Color32, Rounding, Stroke, Vec2};

/// Brand violet — light theme primary.
pub const BRAND_LIGHT: Color32 = Color32::from_rgb(0x52, 0x38, 0xE8);
/// Brand violet — dark theme primary.
pub const BRAND_DARK: Color32 = Color32::from_rgb(0xB9, 0xA8, 0xFF);

/// Deep violet-tinted charcoal background (dark).
pub const BG_DARK: Color32 = Color32::from_rgb(0x13, 0x11, 0x20);
/// Surface one step lighter than background (dark).
pub const SURFACE_DARK: Color32 = Color32::from_rgb(0x1C, 0x19, 0x2E);
/// Elevated / card surface (dark).
pub const SURFACE_ELEVATED_DARK: Color32 = Color32::from_rgb(0x26, 0x22, 0x3A);

/// Light theme background.
pub const BG_LIGHT: Color32 = Color32::from_rgb(0xF8, 0xF7, 0xFC);
pub const SURFACE_LIGHT: Color32 = Color32::from_rgb(0xFF, 0xFF, 0xFF);
pub const SURFACE_ELEVATED_LIGHT: Color32 = Color32::from_rgb(0xF0, 0xEE, 0xF8);

pub const TEXT_PRIMARY_DARK: Color32 = Color32::from_rgb(0xF5, 0xF3, 0xFA);
pub const TEXT_SECONDARY_DARK: Color32 = Color32::from_rgb(0xA8, 0xA0, 0xB8);
pub const TEXT_PRIMARY_LIGHT: Color32 = Color32::from_rgb(0x1A, 0x16, 0x2B);
pub const TEXT_SECONDARY_LIGHT: Color32 = Color32::from_rgb(0x5C, 0x56, 0x70);

/// Semantic category accents (design.md).
pub const ACCENT_NEEDS_ATTENTION: Color32 = Color32::from_rgb(0xF5, 0xA6, 0x23);
pub const ACCENT_PROMOTIONS: Color32 = Color32::from_rgb(0xF0, 0x7D, 0x26);
pub const ACCENT_UPDATES: Color32 = Color32::from_rgb(0x38, 0xB0, 0xDE);
pub const ACCENT_WORK_LEARNING: Color32 = Color32::from_rgb(0x3B, 0x5B, 0xDB);
pub const ACCENT_EVERYTHING_ELSE: Color32 = BRAND_LIGHT;

/// Card corner radius (28px).
pub const CARD_RADIUS: f32 = 28.0;
/// Mail-row avatar / profile circle.
pub const AVATAR_SIZE: f32 = 40.0;
/// Minimum mail-row height.
pub const MAIL_ROW_MIN_HEIGHT: f32 = 88.0;
/// Sidebar width (compact, stable).
pub const SIDEBAR_WIDTH: f32 = 220.0;
/// Settings slide-in width.
pub const SETTINGS_WIDTH: f32 = 360.0;
/// Settings slide duration (ms) — design.md.
pub const SETTINGS_SLIDE_MS: f32 = 350.0;
/// Scroll-hide bar duration (ms).
pub const SCROLL_HIDE_MS: f32 = 250.0;

/// Spacing scale.
pub mod space {
    pub const XS: f32 = 4.0;
    pub const SM: f32 = 8.0;
    pub const MD: f32 = 12.0;
    pub const LG: f32 = 16.0;
    pub const XL: f32 = 24.0;
}

/// Typography sizes (sp → px 1:1 per design.md).
pub mod type_size {
    pub const DISPLAY: f32 = 32.0;
    pub const TITLE_LG: f32 = 22.0;
    pub const TITLE_MD: f32 = 17.0;
    pub const TITLE_SM: f32 = 16.0;
    pub const BODY: f32 = 14.0;
    pub const BODY_LG: f32 = 16.0;
    pub const LABEL: f32 = 12.0;
    pub const LABEL_LG: f32 = 14.0;
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ThemeMode {
    Light,
    Dark,
    HighContrast,
}

#[derive(Clone, Copy, Debug)]
pub struct ThemeTokens {
    pub mode: ThemeMode,
    pub bg: Color32,
    pub surface: Color32,
    pub surface_elevated: Color32,
    pub brand: Color32,
    pub text_primary: Color32,
    pub text_secondary: Color32,
    pub row_hover: Color32,
    pub row_selected: Color32,
    pub divider: Color32,
    pub focus_ring: Color32,
}

impl ThemeTokens {
    pub fn dark() -> Self {
        Self {
            mode: ThemeMode::Dark,
            bg: BG_DARK,
            surface: SURFACE_DARK,
            surface_elevated: SURFACE_ELEVATED_DARK,
            brand: BRAND_DARK,
            text_primary: TEXT_PRIMARY_DARK,
            text_secondary: TEXT_SECONDARY_DARK,
            row_hover: Color32::from_rgba_unmultiplied(0xB9, 0xA8, 0xFF, 28),
            row_selected: Color32::from_rgba_unmultiplied(0xB9, 0xA8, 0xFF, 48),
            divider: Color32::from_rgba_unmultiplied(0xFF, 0xFF, 0xFF, 18),
            focus_ring: BRAND_DARK,
        }
    }

    pub fn light() -> Self {
        Self {
            mode: ThemeMode::Light,
            bg: BG_LIGHT,
            surface: SURFACE_LIGHT,
            surface_elevated: SURFACE_ELEVATED_LIGHT,
            brand: BRAND_LIGHT,
            text_primary: TEXT_PRIMARY_LIGHT,
            text_secondary: TEXT_SECONDARY_LIGHT,
            row_hover: Color32::from_rgba_unmultiplied(0x52, 0x38, 0xE8, 20),
            row_selected: Color32::from_rgba_unmultiplied(0x52, 0x38, 0xE8, 36),
            divider: Color32::from_rgba_unmultiplied(0x00, 0x00, 0x00, 18),
            focus_ring: BRAND_LIGHT,
        }
    }

    pub fn high_contrast() -> Self {
        let mut t = Self::dark();
        t.mode = ThemeMode::HighContrast;
        t.text_primary = Color32::WHITE;
        t.text_secondary = Color32::from_rgb(0xE0, 0xE0, 0xE0);
        t.brand = Color32::from_rgb(0xD0, 0xC0, 0xFF);
        t.focus_ring = Color32::YELLOW;
        t
    }

    pub fn card_rounding() -> Rounding {
        Rounding::same(CARD_RADIUS)
    }

    pub fn hairline(&self) -> Stroke {
        Stroke::new(1.0_f32, self.divider)
    }
}

/// Motion tiers (design.md).
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum MotionTier {
    Essential,
    Standard,
    Expressive,
    Reduced,
}

impl MotionTier {
    pub fn settings_slide_secs(self) -> f32 {
        match self {
            Self::Reduced => 0.0,
            _ => SETTINGS_SLIDE_MS / 1000.0,
        }
    }

    pub fn scroll_hide_secs(self) -> f32 {
        match self {
            Self::Reduced => 0.0,
            _ => SCROLL_HIDE_MS / 1000.0,
        }
    }

    pub fn message_reveal_secs(self) -> f32 {
        match self {
            Self::Reduced => 0.0,
            Self::Essential => 0.15,
            _ => 0.35,
        }
    }
}

/// Apply tokens to egui Visuals.
pub fn apply_egui_visuals(ctx: &egui::Context, theme: &ThemeTokens) {
    let mut visuals = if matches!(theme.mode, ThemeMode::Light) {
        egui::Visuals::light()
    } else {
        egui::Visuals::dark()
    };
    visuals.panel_fill = theme.surface;
    visuals.window_fill = theme.bg;
    visuals.override_text_color = Some(theme.text_primary);
    visuals.selection.bg_fill = theme.row_selected;
    visuals.widgets.noninteractive.bg_fill = theme.surface;
    visuals.widgets.inactive.bg_fill = theme.surface_elevated;
    visuals.widgets.hovered.bg_fill = theme.row_hover;
    visuals.widgets.active.bg_fill = theme.row_selected;
    visuals.widgets.open.bg_fill = theme.surface_elevated;
    visuals.extreme_bg_color = theme.bg;
    visuals.faint_bg_color = theme.surface_elevated;
    ctx.set_visuals(visuals);
    let mut style = (*ctx.style()).clone();
    style.spacing.item_spacing = Vec2::new(space::SM, space::SM);
    style.spacing.window_margin = egui::Margin::same(space::LG);
    ctx.set_style(style);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn dark_bg_is_charcoal_not_black() {
        let t = ThemeTokens::dark();
        assert_ne!(t.bg, Color32::BLACK);
        assert_eq!(t.bg, BG_DARK);
    }

    #[test]
    fn brand_tokens_match_design() {
        assert_eq!(BRAND_LIGHT, Color32::from_rgb(0x52, 0x38, 0xE8));
        assert_eq!(BRAND_DARK, Color32::from_rgb(0xB9, 0xA8, 0xFF));
    }
}
