//! Visual tokens — Design B pastel blue / mint (Windows desktop).
//!
//! Design B wins for brand colours on Windows (`10-implementation-map.md`).
//! Android violet is documented but not applied as shell primary.
//! Typography / spacing / motion still follow Android Type.kt / Spacing.kt habits.
//!
//! Dark mode canvas `#151A22`, never pure black. Cards = 28px radius.
//! Mail rows stay flat on the background (no card chrome).

#![allow(dead_code)]

use egui::{Color32, Rounding, Stroke, Vec2};

// =============================================================================
// Brand — Design B pastel blue
// =============================================================================

/// Brand primary — light (`#4285F4`).
pub const BRAND_LIGHT: Color32 = Color32::from_rgb(0x42, 0x85, 0xF4);
/// Brand / pastel primary — dark (`#8AB4F8`).
pub const BRAND_DARK: Color32 = Color32::from_rgb(0x8A, 0xB4, 0xF8);

/// Pastel primary accent (`#8AB4F8`) — light secondary role.
pub const PASTEL_PRIMARY: Color32 = Color32::from_rgb(0x8A, 0xB4, 0xF8);

pub const BRAND_PRIMARY_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0xDC, 0xEB, 0xFF);
pub const ON_BRAND_PRIMARY_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0x17, 0x2B, 0x4D);
pub const BRAND_SECONDARY_LIGHT: Color32 = PASTEL_PRIMARY;
pub const BRAND_SECONDARY_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0xE8, 0xF1, 0xFF);
pub const ON_BRAND_SECONDARY_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0x17, 0x2B, 0x4D);
/// Mint accent (`#34C759`) — tertiary role.
pub const TERTIARY_LIGHT: Color32 = Color32::from_rgb(0x34, 0xC7, 0x59);
pub const BRAND_TERTIARY_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0xA8, 0xE6, 0xCF);
pub const ON_BRAND_TERTIARY_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0x00, 0x3D, 0x1F);

pub const ON_BRAND_PRIMARY_DARK: Color32 = Color32::from_rgb(0x15, 0x1A, 0x22);
pub const BRAND_PRIMARY_CONTAINER_DARK: Color32 = Color32::from_rgb(0x2B, 0x40, 0x5A);
pub const BRAND_SECONDARY_DARK: Color32 = Color32::from_rgb(0xA7, 0xC8, 0xF5);
pub const ON_BRAND_SECONDARY_DARK: Color32 = Color32::from_rgb(0x15, 0x1A, 0x22);
pub const BRAND_SECONDARY_CONTAINER_DARK: Color32 = Color32::from_rgb(0x34, 0x41, 0x50);
/// Mint accent — dark (`#83DCC0`).
pub const TERTIARY_DARK: Color32 = Color32::from_rgb(0x83, 0xDC, 0xC0);
pub const ON_BRAND_TERTIARY_DARK: Color32 = Color32::from_rgb(0x00, 0x3D, 0x2E);
pub const BRAND_TERTIARY_CONTAINER_DARK: Color32 = Color32::from_rgb(0x1A, 0x4A, 0x3C);

// Aliases used by older call sites / shell.
pub const PRIMARY_CONTAINER_LIGHT: Color32 = BRAND_PRIMARY_CONTAINER_LIGHT;
pub const PRIMARY_CONTAINER_DARK: Color32 = BRAND_PRIMARY_CONTAINER_DARK;

// =============================================================================
// Light surfaces — Design B
// =============================================================================

pub const BG_LIGHT: Color32 = Color32::from_rgb(0xF7, 0xF9, 0xFC);
pub const SURFACE_LIGHT: Color32 = Color32::from_rgb(0xFF, 0xFF, 0xFF);
pub const TEXT_PRIMARY_LIGHT: Color32 = Color32::from_rgb(0x17, 0x2B, 0x4D);
pub const SURFACE_VARIANT_LIGHT: Color32 = Color32::from_rgb(0xE8, 0xF1, 0xFF);
pub const TEXT_SECONDARY_LIGHT: Color32 = Color32::from_rgb(0x64, 0x74, 0x8B);
pub const SURFACE_CONTAINER_LOWEST_LIGHT: Color32 = Color32::WHITE;
pub const SURFACE_CONTAINER_LOW_LIGHT: Color32 = Color32::from_rgb(0xF0, 0xF6, 0xFF);
pub const SURFACE_ELEVATED_LIGHT: Color32 = Color32::from_rgb(0xFF, 0xFF, 0xFF);
pub const SURFACE_CONTAINER_HIGH_LIGHT: Color32 = Color32::from_rgb(0xE3, 0xEA, 0xF2);
pub const SURFACE_CONTAINER_HIGHEST_LIGHT: Color32 = Color32::from_rgb(0xDC, 0xEB, 0xFF);
pub const OUTLINE_LIGHT: Color32 = Color32::from_rgb(0x64, 0x74, 0x8B);
pub const OUTLINE_VARIANT_LIGHT: Color32 = Color32::from_rgb(0xE3, 0xEA, 0xF2);
pub const ROW_HOVER_LIGHT: Color32 = Color32::from_rgb(0xF0, 0xF6, 0xFF);
pub const ROW_SELECTED_LIGHT: Color32 = Color32::from_rgb(0xDC, 0xEB, 0xFF);
pub const DIVIDER_LIGHT: Color32 = Color32::from_rgb(0xE3, 0xEA, 0xF2);

