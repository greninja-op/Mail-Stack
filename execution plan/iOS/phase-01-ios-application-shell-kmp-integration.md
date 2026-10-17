# Phase 1 — iOS Application Shell & KMP Integration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase targets **iOS / iPhone only**. All iOS-specific source, configuration, assets, tests, and documents must live under `iOS/`. Never create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/`. Shared KMP changes must remain shared and must be justified by this phase.

## Mission

Create the real iPhone application shell and establish a clean SwiftUI ↔ KMP integration boundary without implementing the complete mailbox yet.

## Scope

- iOS application entry point and scene lifecycle
- SwiftUI navigation shell
- dependency/bootstrap boundary
- KMP framework/XCFramework integration
- environment/configuration separation
- centralized iOS design tokens
- basic loading/error/empty/offline state primitives
- development diagnostics that do not leak secrets
- simulator build and launch

## Requirements

The shell must be capable of hosting later mailbox screens without requiring a rewrite. Shared use cases must be injected through stable interfaces rather than directly coupling SwiftUI views to repositories.

Use iOS-native lifecycle and state handling. Keep platform concerns out of shared business logic unless represented through explicit interfaces.

## Validation

- clean iOS build
- unit tests for any new boundary logic
- Simulator install/launch/terminate/relaunch
- verify orientation and common iPhone sizes
- verify light/dark mode
- verify Dynamic Type basics
- inspect logs for accidental tokens or email content

Do not implement OAuth, Gmail sync, mailbox intelligence, Calendar, Tasks, automation, or AI beyond stubs/contracts required for compilation.

**STOP AFTER PHASE 1.**

## 2026-10-18 — Current-design update

- Shell implements the CURRENT navigation: drawer covering the top section (Gmail parity), uniform 40pt profile circle on every tab, Gmail-style account switcher, Settings panel sliding in from the right (350ms, underlying screen slides 1/12 left), scroll-hide bars (down hides, up shows, 250ms, pure translate), tab switches that never reload the page (content slides from the respective side, no flicker).
- Centralized design tokens from `design.md` (brand violet, dark-mode charcoal `#131120`, typography scale, 28pt card radius, motion tiers). No per-screen magic values.
- Safe areas respected on all iPhone sizes; bars overlaid, never padded/resized containers.
