//! Category-group model hooks (single-cavity cards, semantic accents).
//!
//! UI tokens (28px radius, accent colors) live in Windows presentation.
//! This module defines the shared group identity the UI consumes.

use crate::account::AccountId;
use serde::{Deserialize, Serialize};

/// Stable category-group identifier within one account.
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct CategoryGroupId(String);

impl CategoryGroupId {
    pub fn new(raw: impl Into<String>) -> Option<Self> {
        let s = raw.into();
        let t = s.trim();
        if t.is_empty() {
            None
        } else {
            Some(Self(t.to_owned()))
        }
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }
}

/// Semantic group kinds aligned with design.md accents.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum CategoryGroupKind {
    /// Needs attention → amber.
    NeedsAttention,
    /// Promotions → orange.
    Promotions,
    /// Updates → sky blue.
    Updates,
    /// Work & learning → royal blue.
    WorkAndLearning,
    /// Everything else → brand violet.
    EverythingElse,
}

impl CategoryGroupKind {
    /// Token key for Windows theme mapping (not a color value).
    pub fn accent_token(&self) -> &'static str {
        match self {
            Self::NeedsAttention => "accent.needs_attention",
            Self::Promotions => "accent.promotions",
            Self::Updates => "accent.updates",
            Self::WorkAndLearning => "accent.work_learning",
            Self::EverythingElse => "accent.brand_violet",
        }
    }
}

/// Account-scoped category group for single-cavity card presentation.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct CategoryGroupModel {
    pub account_id: AccountId,
    pub id: CategoryGroupId,
    pub kind: CategoryGroupKind,
    pub title: String,
    /// Member message / thread ids (opaque at this contract layer).
    pub member_ids: Vec<String>,
    pub expanded: bool,
}

impl crate::account::AccountScoped for CategoryGroupModel {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::account::AccountScoped;

    #[test]
    fn group_is_account_scoped() {
        let acct = AccountId::new("a1").unwrap();
        let g = CategoryGroupModel {
            account_id: acct.clone(),
            id: CategoryGroupId::new("needs").unwrap(),
            kind: CategoryGroupKind::NeedsAttention,
            title: "Needs attention".into(),
            member_ids: vec!["m1".into()],
            expanded: true,
        };
        assert_eq!(g.account_id(), &acct);
        assert_eq!(g.kind.accent_token(), "accent.needs_attention");
    }
}
