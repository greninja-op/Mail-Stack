//! Phase 10 — Gmail data-access adapter contracts + mock.

use crate::account::AccountId;
use crate::error::{CoreError, CoreResult};
use crate::pacing::{PaceDecision, RequestPacer, TokenBucketPacer};
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct GmailProfile {
    pub email_address: String,
    pub messages_total: u64,
    pub threads_total: u64,
    pub history_id: String,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct GmailMessageRef {
    pub id: String,
    pub thread_id: String,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct GmailMessage {
    pub id: String,
    pub thread_id: String,
    pub subject: String,
    pub snippet: String,
    pub sender_email: String,
    pub sender_display: String,
    pub label_ids: Vec<String>,
    pub internal_date_ms: i64,
    pub unread: bool,
    pub starred: bool,
    pub html_body: Option<String>,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ListPage {
    pub messages: Vec<GmailMessageRef>,
    pub next_page_token: Option<String>,
    pub result_size_estimate: Option<u64>,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum GmailApiErrorKind {
    Throttled { retry_after_secs: u64 },
    DailyQuota,
    InsufficientScopes,
    Auth,
    Offline,
    Other(String),
}

impl From<GmailApiErrorKind> for CoreError {
    fn from(k: GmailApiErrorKind) -> Self {
        match k {
            GmailApiErrorKind::Throttled { retry_after_secs } => {
                CoreError::Throttled { retry_after_secs }
            }
            GmailApiErrorKind::DailyQuota => CoreError::DailyQuota,
            GmailApiErrorKind::InsufficientScopes => CoreError::PermissionDenied,
            GmailApiErrorKind::Auth => CoreError::AuthenticationRequired,
            GmailApiErrorKind::Offline => CoreError::Offline,
            GmailApiErrorKind::Other(s) => CoreError::Internal(s),
        }
    }
}

/// Parse provider error bodies into taxonomy (honest UX later).
pub fn classify_provider_error(status: u16, body: &str) -> GmailApiErrorKind {
    let lower = body.to_ascii_lowercase();
    if status == 429 || lower.contains("ratelimitexceeded") || lower.contains("userratelimit") {
        return GmailApiErrorKind::Throttled {
            retry_after_secs: parse_retry_after(body).unwrap_or(60),
        };
    }
    if lower.contains("dailylimitexceeded") {
        return GmailApiErrorKind::DailyQuota;
    }
    if lower.contains("insufficientpermissions") || lower.contains("insufficientscopes") {
        return GmailApiErrorKind::InsufficientScopes;
    }
    if status == 401 {
        return GmailApiErrorKind::Auth;
    }
    GmailApiErrorKind::Other(format!("http {status}"))
}

fn parse_retry_after(body: &str) -> Option<u64> {
    // Extremely small heuristic for tests.
    for part in body.split(|c: char| !c.is_ascii_digit()) {
        if let Ok(n) = part.parse::<u64>() {
            if (1..3600).contains(&n) {
                return Some(n);
            }
        }
    }
    None
}

pub trait GmailApi: Send + Sync {
    fn get_profile(&self, account: &AccountId) -> CoreResult<GmailProfile>;
    fn list_messages(
        &self,
        account: &AccountId,
        page_token: Option<&str>,
    ) -> CoreResult<ListPage>;
    /// Never silently skip — caller must retry on throttle.
    fn get_message(&self, account: &AccountId, id: &str) -> CoreResult<GmailMessage>;
}

/// Mock Gmail used by sync engine unit tests.
#[derive(Default)]
pub struct MockGmail {
    pub profile: Option<GmailProfile>,
    pub pages: Vec<ListPage>,
    pub messages: HashMapLite,
    pub throttle_gets: u32,
    pub gets_attempted: std::sync::atomic::AtomicU32,
}

#[derive(Default)]
pub struct HashMapLite {
    pub map: std::sync::Mutex<std::collections::HashMap<String, GmailMessage>>,
}

impl MockGmail {
    pub fn with_fixture(total: u64, msgs: Vec<GmailMessage>) -> Self {
        let mut pages = Vec::new();
        let refs: Vec<_> = msgs
            .iter()
            .map(|m| GmailMessageRef {
                id: m.id.clone(),
                thread_id: m.thread_id.clone(),
            })
            .collect();
        pages.push(ListPage {
            messages: refs,
            next_page_token: None,
            result_size_estimate: Some(total),
        });
        let store = HashMapLite::default();
        {
            let mut g = store.map.lock().unwrap();
            for m in msgs {
                g.insert(m.id.clone(), m);
            }
        }
        Self {
            profile: Some(GmailProfile {
                email_address: "user@gmail.com".into(),
                messages_total: total,
                threads_total: total,
                history_id: "1".into(),
            }),
            pages,
            messages: store,
            throttle_gets: 0,
            gets_attempted: std::sync::atomic::AtomicU32::new(0),
        }
    }
}

impl GmailApi for MockGmail {
    fn get_profile(&self, _account: &AccountId) -> CoreResult<GmailProfile> {
        self.profile
            .clone()
            .ok_or_else(|| CoreError::NotFound("profile".into()))
    }

    fn list_messages(
        &self,
        _account: &AccountId,
        _page_token: Option<&str>,
    ) -> CoreResult<ListPage> {
        self.pages
            .first()
            .cloned()
            .ok_or_else(|| CoreError::NotFound("page".into()))
    }

    fn get_message(&self, _account: &AccountId, id: &str) -> CoreResult<GmailMessage> {
        let n = self
            .gets_attempted
            .fetch_add(1, std::sync::atomic::Ordering::SeqCst);
        if n < self.throttle_gets {
            return Err(CoreError::Throttled {
                retry_after_secs: 1,
            });
        }
        let g = self
            .messages
            .map
            .lock()
            .map_err(|_| CoreError::Internal("lock".into()))?;
        g.get(id)
            .cloned()
            .ok_or_else(|| CoreError::NotFound(id.into()))
    }
}

/// Fetch a message with pacing — retries throttles, never skips.
pub fn get_message_never_skip<A: GmailApi, P: RequestPacer>(
    api: &A,
    pacer: &mut P,
    account: &AccountId,
    id: &str,
    max_retries: u32,
) -> CoreResult<GmailMessage> {
    let mut attempts = 0;
    loop {
        match pacer.before_request() {
            PaceDecision::Wait(d) => {
                // Cooperative: real runtime sleeps; tests simulate elapsed.
                pacer.simulate_elapsed(d);
                continue;
            }
            PaceDecision::Proceed => {}
        }
        match api.get_message(account, id) {
            Ok(m) => return Ok(m),
            Err(CoreError::Throttled { retry_after_secs }) => {
                attempts += 1;
                if attempts > max_retries {
                    return Err(CoreError::Throttled { retry_after_secs });
                }
                let wait = pacer.honor_retry_after(retry_after_secs);
                if let PaceDecision::Wait(d) = wait {
                    pacer.simulate_elapsed(d);
                }
                // Do not skip — loop retries same id.
            }
            Err(e) => return Err(e),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn classify_throttle() {
        let k = classify_provider_error(429, "rateLimitExceeded retry 30");
        assert!(matches!(k, GmailApiErrorKind::Throttled { .. }));
    }

    #[test]
    fn never_skip_retries_throttled_get() {
        let account = AccountId::new("a").unwrap();
        let msg = GmailMessage {
            id: "m1".into(),
            thread_id: "t".into(),
            subject: "s".into(),
            snippet: "x".into(),
            sender_email: "a@co.com".into(),
            sender_display: "A".into(),
            label_ids: vec!["INBOX".into()],
            internal_date_ms: 1,
            unread: true,
            starred: false,
            html_body: None,
        };
        let mut api = MockGmail::with_fixture(1, vec![msg]);
        api.throttle_gets = 2;
        let mut pacer = TokenBucketPacer::new(100.0);
        let got = get_message_never_skip(&api, &mut pacer, &account, "m1", 5).unwrap();
        assert_eq!(got.id, "m1");
        assert!(api.gets_attempted.load(std::sync::atomic::Ordering::SeqCst) >= 3);
    }
}
