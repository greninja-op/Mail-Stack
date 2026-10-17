# Phase 01 — Project Target & Toolchain Foundation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Establish the AndroidTablet target/module and Gradle boundaries without duplicating shared business logic. Configure application identity, source/resource separation, build variants, manifests, dependencies, emulator/device targets and release/debug separation. Verify installation and launch on multiple tablet configurations, configuration changes and process termination. Keep all tablet-specific work under AndroidTablet/.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 01.**

## 2026-10-18 — Current-design update

- Module/Gradle boundaries, application identity, source/resource separation, build variants, manifests, dependencies, emulator/device targets, release/debug separation — same conventions as the phone app.
- Test builds sign with the STABLE debug keystore (SHA-1 `07:31:05:EF:F3:E1:CD:DD:6C:7F:16:C1:A5:B1:83:4D:12:AA:7D:05`) — never change it; the OAuth client is bound to it. Production signing uses a separate release key.
- Every release increments the version — never reuse a version name. No per-fix test APKs; build a test APK only when the user explicitly asks.
- Verify installation and launch on multiple tablet configurations, configuration changes, and process termination.
