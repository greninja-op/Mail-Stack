use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct AccountId(String);

impl AccountId {
    pub fn new(value: impl Into<String>) -> Result<Self, &'static str> {
        let v = value.into();
        if v.trim().is_empty() {
            Err("AccountId must not be blank")
        } else {
            Ok(Self(v))
        }
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }

    pub fn is_demo(&self) -> bool {
        self.0.starts_with("local-") || self.0.starts_with("fixture-")
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AccountSnapshot {
    pub account_id: AccountId,
    pub email: String,
    pub display_name: Option<String>,
    pub is_active: bool,
}
