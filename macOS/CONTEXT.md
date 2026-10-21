# macOS

macOS in this repo is backend only. The product UI has not been started.

## Status (2026-10-21)

The UI has not been touched in the 2026-10-21 work.

There is no macOS product UI in the repo. The earlier scaffold was removed on 2026-10-20 because it was bad.

Only the shared Rust backend exists for macOS right now: crate desktop-core (platform-neutral contracts: persistence, oauth, gmail adapter, sync, classify, search, rules, privacy, AI off by default). macOS adapters are being added under macOS/crates by the other agents. Do not invent their APIs.

The plan says Rust is the backend. Swift is only for UI and OS integration. There is no Swift UI in this work.

The UI still needs to be built. Do not treat any leftover shell as acceptable.

The UI still has to be done later, matching the current Android phone app, because the current and previous UI is really bad. Do not start that UI from this file.

This machine is Windows. Do not claim an Xcode or Mac launch.

No fake OAuth, Keychain success, or sync percentages.

## What that means

`desktop-core` is the shared crate macOS is allowed to reuse. It is platform-neutral. It is not a macOS application, and it is not a window.

macOS-specific work that is not in `desktop-core` — a real Keychain store, a real SQLite file, and live HTTP — belongs in adapters under `macOS/crates`. Those adapters are owned by the other agents. This file does not name their types or functions.

Swift would be the later native shell and OS integration layer. It must not become a second copy of the business rules. That shell does not exist in this repo.

The root workspace currently lists `desktop-core` and `Windows` only. This folder does not change the root `Cargo.toml`.

Read `macOS/docs/00-backend-and-ui-status.md` for what `desktop-core` actually exports and what a macOS host still lacks.