// =============================================================================
// Dark surfaces — Design B (never pure black for app background)
// =============================================================================

pub const BG_DARK: Color32 = Color32::from_rgb(0x15, 0x1A, 0x22);
pub const SURFACE_DARK: Color32 = Color32::from_rgb(0x1D, 0x26, 0x32);
pub const TEXT_PRIMARY_DARK: Color32 = Color32::from_rgb(0xF1, 0xF5, 0xFA);
pub const SURFACE_VARIANT_DARK: Color32 = Color32::from_rgb(0x2B, 0x40, 0x5A);
pub const TEXT_SECONDARY_DARK: Color32 = Color32::from_rgb(0xA7, 0xB4, 0xC5);
pub const SURFACE_CONTAINER_LOWEST_DARK: Color32 = Color32::BLACK;
pub const SURFACE_CONTAINER_LOW_DARK: Color32 = Color32::from_rgb(0x1D, 0x26, 0x32);
pub const SURFACE_ELEVATED_DARK: Color32 = Color32::from_rgb(0x24, 0x2F, 0x3D);
pub const SURFACE_CONTAINER_HIGH_DARK: Color32 = Color32::from_rgb(0x2B, 0x40, 0x5A);
pub const SURFACE_CONTAINER_HIGHEST_DARK: Color32 = Color32::from_rgb(0x34, 0x41, 0x50);
pub const OUTLINE_DARK: Color32 = Color32::from_rgb(0xA7, 0xB4, 0xC5);
pub const OUTLINE_VARIANT_DARK: Color32 = Color32::from_rgb(0x34, 0x41, 0x50);
pub const ROW_HOVER_DARK: Color32 = Color32::from_rgb(0x2B, 0x40, 0x5A);
pub const ROW_SELECTED_DARK: Color32 = Color32::from_rgb(0x2B, 0x40, 0x5A);
pub const DIVIDER_DARK: Color32 = Color32::from_rgb(0x34, 0x41, 0x50);

// =============================================================================
// Errors — semantic (kept readable on Design B surfaces)
// =============================================================================

pub const ERROR_LIGHT: Color32 = Color32::from_rgb(0xB3, 0x26, 0x1E);
pub const ERROR_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0xF9, 0xDE, 0xDC);
pub const ON_ERROR_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0x41, 0x0E, 0x0B);
pub const ERROR_DARK: Color32 = Color32::from_rgb(0xFF, 0xB4, 0xAB);
pub const ON_ERROR_DARK: Color32 = Color32::from_rgb(0x69, 0x00, 0x05);
pub const ERROR_CONTAINER_DARK: Color32 = Color32::from_rgb(0x93, 0x00, 0x0A);
pub const ON_ERROR_CONTAINER_DARK: Color32 = Color32::from_rgb(0xFF, 0xDA, 0xD6);

// =============================================================================
// Status — success / warning / info
// =============================================================================

pub const SUCCESS_LIGHT: Color32 = Color32::from_rgb(0x34, 0xC7, 0x59);
pub const WARNING_LIGHT: Color32 = Color32::from_rgb(0xB4, 0x53, 0x09);
pub const INFO_LIGHT: Color32 = Color32::from_rgb(0x42, 0x85, 0xF4);
pub const SUCCESS_DARK: Color32 = Color32::from_rgb(0x83, 0xDC, 0xC0);
pub const WARNING_DARK: Color32 = Color32::from_rgb(0xFB, 0xBF, 0x24);
pub const INFO_DARK: Color32 = Color32::from_rgb(0x8A, 0xB4, 0xF8);

// Warning badge
pub const WARNING_BADGE_CONTAINER_LIGHT: Color32 = Color32::from_rgb(0xFF, 0xEF, 0xC4);
pub const WARNING_BADGE_CONTENT_LIGHT: Color32 = Color32::from_rgb(0x7A, 0x4A, 0x00);
pub const WARNING_BADGE_CONTAINER_DARK: Color32 = Color32::from_rgb(0x4A, 0x32, 0x00);
pub const WARNING_BADGE_CONTENT_DARK: Color32 = Color32::from_rgb(0xFF, 0xD8, 0x8A);

