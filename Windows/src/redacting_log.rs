//! Phase 06 — Logging sanitizer: redact secrets and PII before emit.

use std::sync::OnceLock;

use log::{Level, LevelFilter, Log, Metadata, Record};

static LOGGER: OnceLock<RedactingLogger> = OnceLock::new();

/// Install the redacting logger (idempotent).
pub fn init(max_level: LevelFilter) {
    let logger = LOGGER.get_or_init(RedactingLogger::default);
    let _ = log::set_logger(logger);
    log::set_max_level(max_level);
}

#[derive(Default)]
pub struct RedactingLogger;

impl Log for RedactingLogger {
    fn enabled(&self, metadata: &Metadata) -> bool {
        metadata.level() <= Level::Debug
    }

    fn log(&self, record: &Record) {
        if !self.enabled(record.metadata()) {
            return;
        }
        let msg = redact(&format!("{}", record.args()));
        eprintln!("[{}] {}: {}", record.level(), record.target(), msg);
    }

    fn flush(&self) {}
}

/// Redact bearer tokens, OAuth tokens, Authorization headers, and emails
/// (preserve domain after `@`).
pub fn redact(input: &str) -> String {
    let mut out = input.to_string();
    out = redact_regex_like(&out, "bearer ");
    out = redact_authorization(&out);
    out = redact_emails(&out);
    out = redact_token_keys(&out);
    out
}

fn redact_authorization(s: &str) -> String {
    let lower = s.to_ascii_lowercase();
    if let Some(idx) = lower.find("authorization:") {
        let rest = &s[idx..];
        if let Some(end) = rest.find(|c: char| c == '\n' || c == ',' || c == '}') {
            format!("{}Authorization: [REDACTED]{}", &s[..idx], &rest[end..])
        } else {
            format!("{}Authorization: [REDACTED]", &s[..idx])
        }
    } else {
        s.to_string()
    }
}

fn redact_regex_like(s: &str, prefix: &str) -> String {
    let lower = s.to_ascii_lowercase();
    let mut result = String::new();
    let mut i = 0;
    while let Some(rel) = lower[i..].find(prefix) {
        let abs = i + rel;
        result.push_str(&s[i..abs]);
        result.push_str("bearer [REDACTED]");
        let after = abs + prefix.len();
        let end = s[after..]
            .find(|c: char| c.is_whitespace() || c == '"' || c == '\'' || c == ',')
            .map(|e| after + e)
            .unwrap_or(s.len());
        i = end;
    }
    result.push_str(&s[i..]);
    result
}

fn redact_token_keys(s: &str) -> String {
    let keys = ["access_token", "refresh_token", "id_token", "client_secret"];
    let mut out = s.to_string();
    for key in keys {
        let lower = out.to_ascii_lowercase();
        if let Some(idx) = lower.find(key) {
            // Replace value after = or :
            let bytes = out.as_bytes();
            let mut j = idx + key.len();
            while j < bytes.len() && (bytes[j] == b'=' || bytes[j] == b':' || bytes[j] == b' ') {
                j += 1;
            }
            let start_val = j;
            while j < bytes.len() && !bytes[j].is_ascii_whitespace() && bytes[j] != b',' && bytes[j] != b'"' {
                j += 1;
            }
            if start_val < j {
                out = format!("{}[REDACTED]{}", &out[..start_val], &out[j..]);
            }
        }
    }
    out
}

fn redact_emails(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    let chars: Vec<char> = s.chars().collect();
    let mut i = 0;
    while i < chars.len() {
        if chars[i] == '@' && i > 0 {
            // walk back for local part
            let mut start = i;
            while start > 0 {
                let c = chars[start - 1];
                if c.is_ascii_alphanumeric() || c == '.' || c == '_' || c == '%' || c == '+' || c == '-' {
                    start -= 1;
                } else {
                    break;
                }
            }
            let mut end = i + 1;
            while end < chars.len() {
                let c = chars[end];
                if c.is_ascii_alphanumeric() || c == '.' || c == '-' {
                    end += 1;
                } else {
                    break;
                }
            }
            if start < i && end > i + 1 {
                // push chars before local
                for ch in &chars[out_char_len(&out)..start] {
                    // simpler: rebuild from i tracking
                    let _ = ch;
                }
                // Actually rebuild carefully:
                // We've been pushing char-by-char; adjust approach.
            }
        }
        i += 1;
    }
    // Simpler second pass implementation:
    email_redact_pass(s)
}

fn out_char_len(s: &str) -> usize {
    s.chars().count()
}

fn email_redact_pass(s: &str) -> String {
    let mut result = String::new();
    let mut rest = s;
    while let Some(at) = rest.find('@') {
        let (before, after_at) = rest.split_at(at);
        // find local start
        let local_start = before
            .rfind(|c: char| !(c.is_ascii_alphanumeric() || matches!(c, '.' | '_' | '%' | '+' | '-')))
            .map(|i| i + 1)
            .unwrap_or(0);
        let domain_part = &after_at[1..];
        let domain_end = domain_part
            .find(|c: char| !(c.is_ascii_alphanumeric() || c == '.' || c == '-'))
            .unwrap_or(domain_part.len());
        if local_start < before.len() && domain_end > 0 {
            result.push_str(&before[..local_start]);
            result.push_str("[REDACTED]@");
            result.push_str(&domain_part[..domain_end]);
            rest = &domain_part[domain_end..];
        } else {
            result.push_str(before);
            result.push('@');
            rest = &after_at[1..];
        }
    }
    result.push_str(rest);
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn redacts_email_keeps_domain() {
        let r = redact("user alice@example.com wrote");
        assert!(r.contains("[REDACTED]@example.com"));
        assert!(!r.contains("alice@"));
    }

    #[test]
    fn redacts_bearer() {
        let r = redact("Authorization: Bearer supersecrettoken");
        assert!(r.contains("[REDACTED]"));
        assert!(!r.contains("supersecrettoken"));
    }

    #[test]
    fn redacts_access_token() {
        let r = redact("access_token=abc123xyz");
        assert!(r.contains("[REDACTED]"));
        assert!(!r.contains("abc123xyz"));
    }
}
