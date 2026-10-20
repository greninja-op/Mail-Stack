# macOS — phase execution status

> Generated 2026-10-20. SOURCE = code/docs in platform root. BLOCKED_* = cannot complete verify here (Mac/device/secrets/GUI toolkit).

| Phase | File | Status | Notes |
|------:|------|--------|-------|
| 00 | `phase-00-macos-architecture-shared-core-desktop-audit.md` | SOURCE | Audit + desktop-core |
| 01 | `phase-01-macos-target-toolchain-foundation.md` | SOURCE | Core wiring; GUI pending |
| 02 | `phase-02-shared-kmp-contract-macos-platform-boundary.md` | SOURCE | Core wiring; GUI pending |
| 03 | `phase-03-swiftui-app-shell-scenes-lifecycle.md` | SOURCE | Core wiring; GUI pending |
| 04 | `phase-04-desktop-navigation-multi-column-workspace.md` | SOURCE | Core wiring; GUI pending |
| 05 | `phase-05-macos-visual-system-tokens-theming.md` | SOURCE | Core wiring; GUI pending |
| 06 | `phase-06-kmp-framework-integration-dependency-boundary.md` | SOURCE | Core wiring; GUI pending |
| 07 | `phase-07-local-persistence-state-hydration.md` | SOURCE | Core wiring; GUI pending |
| 08 | `phase-08-secure-storage-keychain-account-state.md` | SOURCE | Core wiring; GUI pending |
| 09 | `phase-09-google-oauth-account-connection.md` | SOURCE_SEAMS | BLOCKED live OAuth verify |
| 10 | `phase-10-gmail-data-access-api-adapter.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 11 | `phase-11-initial-gmail-sync.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 12 | `phase-12-incremental-sync-pagination-recovery.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 13 | `phase-13-mailbox-data-model-message-presentation.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 14 | `phase-14-macos-mail-workspace-conversation-viewer.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 15 | `phase-15-categories-company-intelligence-company-workspace.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 16 | `phase-16-search-local-index.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 17 | `phase-17-priority-action-required-explainability.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 18 | `phase-18-rules-user-corrections.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 19 | `phase-19-temporal-intelligence-conversation-intelligence.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 20 | `phase-20-action-cards-calendar-tasks.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 21 | `phase-21-multi-account-unified-inbox.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 22 | `phase-22-offline-background-refresh-notifications.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 23 | `phase-23-privacy-security-data-protection.md` | SOURCE | Logic in desktop-core + workspace façade; native GUI pending |
| 24 | `phase-24-optional-ai-fallback.md` | PARTIAL | Hardening docs |
| 25 | `phase-25-advanced-automation.md` | PARTIAL | Hardening docs |
| 26 | `phase-26-desktop-interaction-keyboard-menus-multi-window.md` | PARTIAL | Hardening docs |
| 27 | `phase-27-performance-accessibility-reliability-hardening.md` | PARTIAL | Hardening docs |
| 28 | `phase-28-comprehensive-functional-integration-qa.md` | CHECKLIST | BLOCKED packaging/signing |
| 29 | `phase-29-security-adversarial-migration-recovery-qa.md` | CHECKLIST | BLOCKED packaging/signing |
| 30 | `phase-30-production-signing-notarization-final-release-hardening.md` | CHECKLIST | BLOCKED packaging/signing |
