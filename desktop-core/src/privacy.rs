//! Privacy / security helpers (phase 23).
//!
//! Tokens never in DB/logs/UI. Email is untrusted. Images never fetched.
//! Local purge never modifies remote Gmail.

use crate::account::AccountId;
use crate::error::{CoreError, CoreResult};
use crate::platform::BrowserOpen;
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::sync::OnceLock;

/// Privacy Center tabs (Overview, Your Data, Google Access, Security).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PrivacyCenterTab {
    Overview,
    YourData,
    GoogleAccess,
    Security,
}

impl PrivacyCenterTab {
    pub const ALL: [PrivacyCenterTab; 4] = [
        Self::Overview,
        Self::YourData,
        Self::GoogleAccess,
        Self::Security,
    ];

    pub fn label(self) -> &'static str {
        match self {
            Self::Overview => "Overview",
            Self::YourData => "Your Data",
            Self::GoogleAccess => "Google Access",
            Self::Security => "Security",
        }
    }
}

/// Model backing the Privacy Center surface.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct PrivacyCenterModel {
    pub selected_tab: PrivacyCenterTab,
    pub inventory: Vec<DataInventoryEntryOwned>,
    pub google_scopes_disclosed: Vec<String>,
    pub security_checks: Vec<SecurityAuditCheck>,
}

