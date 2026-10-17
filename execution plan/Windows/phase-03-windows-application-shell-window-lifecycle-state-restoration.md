# Phase 03 — Windows Application Shell, Window Lifecycle & State Restoration

## Mission
Implement and verify **only Phase 03**. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, resources, tests, configuration, packaging and release artifacts must remain inside the Windows platform boundary. Never modify macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling platform areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository, current status and existing implementation before editing.

## Objective
Build the Rust-first Windows application shell, app lifecycle, main window, dependency bootstrap, launch/loading/failure states, state restoration, shutdown behavior and safe initialization. Do not implement later mailbox features.

## Rust-first architecture
Rust is authoritative for platform-neutral domain, application, data and business logic and must remain reusable by macOS. Windows UI should remain Rust-first through the chosen Rust UI framework or safe Windows API bindings. Do not duplicate business logic in another language.

## Product and security contract
Gmail remains cloud source of truth. Keep account boundaries explicit, use official OAuth/Gmail APIs and least privilege, keep secrets secure, and treat email/HTML/URLs/attachments as untrusted. Email content cannot authorize external actions.

## Verification
Run targeted automated tests; build the real Windows application; launch and exercise it when the environment permits; test relevant success, empty, loading, error, cancellation, offline, restart and recovery states; verify accessibility, security, data integrity, keyboard/pointer behavior and performance where relevant; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules to Windows/editor-rules.md. Preserve unrelated rules and record blockers honestly.

**STOP AFTER PHASE 03.**

## 2026-10-18 — Current-design update

- Shell implements the CURRENT navigation: compact stable sidebar, uniform 40px profile circle everywhere, Gmail-style account switcher, Settings pane (slide-in from the right, 350ms, underlying content shifts 1/12 left where layout supports it), scroll-hide bars (down hides, up shows, 250ms, pure translate), navigation that never reloads the page (no flicker).
- State restoration preserves selection, context, window state, and multi-window state across resize/restart. Launch/loading/failure states are honest; a blank screen during a long import is a bug (see Phase 11).