// =============================================================================
// Category accents — readable on Design B (colour never sole indicator)
// =============================================================================

pub const CAT_ACTION_REQUIRED_LIGHT: Color32 = ERROR_LIGHT;
pub const CAT_ACTION_REQUIRED_DARK: Color32 = ERROR_DARK;
pub const CAT_IMPORTANT_LIGHT: Color32 = BRAND_LIGHT;
pub const CAT_IMPORTANT_DARK: Color32 = BRAND_DARK;
pub const CAT_CAREER_LIGHT: Color32 = Color32::from_rgb(0x1D, 0x4E, 0xD8);
pub const CAT_CAREER_DARK: Color32 = Color32::from_rgb(0x60, 0xA5, 0xFA);
pub const CAT_EDUCATION_LIGHT: Color32 = Color32::from_rgb(0x7C, 0x3A, 0xED);
pub const CAT_EDUCATION_DARK: Color32 = Color32::from_rgb(0xC4, 0xB5, 0xFD);
pub const CAT_RECEIPTS_LIGHT: Color32 = Color32::from_rgb(0x34, 0xC7, 0x59);
pub const CAT_RECEIPTS_DARK: Color32 = Color32::from_rgb(0x83, 0xDC, 0xC0);
pub const CAT_SECURITY_LIGHT: Color32 = Color32::from_rgb(0xB4, 0x53, 0x09);
pub const CAT_SECURITY_DARK: Color32 = Color32::from_rgb(0xFB, 0xBF, 0x24);
pub const CAT_NOTIFICATIONS_LIGHT: Color32 = Color32::from_rgb(0x02, 0x84, 0xC7);
pub const CAT_NOTIFICATIONS_DARK: Color32 = Color32::from_rgb(0x38, 0xBD, 0xF8);
pub const CAT_NEWSLETTERS_LIGHT: Color32 = Color32::from_rgb(0x0D, 0x94, 0x88);
pub const CAT_NEWSLETTERS_DARK: Color32 = Color32::from_rgb(0x5E, 0xEA, 0xD4);
pub const CAT_PROMOTIONS_LIGHT: Color32 = Color32::from_rgb(0xEA, 0x58, 0x0C);
pub const CAT_PROMOTIONS_DARK: Color32 = Color32::from_rgb(0xFB, 0x92, 0x3C);
pub const CAT_LOW_VALUE_LIGHT: Color32 = Color32::from_rgb(0x6B, 0x72, 0x80);
pub const CAT_LOW_VALUE_DARK: Color32 = Color32::from_rgb(0x9C, 0xA3, 0xAF);

// Group header aliases
pub const ACCENT_NEEDS_ATTENTION_LIGHT: Color32 = CAT_SECURITY_LIGHT;
pub const ACCENT_NEEDS_ATTENTION_DARK: Color32 = CAT_SECURITY_DARK;
pub const ACCENT_PROMOTIONS_LIGHT: Color32 = CAT_PROMOTIONS_LIGHT;
pub const ACCENT_PROMOTIONS_DARK: Color32 = CAT_PROMOTIONS_DARK;
pub const ACCENT_UPDATES_LIGHT: Color32 = CAT_NOTIFICATIONS_LIGHT;
pub const ACCENT_UPDATES_DARK: Color32 = CAT_NOTIFICATIONS_DARK;
pub const ACCENT_WORK_LEARNING_LIGHT: Color32 = CAT_CAREER_LIGHT;
pub const ACCENT_WORK_LEARNING_DARK: Color32 = CAT_CAREER_DARK;

/// @deprecated aliases — prefer theme-aware `group_accent`.
pub const ACCENT_NEEDS_ATTENTION: Color32 = ACCENT_NEEDS_ATTENTION_LIGHT;
pub const ACCENT_PROMOTIONS: Color32 = ACCENT_PROMOTIONS_LIGHT;
pub const ACCENT_UPDATES: Color32 = ACCENT_UPDATES_LIGHT;
pub const ACCENT_WORK_LEARNING: Color32 = ACCENT_WORK_LEARNING_LIGHT;
pub const ACCENT_EVERYTHING_ELSE: Color32 = BRAND_LIGHT;

// =============================================================================
// Layout / motion constants (Spacing.kt / design.md)
// =============================================================================