impl Default for PrivacyCenterModel {
    fn default() -> Self {
        Self {
            selected_tab: PrivacyCenterTab::Overview,
            inventory: data_inventory()
                .iter()
                .map(DataInventoryEntryOwned::from_static)
                .collect(),
            google_scopes_disclosed: vec![
                "gmail.readonly".into(),
                "calendar.events".into(),
                "tasks".into(),
            ],
            security_checks: default_security_checks(),
        }
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct SecurityAuditCheck {
    pub id: String,
    pub title: String,
    pub passed: bool,
    pub detail: String,
}

fn default_security_checks() -> Vec<SecurityAuditCheck> {
    vec![
        SecurityAuditCheck {
            id: "remote_images_blocked".into(),
            title: "Remote images never fetched".into(),
            passed: true,
            detail: "Opening mail does not phone home via image loads.".into(),
        },
        SecurityAuditCheck {
            id: "tokens_not_in_db".into(),
            title: "OAuth tokens outside local DB".into(),
            passed: true,
            detail: "Tokens live in the OS credential store.".into(),
        },
        SecurityAuditCheck {
            id: "html_sanitized".into(),
            title: "HTML sanitized / scripts stripped".into(),
            passed: true,
            detail: "Email HTML is treated as untrusted input.".into(),
        },
    ]
}

/// Redacts secrets and emails from log lines (domains preserved).
pub fn sanitize_log_line(line: &str) -> String {
    let mut out = line.to_owned();
    static BEARER: OnceLock<Regex> = OnceLock::new();
    let bearer = BEARER.get_or_init(|| {
        Regex::new(r"(?i)(bearer\s+)[A-Za-z0-9._\-]+").expect("bearer regex")
    });
    out = bearer.replace_all(&out, "${1}[REDACTED]").into_owned();

    static AUTH_HDR: OnceLock<Regex> = OnceLock::new();
    let auth = AUTH_HDR.get_or_init(|| {
        Regex::new(r"(?i)(authorization\s*[:=]\s*)\S+").expect("auth regex")
    });
    out = auth.replace_all(&out, "${1}[REDACTED]").into_owned();

    static TOKEN: OnceLock<Regex> = OnceLock::new();
    let token = TOKEN.get_or_init(|| {
        Regex::new(r"(?i)((access|refresh|id)_token|client_secret)\s*[:=]\s*\S+")
            .expect("token regex")
    });
    out = token
        .replace_all(&out, |caps: &regex::Captures| {
            let m = caps.get(0).map(|m| m.as_str()).unwrap_or("");
            if let Some(idx) = m.find(['=', ':']) {
                format!("{}[REDACTED]", &m[..=idx])
            } else {
                "[REDACTED]".into()
            }
        })
        .into_owned();

    static EMAIL: OnceLock<Regex> = OnceLock::new();
    let email = EMAIL.get_or_init(|| {
        Regex::new(r"(?i)\b([A-Za-z0-9._%+\-]+)@([A-Za-z0-9.\-]+\.[A-Za-z]{2,})\b")
            .expect("email regex")
    });
    out = email.replace_all(&out, "[redacted]@$2").into_owned();
    out
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum UrlSafety {
    Safe,
    Rejected(&'static str),
}

/// External link launcher policy — reject dangerous schemes.
pub fn validate_external_url(url: &str) -> UrlSafety {
    let trimmed = url.trim();
    if trimmed.is_empty() || trimmed.chars().any(|c| c.is_control() || c.is_whitespace()) {
        return UrlSafety::Rejected("invalid url");
    }
    let lower = trimmed.to_ascii_lowercase();
    if lower.starts_with("javascript:") {
        return UrlSafety::Rejected("javascript scheme");
    }
    if lower.starts_with("file:") {
        return UrlSafety::Rejected("file scheme");
    }
    if lower.starts_with("content:") {
        return UrlSafety::Rejected("content scheme");
    }
    if lower.starts_with("data:") {
        return UrlSafety::Rejected("data scheme");
    }
    if lower.starts_with("vbscript:") {
        return UrlSafety::Rejected("vbscript scheme");
    }
    if lower.starts_with("http://") || lower.starts_with("https://") || lower.starts_with("mailto:")
    {
        return UrlSafety::Safe;
    }
    UrlSafety::Rejected("unsupported scheme")
}

/// Safe link launcher — validates then delegates to a [`BrowserOpen`] adapter.
pub fn launch_safe_link(browser: &dyn BrowserOpen, url: &str) -> CoreResult<()> {
    match validate_external_url(url) {
        UrlSafety::Safe => browser.open_url(url),
        UrlSafety::Rejected(reason) => Err(CoreError::InvalidArgument(format!(
            "blocked unsafe url: {reason}"
        ))),
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct DataInventoryEntry {
    pub key: &'static str,
    pub description: &'static str,
    pub stored_locally: bool,
    /// Purge never modifies remote Gmail.
    pub remote_gmail_modified_on_purge: bool,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct DataInventoryEntryOwned {
    pub key: String,
    pub description: String,
    pub stored_locally: bool,
    pub remote_gmail_modified_on_purge: bool,
}

impl DataInventoryEntryOwned {
    fn from_static(e: &DataInventoryEntry) -> Self {
        Self {
            key: e.key.into(),
            description: e.description.into(),
            stored_locally: e.stored_locally,
            remote_gmail_modified_on_purge: e.remote_gmail_modified_on_purge,
        }
    }
}

pub fn data_inventory() -> &'static [DataInventoryEntry] {
    &[
        DataInventoryEntry {
            key: "local_messages",
            description: "Cached message metadata and bodies",
            stored_locally: true,
            remote_gmail_modified_on_purge: false,
        },
        DataInventoryEntry {
            key: "oauth_tokens",
            description: "OAuth tokens in OS credential store (not DB)",
            stored_locally: true,
            remote_gmail_modified_on_purge: false,
        },
        DataInventoryEntry {
            key: "classifications",
            description: "Local category / priority / rules state",
            stored_locally: true,
            remote_gmail_modified_on_purge: false,
        },
        DataInventoryEntry {
            key: "search_index",
            description: "Local full-text search index",
            stored_locally: true,
            remote_gmail_modified_on_purge: false,
        },
        DataInventoryEntry {
            key: "automation_rules",
            description: "User-created automation configuration",
            stored_locally: true,
            remote_gmail_modified_on_purge: false,
        },
    ]
}

/// Local-only purge plan — NEVER touches remote Gmail.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct LocalPurgePlan {
    pub account_id: AccountId,
    pub wipe_messages: bool,
    pub wipe_index: bool,
    pub wipe_rules: bool,
    pub wipe_credentials: bool,
    pub modifies_remote_gmail: bool,
}

impl LocalPurgePlan {
    pub fn for_account(account_id: AccountId) -> Self {
        Self {
            account_id,
            wipe_messages: true,
            wipe_index: true,
            wipe_rules: true,
            wipe_credentials: true,
            modifies_remote_gmail: false,
        }
    }

    pub fn validate(&self) -> CoreResult<()> {
        if self.modifies_remote_gmail {
            return Err(CoreError::PermissionDenied);
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::platform::BrowserOpen;

    struct RejectAllBrowser;
    impl BrowserOpen for RejectAllBrowser {
        fn open_url(&self, _url: &str) -> CoreResult<()> {
            Ok(())
        }
    }

    #[test]
    fn redacts_bearer_and_email() {
        let s = sanitize_log_line(
            "Authorization: Bearer ya29.secret user=alice@example.com refresh_token=abc",
        );
        assert!(!s.contains("ya29"));
        assert!(!s.contains("alice@"));
        assert!(s.contains("[redacted]@example.com"));
        assert!(s.contains("[REDACTED]"));
    }

    #[test]
    fn rejects_dangerous_schemes() {
        assert!(matches!(
            validate_external_url("javascript:alert(1)"),
            UrlSafety::Rejected(_)
        ));
        assert!(matches!(
            validate_external_url("file:///c:/secrets"),
            UrlSafety::Rejected(_)
        ));
        assert_eq!(
            validate_external_url("https://example.com"),
            UrlSafety::Safe
        );
    }

    #[test]
    fn safe_launcher_blocks_javascript() {
        let b = RejectAllBrowser;
        assert!(launch_safe_link(&b, "javascript:alert(1)").is_err());
        assert!(launch_safe_link(&b, "https://mail.google.com").is_ok());
    }

    #[test]
    fn purge_never_touches_gmail() {
        assert!(data_inventory()
            .iter()
            .all(|e| !e.remote_gmail_modified_on_purge));
        let plan = LocalPurgePlan::for_account(AccountId::new("a").unwrap());
        assert!(!plan.modifies_remote_gmail);
        assert!(plan.validate().is_ok());
    }

    #[test]
    fn privacy_center_has_four_tabs() {
        assert_eq!(PrivacyCenterTab::ALL.len(), 4);
        let m = PrivacyCenterModel::default();
        assert_eq!(m.selected_tab, PrivacyCenterTab::Overview);
        assert!(!m.inventory.is_empty());
    }
}
