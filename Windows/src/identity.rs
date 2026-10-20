//! Application identity for Mailstack Windows.

/// Product identity (user-facing name is always "Mailstack").
#[derive(Clone, Debug)]
pub struct AppIdentity {
    pub display_name: &'static str,
    pub package_family_stub: &'static str,
    /// Semver string; release builds bump per standing rules.
    pub version: &'static str,
    /// Monotonic integer for MSIX / installer comparisons.
    pub version_code: u32,
}

pub fn app_identity() -> AppIdentity {
    AppIdentity {
        display_name: "Mailstack",
        package_family_stub: "Mailstack.Desktop",
        version: env!("CARGO_PKG_VERSION"),
        version_code: 1,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn display_name_is_mailstack() {
        assert_eq!(app_identity().display_name, "Mailstack");
    }
}
