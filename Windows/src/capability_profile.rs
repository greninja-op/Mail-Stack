//! Phase 27 — Adaptive performance + accessibility hooks.
//!
//! Thin Windows wrapper over `desktop_core::capability::PerformanceProfile`.
//! Never claim FPS from display refresh rate.

pub use desktop_core::capability::{PerformanceProfile as CapabilityProfile, PerformanceTier as CapabilityTier};

use crate::tokens::{MotionTier, ThemeMode};

/// Narrator / high-contrast / reduced-motion accessibility snapshot.
#[derive(Clone, Debug, PartialEq)]
pub struct AccessibilityHooks {
    pub narrator_expected: bool,
    pub high_contrast: bool,
    pub reduced_motion: bool,
    pub large_text_scale: f32,
}

impl Default for AccessibilityHooks {
    fn default() -> Self {
        Self {
            narrator_expected: false,
            high_contrast: false,
            reduced_motion: false,
            large_text_scale: 1.0,
        }
    }
}

impl AccessibilityHooks {
    /// Re-evaluate from app theme + OS-reported reduced motion (no continuous benchmarking).
    pub fn from_app(theme_mode: ThemeMode, reduced_motion: bool, text_scale: f32) -> Self {
        Self {
            narrator_expected: true, // always expose labels; Narrator may or may not be active
            high_contrast: matches!(theme_mode, ThemeMode::HighContrast),
            reduced_motion,
            large_text_scale: text_scale.clamp(1.0, 2.0),
        }
    }

    pub fn effective_motion(&self, preferred: MotionTier) -> MotionTier {
        if self.reduced_motion {
            MotionTier::Reduced
        } else {
            preferred
        }
    }

    /// Widget label helper for Narrator / screen readers.
    pub fn label(name: &str, role: &str) -> String {
        format!("{name}, {role}")
    }
}

/// Detect performance tier; reduced motion forces Conserving.
pub fn detect_profile(hooks: &AccessibilityHooks, available_parallelism: usize) -> CapabilityProfile {
    CapabilityProfile::detect(hooks.reduced_motion, available_parallelism)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn conserving_disables_expressive() {
        let p = CapabilityProfile::for_tier(CapabilityTier::Conserving);
        assert!(!p.allow_expressive_motion);
    }

    #[test]
    fn detect_respects_reduced_motion() {
        let hooks = AccessibilityHooks {
            reduced_motion: true,
            ..Default::default()
        };
        let p = detect_profile(&hooks, 16);
        assert_eq!(p.tier, CapabilityTier::Conserving);
    }

    #[test]
    fn no_fps_from_hz() {
        assert!(CapabilityProfile::fps_claim_from_refresh_hz(120).is_none());
    }

    #[test]
    fn narrator_label_format() {
        let l = AccessibilityHooks::label("Star", "button");
        assert!(l.contains("Star"));
        assert!(l.contains("button"));
    }
}