/// Panel corner — Design A cards, tightened for desktop density.
pub const CARD_RADIUS: f32 = 12.0;
/// Pill radius for primary buttons and chips (half of a 40px control).
pub const PILL_RADIUS: f32 = 20.0;
/// Sender avatar on compact mail rows.
pub const AVATAR_SIZE: f32 = 28.0;
/// Receiving-account indicator on unified rows (Android `MoSpacing.xxl`).
pub const ACCOUNT_INDICATOR_SIZE: f32 = 32.0;
/// Category leading accent circle (Android `MoSpacing.xxxl`).
pub const CATEGORY_ICON_SIZE: f32 = 40.0;
pub const MAIL_ROW_MIN_HEIGHT: f32 = 56.0;
/// Horizontal padding inside a mail row (Android `MoSpacing.md` = 16).
pub const MAIL_ROW_PAD_H: f32 = 16.0;
/// Vertical padding inside a mail row (Android `MoSpacing.xs` = 8).
pub const MAIL_ROW_PAD_V: f32 = 8.0;
/// Accent tint on category leading circle — 14% alpha (design.md).
pub const ACCENT_TINT_ALPHA: u8 = 36; // round(255 * 0.14)
pub const SIDEBAR_WIDTH: f32 = 208.0;
/// Fixed settings panel width — never animate this every frame.
pub const SETTINGS_WIDTH: f32 = 360.0;
pub const SETTINGS_SLIDE_MS: f32 = 350.0;
pub const SCROLL_HIDE_MS: f32 = 250.0;

/// Import card hardcoded dark palette (Android ImportScreens — both themes).
pub mod import_palette {
    use egui::Color32;
    pub const APP_BG: Color32 = Color32::from_rgb(0x10, 0x14, 0x18);
    pub const CARD: Color32 = Color32::from_rgb(0x1C, 0x1F, 0x24);
    pub const ON_SURFACE: Color32 = Color32::from_rgb(0xE2, 0xE2, 0xE6);
    pub const ON_SURFACE_BODY: Color32 = Color32::from_rgb(0xC4, 0xC6, 0xD0);
    pub const ON_SURFACE_MUTED: Color32 = Color32::from_rgb(0x8E, 0x90, 0x99);
    pub const OUTLINE: Color32 = Color32::from_rgb(0x3C, 0x40, 0x43);
    pub const RING_TRACK: Color32 = Color32::from_rgb(0x3C, 0x40, 0x43);
    pub const CONNECTED_BLUE: Color32 = Color32::from_rgb(0x8A, 0xB4, 0xF8);
    pub const AVATAR_BLUE: Color32 = Color32::from_rgb(0x1B, 0x5E, 0x8A);
}

/// Accent at 14% alpha for category icon cavities.
pub fn accent_tint(accent: Color32) -> Color32 {
    Color32::from_rgba_unmultiplied(accent.r(), accent.g(), accent.b(), ACCENT_TINT_ALPHA)
}

/// Android `MoSpacing` 4dp grid (audit §4). `MD = 16` matches Android `md`.
pub mod space {
    pub const XXS: f32 = 4.0;
    pub const XS: f32 = 8.0;
    pub const SM: f32 = 12.0;
    pub const MD: f32 = 16.0;
    pub const LG: f32 = 20.0;
    pub const XL: f32 = 24.0;
    pub const XXL: f32 = 32.0;
    pub const XXXL: f32 = 40.0;
    pub const HUGE: f32 = 48.0;
    pub const GIANT: f32 = 64.0;
}

/// Android `Type.kt` sizes used by the egui shell (audit §2).
pub mod type_size {
    pub const DISPLAY: f32 = 32.0;
    pub const TITLE_LG: f32 = 24.0; // titleLarge
    pub const TITLE_MD: f32 = 18.0; // titleMedium — mail sender
    pub const TITLE_SM: f32 = 14.0; // titleSmall
    pub const BODY: f32 = 14.0; // bodyMedium — subject / snippet
    pub const BODY_LG: f32 = 16.0; // bodyLarge
    pub const BODY_SM: f32 = 12.0; // bodySmall
    pub const LABEL: f32 = 12.0; // labelMedium — time
    pub const LABEL_LG: f32 = 14.0; // labelLarge
    pub const LABEL_SM: f32 = 11.0; // labelSmall
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ThemeMode {
    Light,
    Dark,
    HighContrast,
}

/// Semantic status colors.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct StatusColors {
    pub success: Color32,
    pub warning: Color32,
    pub info: Color32,
    pub error: Color32,
}

/// Per-category accents. Color is never the only indicator.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct CategoryAccents {
    pub action_required: Color32,
    pub important: Color32,
    pub career: Color32,
    pub education: Color32,
    pub receipts_orders: Color32,
    pub security: Color32,
    pub notifications: Color32,
    pub newsletters: Color32,
    pub promotions: Color32,
    pub low_value: Color32,
    pub unclassified: Color32,
}

