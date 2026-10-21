//! macOS backend boundary.
//!
//! There is no UI in this crate. The previous macOS UI was removed because it
//! was bad, and the UI has not been rebuilt. Business logic stays in
//! `desktop-core`; this crate only re-exports it.

pub use desktop_core::*;

#[cfg(test)]
mod tests {
    use super::*;

    /// Documented SQL migrations must not introduce credential columns.
    /// `schema_forbids_tokens` strips `--` comments, then rejects
    /// `refresh_token`, `access_token`, `password`, and `client_secret`.
    #[test]
    fn schema_migrations_forbid_credential_tokens() {
        let migrations = migrations();
        assert!(!migrations.is_empty());
        for migration in migrations {
            assert!(
                schema_forbids_tokens(migration.sql),
                "migration {} must not store tokens or secrets",
                migration.name
            );
        }
    }
}
