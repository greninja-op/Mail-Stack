//! This crate is the Linux backend boundary. There is no UI here.
//! The Linux UI has not been built and the previous one was removed because it was bad.

pub use desktop_core::*;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn migrations_forbid_tokens_and_ai_defaults_off() {
        let migs = migrations();
        assert!(!migs.is_empty());
        for migration in migs {
            assert!(
                schema_forbids_tokens(migration.sql),
                "migration {} must not store credentials",
                migration.name
            );
        }
        let settings = AiSettings::default();
        assert!(!settings.enabled);
        assert!(settings.allowed_capabilities.is_empty());
    }
}