#[derive(Clone, Copy, Debug)]
pub struct ThemeTokens {
    pub mode: ThemeMode,
    // Surfaces
    pub bg: Color32,
    pub surface: Color32,
    pub surface_variant: Color32,
    pub surface_container_lowest: Color32,
    pub surface_container_low: Color32,
    pub surface_elevated: Color32,
    pub surface_container_high: Color32,
    pub surface_container_highest: Color32,
    pub outline: Color32,
    pub outline_variant: Color32,
    // Brand
    pub brand: Color32,
    pub on_brand: Color32,
    pub primary_container: Color32,
    pub on_primary_container: Color32,
    pub secondary: Color32,
    pub secondary_container: Color32,
    pub tertiary: Color32,
    pub tertiary_container: Color32,
    // Text
    pub text_primary: Color32,
    pub text_secondary: Color32,
    // Interaction
    pub row_hover: Color32,
    pub row_selected: Color32,
    pub divider: Color32,
    pub focus_ring: Color32,
    // Semantic
    pub error: Color32,
    pub error_container: Color32,
    pub on_error_container: Color32,
    pub status: StatusColors,
    pub categories: CategoryAccents,
    pub warning_badge_container: Color32,
    pub warning_badge_content: Color32,
}

impl ThemeTokens {
    pub fn light() -> Self {
        Self {
            mode: ThemeMode::Light,
            bg: BG_LIGHT,
            surface: SURFACE_LIGHT,
            surface_variant: SURFACE_VARIANT_LIGHT,
            surface_container_lowest: SURFACE_CONTAINER_LOWEST_LIGHT,
            surface_container_low: SURFACE_CONTAINER_LOW_LIGHT,
            surface_elevated: SURFACE_ELEVATED_LIGHT,
            surface_container_high: SURFACE_CONTAINER_HIGH_LIGHT,
            surface_container_highest: SURFACE_CONTAINER_HIGHEST_LIGHT,
            outline: OUTLINE_LIGHT,
            outline_variant: OUTLINE_VARIANT_LIGHT,
            brand: BRAND_LIGHT,
            on_brand: Color32::WHITE,
            primary_container: BRAND_PRIMARY_CONTAINER_LIGHT,
            on_primary_container: ON_BRAND_PRIMARY_CONTAINER_LIGHT,
            secondary: BRAND_SECONDARY_LIGHT,
            secondary_container: BRAND_SECONDARY_CONTAINER_LIGHT,
            tertiary: TERTIARY_LIGHT,
            tertiary_container: BRAND_TERTIARY_CONTAINER_LIGHT,
            text_primary: TEXT_PRIMARY_LIGHT,
            text_secondary: TEXT_SECONDARY_LIGHT,
            row_hover: ROW_HOVER_LIGHT,
            row_selected: ROW_SELECTED_LIGHT,
            divider: DIVIDER_LIGHT,
            focus_ring: BRAND_LIGHT,
            error: ERROR_LIGHT,
            error_container: ERROR_CONTAINER_LIGHT,
            on_error_container: ON_ERROR_CONTAINER_LIGHT,
            status: StatusColors {
                success: SUCCESS_LIGHT,
                warning: WARNING_LIGHT,
                info: INFO_LIGHT,
                error: ERROR_LIGHT,
            },
            categories: CategoryAccents {
                action_required: CAT_ACTION_REQUIRED_LIGHT,
                important: CAT_IMPORTANT_LIGHT,
                career: CAT_CAREER_LIGHT,
                education: CAT_EDUCATION_LIGHT,
                receipts_orders: CAT_RECEIPTS_LIGHT,
                security: CAT_SECURITY_LIGHT,
                notifications: CAT_NOTIFICATIONS_LIGHT,
                newsletters: CAT_NEWSLETTERS_LIGHT,
                promotions: CAT_PROMOTIONS_LIGHT,
                low_value: CAT_LOW_VALUE_LIGHT,
                unclassified: CAT_LOW_VALUE_LIGHT,
            },
            warning_badge_container: WARNING_BADGE_CONTAINER_LIGHT,
            warning_badge_content: WARNING_BADGE_CONTENT_LIGHT,
        }
    }

