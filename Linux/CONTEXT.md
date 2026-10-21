# Linux

Linux in this repo is backend only. The product UI has not been started.

## Status (2026-10-21)

The UI has not been touched in the 2026-10-21 work.

There is no Linux product UI in the repo. The earlier scaffold was removed on 2026-10-20 because it was bad.

Only the shared Rust backend exists for Linux right now: crate desktop-core (platform-neutral contracts: persistence, oauth, gmail adapter, sync, classify, search, rules, privacy, AI off by default). Linux-specific adapters are being added under Linux/crates by the other four agents. Do not invent their APIs.

The UI still needs to be built. Do not treat any leftover shell as acceptable.

The UI still has to be done later, matching the current Android phone app, because the current and previous UI is really bad. Do not start that UI from this file.

This machine is Windows. Do not claim a Linux desktop launch.

No fake Gmail, OAuth success, or sync percentages.

## What that means

`desktop-core` is the shared crate Linux is allowed to reuse. It is platform-neutral. It is not a Linux application, and it is not a window.

Linux-specific work that is not in `desktop-core` — a real Secret Service store, a real SQLite file, and live HTTP — belongs in adapters under `Linux/crates`. Those adapters are owned by the other agents. This file does not name their types or functions.

The root workspace currently lists `desktop-core` and `Windows` only. This folder does not change the root `Cargo.toml`.

Read `Linux/docs/00-backend-and-ui-status.md` for what `desktop-core` actually exports and what a Linux host still lacks.
