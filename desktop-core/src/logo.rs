//! Logo pipeline inputs: raw sender email + display name → detected domain.
//!
//! Pipeline order (design.md): bundled logo → app assets → favicon → letter.
//! This module only resolves the **domain input**. Fetching/caching logos is
//! later-phase work. Never fabricate a logo; personal mailbox domains yield
//! `None` (letter avatar).

use serde::{Deserialize, Serialize};

/// Inputs the logo pipeline needs for every mail row.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct LogoPipelineInput {
    pub email: String,
    pub display_name: String,
}

impl LogoPipelineInput {
    pub fn new(email: impl Into<String>, display_name: impl Into<String>) -> Self {
        Self {
            email: email.into(),
            display_name: display_name.into(),
        }
    }

    /// Detects a company sender domain, or `None` for people / malformed.
    pub fn detect_sender_domain(&self) -> Option<SenderDomain> {
        detect_sender_domain(&self.email, &self.display_name)
    }
}

/// Canonical sender domain used by the logo / company pipeline.
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct SenderDomain(String);

impl SenderDomain {
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

/// Free/personal mailbox domains → people, not companies.
const PERSONAL_DOMAINS: &[&str] = &[
    "gmail.com",
    "googlemail.com",
    "yahoo.com",
    "yahoo.co.uk",
    "yahoo.co.in",
    "yahoo.ca",
    "yahoo.com.au",
    "outlook.com",
    "hotmail.com",
    "live.com",
    "msn.com",
    "icloud.com",
    "me.com",
    "mac.com",
    "protonmail.com",
    "proton.me",
    "pm.me",
    "aol.com",
    "zoho.com",
    "gmx.com",
    "mail.com",
    "yandex.com",
];

const MAILING_SUBDOMAINS: &[&str] = &[
    "mail",
    "email",
    "news",
    "newsletter",
    "newsletters",
    "promo",
    "promos",
    "deals",
    "offers",
    "marketing",
    "notify",
    "notification",
    "notifications",
    "alert",
    "alerts",
    "bounce",
    "bounces",
    "mailer",
    "send",
    "smtp",
    "transactional",
];

const MULTI_LABEL_SUFFIXES: &[&str] = &[
    "co.uk", "org.uk", "ac.uk", "gov.uk", "com.au", "net.au", "org.au", "co.jp", "ne.jp",
    "or.jp", "co.in", "net.in", "org.in", "com.br", "net.br", "org.br", "co.za", "co.nz",
    "com.mx", "co.kr",
];

/// Resolves email (+ optional display name) to a company domain.
///
/// Display name is accepted for API symmetry with the Android pipeline; domain
/// detection is driven by the email address (stable identity). Malformed input
/// yields `None`.
pub fn detect_sender_domain(email: &str, _display_name: &str) -> Option<SenderDomain> {
    let domain = domain_of(email)?;
    if PERSONAL_DOMAINS.contains(&domain.as_str()) {
        return None;
    }
    let canonical = canonicalize_domain(&domain);
    if PERSONAL_DOMAINS.contains(&canonical.as_str()) {
        return None;
    }
    if canonical.is_empty() || !canonical.contains('.') {
        return None;
    }
    Some(SenderDomain(canonical))
}

fn domain_of(email: &str) -> Option<String> {
    let trimmed = email.trim().to_ascii_lowercase();
    let at = trimmed.rfind('@')?;
    let domain = trimmed[at + 1..].trim();
    if domain.is_empty() || domain.contains('@') || domain.contains(' ') {
        return None;
    }
    // Strip angle-bracket wrappers / trailing punctuation common in From headers.
    let domain = domain.trim_matches(|c: char| c == '>' || c == '<' || c == ',' || c == ';');
    if domain.is_empty() {
        None
    } else {
        Some(domain.to_owned())
    }
}

fn canonicalize_domain(domain: &str) -> String {
    let mut labels: Vec<&str> = domain.split('.').filter(|l| !l.is_empty()).collect();
    while labels.len() > 2 && MAILING_SUBDOMAINS.contains(&labels[0]) {
        labels.remove(0);
    }
    registrable_domain(&labels)
}

fn registrable_domain(labels: &[&str]) -> String {
    if labels.len() < 2 {
        return labels.join(".");
    }
    let last_two = format!("{}.{}", labels[labels.len() - 2], labels[labels.len() - 1]);
    if MULTI_LABEL_SUFFIXES.contains(&last_two.as_str()) && labels.len() >= 3 {
        return format!(
            "{}.{}.{}",
            labels[labels.len() - 3],
            labels[labels.len() - 2],
            labels[labels.len() - 1]
        );
    }
    last_two
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn company_email_yields_domain() {
        let d = detect_sender_domain("noreply@mail.netflix.com", "Netflix").unwrap();
        assert_eq!(d.as_str(), "netflix.com");
    }

    #[test]
    fn personal_gmail_yields_none() {
        assert!(detect_sender_domain("alice@gmail.com", "Alice").is_none());
    }

    #[test]
    fn mailing_subdomain_stripped() {
        let d = detect_sender_domain("updates@newsletter.spotify.com", "").unwrap();
        assert_eq!(d.as_str(), "spotify.com");
    }

    #[test]
    fn multi_label_suffix_preserved() {
        let d = detect_sender_domain("hello@shop.example.co.uk", "").unwrap();
        assert_eq!(d.as_str(), "example.co.uk");
    }

    #[test]
    fn malformed_email_yields_none() {
        assert!(detect_sender_domain("not-an-email", "").is_none());
        assert!(detect_sender_domain("", "Someone").is_none());
    }

    #[test]
    fn logo_pipeline_input_round_trips_domain() {
        let input = LogoPipelineInput::new("billing@stripe.com", "Stripe");
        assert_eq!(
            input.detect_sender_domain().unwrap().as_str(),
            "stripe.com"
        );
    }
}