    pub fn dark() -> Self {
        Self {
            mode: ThemeMode::Dark,
            bg: BG_DARK,
            surface: SURFACE_DARK,
            surface_variant: SURFACE_VARIANT_DARK,
            surface_container_lowest: SURFACE_CONTAINER_LOWEST_DARK,
            surface_container_low: SURFACE_CONTAINER_LOW_DARK,
            surface_elevated: SURFACE_ELEVATED_DARK,
            surface_container_high: SURFACE_CONTAINER_HIGH_DARK,
            surface_container_highest: SURFACE_CONTAINER_HIGHEST_DARK,
            outline: OUTLINE_DARK,
            outline_variant: OUTLINE_VARIANT_DARK,
            brand: BRAND_DARK,
            on_brand: ON_BRAND_PRIMARY_DARK,
            primary_container: BRAND_PRIMARY_CONTAINER_DARK,
            on_primary_container: Color32::from_rgb(0xDC, 0xEB, 0xFF),
            secondary: BRAND_SECONDARY_DARK,
            secondary_container: BRAND_SECONDARY_CONTAINER_DARK,
            tertiary: TERTIARY_DARK,
            tertiary_container: BRAND_TERTIARY_CONTAINER_DARK,
            text_primary: TEXT_PRIMARY_DARK,
            text_secondary: TEXT_SECONDARY_DARK,
            row_hover: ROW_HOVER_DARK,
            row_selected: ROW_SELECTED_DARK,
            divider: DIVIDER_DARK,
            focus_ring: BRAND_DARK,
            error: ERROR_DARK,
            error_container: ERROR_CONTAINER_DARK,
            on_error_container: ON_ERROR_CONTAINER_DARK,
            status: StatusColors {
                success: SUCCESS_DARK,
                warning: WARNING_DARK,
                info: INFO_DARK,
                error: ERROR_DARK,
            },
            categories: CategoryAccents {
                action_required: CAT_ACTION_REQUIRED_DARK,
                important: CAT_IMPORTANT_DARK,
                career: CAT_CAREER_DARK,
                education: CAT_EDUCATION_DARK,
                receipts_orders: CAT_RECEIPTS_DARK,
                security: CAT_SECURITY_DARK,
                notifications: CAT_NOTIFICATIONS_DARK,
                newsletters: CAT_NEWSLETTERS_DARK,
                promotions: CAT_PROMOTIONS_DARK,
                low_value: CAT_LOW_VALUE_DARK,
                unclassified: CAT_LOW_VALUE_DARK,
            },
            warning_badge_container: WARNING_BADGE_CONTAINER_DARK,
            warning_badge_content: WARNING_BADGE_CONTENT_DARK,
        }
    }

    pub fn high_contrast() -> Self {
        let mut t = Self::dark();
        t.mode = ThemeMode::HighContrast;
        t.text_primary = Color32::WHITE;
        t.text_secondary = Color32::from_rgb(0xE0, 0xE0, 0xE0);
        t.brand = Color32::from_rgb(0xA8, 0xC7, 0xFA);
        t.tertiary = Color32::from_rgb(0xA0, 0xFF, 0xD0);
        t.focus_ring = Color32::YELLOW;
        t.row_hover = SURFACE_CONTAINER_HIGH_DARK;
        t.row_selected = Color32::from_rgba_unmultiplied(0x8A, 0xB4, 0xF8, 64);
        t
    }

    pub fn is_dark(&self) -> bool {
        !matches!(self.mode, ThemeMode::Light)
    }

    /// Solid tonal panel fill — optional subtle hierarchy without photo art.
    pub fn tonal_panel(&self) -> Color32 {
        self.surface_container_low
    }

    /// Category group accent.
    pub fn group_accent(&self, kind: desktop_core::category::CategoryGroupKind) -> Color32 {
        use desktop_core::category::CategoryGroupKind::*;
        match kind {
            NeedsAttention => self.categories.security,
            Promotions => self.categories.promotions,
            Updates => self.categories.notifications,
            WorkAndLearning => self.categories.career,
            EverythingElse => self.categories.important,
        }
    }

    pub fn card_rounding() -> Rounding {
        Rounding::same(CARD_RADIUS)
    }

    pub fn pill_rounding() -> Rounding {
        Rounding::same(PILL_RADIUS)
    }

    pub fn hairline(&self) -> Stroke {
        Stroke::new(1.0_f32, self.divider)
    }

    /// Hover / selected fill for interactive rows. Instant under reduced motion
    /// (no tween — egui paints the target color immediately).
    pub fn interaction_fill(&self, hovered: bool, selected: bool) -> Color32 {
        if selected {
            self.row_selected
        } else if hovered {
            self.row_hover
        } else {
            Color32::TRANSPARENT
        }
    }
}

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

    /// Whether decorative / width animations should run.
    pub fn allows_panel_animation(self) -> bool {
        !matches!(self, Self::Reduced)
    }
}

