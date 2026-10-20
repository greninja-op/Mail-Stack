//! Integration / platform capability queries (phases 22–25 / 27 hooks).
//!
//! Action Engine and UI ask "is this capable?" without duplicating grant logic.

use crate::account::AccountId;
use crate::oauth::{SCOPE_CALENDAR_EVENTS, SCOPE_GMAIL_MODIFY, SCOPE_GMAIL_READONLY, SCOPE_TASKS};
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::sync::Mutex;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum IntegrationCapability {
    GmailRead,
    GmailModify,
    CalendarEvents,
    Tasks,
    Notifications,
    BackgroundSync,
    OptionalAi,
    Automation,
}

impl IntegrationCapability {
    pub fn required_scope(self) -> Option<&'static str> {
        match self {
            Self::GmailRead => Some(SCOPE_GMAIL_READONLY),
            Self::GmailModify => Some(SCOPE_GMAIL_MODIFY),
            Self::CalendarEvents => Some(SCOPE_CALENDAR_EVENTS),
            Self::Tasks => Some(SCOPE_TASKS),
            Self::Notifications | Self::BackgroundSync | Self::OptionalAi | Self::Automation => {
                None
            }
        }
    }
}

#[derive(Clone, Debug, Default)]
pub struct AccountCapabilities {
    pub granted_scopes: HashSet<String>,
    pub notifications_authorized: bool,
    pub background_allowed: bool,
    pub ai_enabled: bool,
    pub automation_enabled: bool,
}

impl AccountCapabilities {
    pub fn is_capable(&self, capability: IntegrationCapability) -> bool {
        match capability {
            IntegrationCapability::Notifications => self.notifications_authorized,
            IntegrationCapability::BackgroundSync => self.background_allowed,
            IntegrationCapability::OptionalAi => self.ai_enabled,
            IntegrationCapability::Automation => self.automation_enabled,
            other => other
                .required_scope()
                .map(|s| self.granted_scopes.contains(s))
                .unwrap_or(false),
        }
    }
}

#[derive(Default)]
pub struct CapabilityRegistry {
    by_account: Mutex<HashMap<String, AccountCapabilities>>,
}

impl CapabilityRegistry {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn set(&self, account: &AccountId, caps: AccountCapabilities) {
        if let Ok(mut g) = self.by_account.lock() {
            g.insert(account.as_str().to_owned(), caps);
        }
    }

    pub fn get(&self, account: &AccountId) -> AccountCapabilities {
        self.by_account
            .lock()
            .ok()
            .and_then(|g| g.get(account.as_str()).cloned())
            .unwrap_or_default()
    }

    pub fn is_capable(&self, account: &AccountId, capability: IntegrationCapability) -> bool {
        self.get(account).is_capable(capability)
    }
}

/// Declared capabilities for a one-sign-in base grant.
pub fn base_sign_in_capabilities() -> Vec<IntegrationCapability> {
    vec![
        IntegrationCapability::GmailRead,
        IntegrationCapability::CalendarEvents,
        IntegrationCapability::Tasks,
    ]
}

// ── Phase 27 adaptive performance (tiers, not device names) ───────────────

/// Adaptive performance tier. Never derived from display Hz / FPS claims.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum PerformanceTier {
    Conserving,
    Balanced,
    Expressive,
}

/// Lightweight capability profile selecting animation complexity, prefetch,
/// image cache, background concurrency, and sync batch sizes.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct PerformanceProfile {
    pub tier: PerformanceTier,
    pub max_prefetch_messages: u32,
    pub image_cache_mb: u32,
    pub background_workers: u32,
    pub sync_batch_size: u32,
    pub allow_expressive_motion: bool,
    pub allow_flying_import_mail: bool,
}

impl PerformanceProfile {
    pub fn for_tier(tier: PerformanceTier) -> Self {
        match tier {
            PerformanceTier::Conserving => Self {
                tier,
                max_prefetch_messages: 20,
                image_cache_mb: 32,
                background_workers: 1,
                sync_batch_size: 25,
                allow_expressive_motion: false,
                allow_flying_import_mail: false,
            },
            PerformanceTier::Balanced => Self {
                tier,
                max_prefetch_messages: 80,
                image_cache_mb: 96,
                background_workers: 2,
                sync_batch_size: 50,
                allow_expressive_motion: true,
                allow_flying_import_mail: true,
            },
            PerformanceTier::Expressive => Self {
                tier,
                max_prefetch_messages: 200,
                image_cache_mb: 256,
                background_workers: 4,
                sync_batch_size: 100,
                allow_expressive_motion: true,
                allow_flying_import_mail: true,
            },
        }
    }

    /// Heuristic from reduced-motion + available parallelism.
    /// Not a continuous hardware benchmark; never claim FPS from refresh rate.
    pub fn detect(reduced_motion: bool, available_parallelism: usize) -> Self {
        if reduced_motion || available_parallelism <= 2 {
            Self::for_tier(PerformanceTier::Conserving)
        } else if available_parallelism <= 8 {
            Self::for_tier(PerformanceTier::Balanced)
        } else {
            Self::for_tier(PerformanceTier::Expressive)
        }
    }

    /// Explicit non-claim: display Hz does not imply animation FPS.
    pub fn fps_claim_from_refresh_hz(_hz: u32) -> Option<u32> {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn base_grant_excludes_modify() {
        let caps = base_sign_in_capabilities();
        assert!(caps.contains(&IntegrationCapability::GmailRead));
        assert!(!caps.contains(&IntegrationCapability::GmailModify));
    }

    #[test]
    fn registry_scope_check() {
        let reg = CapabilityRegistry::new();
        let a = AccountId::new("a").unwrap();
        let mut caps = AccountCapabilities::default();
        caps.granted_scopes
            .insert(SCOPE_GMAIL_READONLY.to_owned());
        caps.background_allowed = true;
        reg.set(&a, caps);
        assert!(reg.is_capable(&a, IntegrationCapability::GmailRead));
        assert!(!reg.is_capable(&a, IntegrationCapability::GmailModify));
        assert!(reg.is_capable(&a, IntegrationCapability::BackgroundSync));
    }

    #[test]
    fn performance_respects_reduced_motion() {
        let p = PerformanceProfile::detect(true, 16);
        assert_eq!(p.tier, PerformanceTier::Conserving);
        assert!(!p.allow_expressive_motion);
    }

    #[test]
    fn never_claims_fps_from_hz() {
        assert!(PerformanceProfile::fps_claim_from_refresh_hz(120).is_none());
        assert!(PerformanceProfile::fps_claim_from_refresh_hz(60).is_none());
    }
}