/// Apply M3-like egui visuals. Call **only** when the theme **mode** changes —
/// calling every frame causes continuous layout thrash / UI shake.
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
    visuals.widgets.noninteractive.fg_stroke = Stroke::new(1.0_f32, theme.text_secondary);
    visuals.widgets.inactive.bg_fill = theme.surface_elevated;
    visuals.widgets.inactive.rounding = ThemeTokens::pill_rounding();
    visuals.widgets.hovered.bg_fill = theme.row_hover;
    visuals.widgets.hovered.rounding = ThemeTokens::pill_rounding();
    visuals.widgets.active.bg_fill = theme.primary_container;
    visuals.widgets.active.rounding = ThemeTokens::pill_rounding();
    visuals.widgets.open.bg_fill = theme.surface_elevated;
    visuals.widgets.open.rounding = ThemeTokens::pill_rounding();
    visuals.extreme_bg_color = theme.bg;
    visuals.faint_bg_color = theme.surface_elevated;
    visuals.window_rounding = ThemeTokens::card_rounding();
    visuals.menu_rounding = ThemeTokens::pill_rounding();
    visuals.hyperlink_color = theme.brand;
    visuals.error_fg_color = theme.error;
    visuals.warn_fg_color = theme.status.warning;
    ctx.set_visuals(visuals);

    let mut style = (*ctx.style()).clone();
    style.spacing.item_spacing = Vec2::new(space::XS, space::XS);
    style.spacing.button_padding = Vec2::new(space::MD, space::XS);
    style.spacing.window_margin = egui::Margin::same(space::MD);
    style.spacing.interact_size = Vec2::new(40.0, 40.0);
    ctx.set_style(style);
}

#[cfg(test)]
mod tests {
    use super::*;
    use desktop_core::category::CategoryGroupKind;

    #[test]
    fn dark_bg_is_design_b_charcoal() {
        let t = ThemeTokens::dark();
        assert_ne!(t.bg, Color32::BLACK);
        assert_eq!(t.bg, Color32::from_rgb(0x15, 0x1A, 0x22));
        assert_eq!(t.surface, Color32::from_rgb(0x1D, 0x26, 0x32));
        assert_eq!(t.surface_elevated, Color32::from_rgb(0x24, 0x2F, 0x3D));
        assert_eq!(t.surface_container_high, SURFACE_CONTAINER_HIGH_DARK);
        assert_eq!(t.surface_container_highest, SURFACE_CONTAINER_HIGHEST_DARK);
    }

    #[test]
    fn brand_tokens_match_design_b() {
        assert_eq!(BRAND_LIGHT, Color32::from_rgb(0x42, 0x85, 0xF4));
        assert_eq!(BRAND_DARK, Color32::from_rgb(0x8A, 0xB4, 0xF8));
        assert_eq!(PASTEL_PRIMARY, Color32::from_rgb(0x8A, 0xB4, 0xF8));
        assert_eq!(
            BRAND_PRIMARY_CONTAINER_LIGHT,
            Color32::from_rgb(0xDC, 0xEB, 0xFF)
        );
        assert_eq!(
            BRAND_PRIMARY_CONTAINER_DARK,
            Color32::from_rgb(0x2B, 0x40, 0x5A)
        );
        assert_eq!(TERTIARY_LIGHT, Color32::from_rgb(0x34, 0xC7, 0x59));
        assert_eq!(TERTIARY_DARK, Color32::from_rgb(0x83, 0xDC, 0xC0));
    }

    #[test]
    fn light_bg_matches_design_b_pastel() {
        assert_eq!(BG_LIGHT, Color32::from_rgb(0xF7, 0xF9, 0xFC));
        let t = ThemeTokens::light();
        assert_eq!(t.bg, BG_LIGHT);
        assert_eq!(t.surface, Color32::WHITE);
        assert_eq!(t.text_primary, Color32::from_rgb(0x17, 0x2B, 0x4D));
        assert_eq!(t.text_secondary, Color32::from_rgb(0x64, 0x74, 0x8B));
        assert_eq!(t.divider, Color32::from_rgb(0xE3, 0xEA, 0xF2));
        assert_eq!(t.row_hover, Color32::from_rgb(0xF0, 0xF6, 0xFF));
        assert_eq!(t.row_selected, Color32::from_rgb(0xDC, 0xEB, 0xFF));
        assert_eq!(t.primary_container, Color32::from_rgb(0xDC, 0xEB, 0xFF));
    }

    #[test]
    fn dark_text_and_divider_match_design_b() {
        let t = ThemeTokens::dark();
        assert_eq!(t.text_primary, Color32::from_rgb(0xF1, 0xF5, 0xFA));
        assert_eq!(t.text_secondary, Color32::from_rgb(0xA7, 0xB4, 0xC5));
        assert_eq!(t.divider, Color32::from_rgb(0x34, 0x41, 0x50));
        assert_eq!(t.brand, Color32::from_rgb(0x8A, 0xB4, 0xF8));
        assert_eq!(t.primary_container, Color32::from_rgb(0x2B, 0x40, 0x5A));
        assert_eq!(t.tertiary, Color32::from_rgb(0x83, 0xDC, 0xC0));
    }

    #[test]
    fn semantic_status_match_design_b() {
        let light = ThemeTokens::light();
        let dark = ThemeTokens::dark();
        assert_eq!(light.status.success, Color32::from_rgb(0x34, 0xC7, 0x59));
        assert_eq!(light.status.warning, Color32::from_rgb(0xB4, 0x53, 0x09));
        assert_eq!(light.status.info, Color32::from_rgb(0x42, 0x85, 0xF4));
        assert_eq!(light.error, ERROR_LIGHT);
        assert_eq!(dark.status.success, Color32::from_rgb(0x83, 0xDC, 0xC0));
        assert_eq!(dark.status.warning, Color32::from_rgb(0xFB, 0xBF, 0x24));
        assert_eq!(dark.status.info, Color32::from_rgb(0x8A, 0xB4, 0xF8));
        assert_eq!(dark.error, ERROR_DARK);
    }

    #[test]
    fn category_accents_use_design_b_brand() {
        let light = ThemeTokens::light().categories;
        let dark = ThemeTokens::dark().categories;
        assert_eq!(light.action_required, ERROR_LIGHT);
        assert_eq!(light.important, BRAND_LIGHT);
        assert_eq!(light.career, Color32::from_rgb(0x1D, 0x4E, 0xD8));
        assert_eq!(light.education, Color32::from_rgb(0x7C, 0x3A, 0xED));
        assert_eq!(light.receipts_orders, Color32::from_rgb(0x34, 0xC7, 0x59));
        assert_eq!(light.security, Color32::from_rgb(0xB4, 0x53, 0x09));
        assert_eq!(light.notifications, Color32::from_rgb(0x02, 0x84, 0xC7));
        assert_eq!(light.newsletters, Color32::from_rgb(0x0D, 0x94, 0x88));
        assert_eq!(light.promotions, Color32::from_rgb(0xEA, 0x58, 0x0C));
        assert_eq!(light.low_value, Color32::from_rgb(0x6B, 0x72, 0x80));
        assert_eq!(dark.important, BRAND_DARK);
        assert_eq!(dark.promotions, Color32::from_rgb(0xFB, 0x92, 0x3C));
        assert_eq!(dark.unclassified, Color32::from_rgb(0x9C, 0xA3, 0xAF));
    }

    #[test]
    fn spacing_md_matches_android_16() {
        assert_eq!(space::MD, 16.0);
        assert_eq!(space::XS, 8.0);
        assert_eq!(space::SM, 12.0);
        assert_eq!(space::LG, 20.0);
    }

    #[test]
    fn category_group_accents_match_audit() {
        let light = ThemeTokens::light();
        let dark = ThemeTokens::dark();
        assert_eq!(
            light.group_accent(CategoryGroupKind::NeedsAttention),
            Color32::from_rgb(0xB4, 0x53, 0x09)
        );
        assert_eq!(
            dark.group_accent(CategoryGroupKind::NeedsAttention),
            Color32::from_rgb(0xFB, 0xBF, 0x24)
        );
        assert_eq!(
            light.group_accent(CategoryGroupKind::Promotions),
            Color32::from_rgb(0xEA, 0x58, 0x0C)
        );
        assert_eq!(
            dark.group_accent(CategoryGroupKind::Updates),
            Color32::from_rgb(0x38, 0xBD, 0xF8)
        );
        assert_eq!(
            light.group_accent(CategoryGroupKind::WorkAndLearning),
            Color32::from_rgb(0x1D, 0x4E, 0xD8)
        );
        assert_eq!(
            dark.group_accent(CategoryGroupKind::EverythingElse),
            BRAND_DARK
        );
    }

    #[test]
    fn mail_sender_is_title_medium_18() {
        assert_eq!(type_size::TITLE_MD, 18.0);
        assert_eq!(type_size::BODY, 14.0);
        assert_eq!(type_size::LABEL, 12.0);
    }

    #[test]
    fn reduced_motion_zeros_durations() {
        assert_eq!(MotionTier::Reduced.settings_slide_secs(), 0.0);
        assert_eq!(MotionTier::Reduced.message_reveal_secs(), 0.0);
        assert!(!MotionTier::Reduced.allows_panel_animation());
    }

    #[test]
    fn not_android_violet() {
        assert_ne!(BRAND_LIGHT, Color32::from_rgb(0x52, 0x38, 0xE8));
        assert_ne!(BG_LIGHT, Color32::from_rgb(0xF6, 0xF3, 0xFF));
        assert_ne!(BG_DARK, Color32::from_rgb(0x13, 0x11, 0x20));
    }
}
