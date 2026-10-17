# Mail Organizer iPadOS — Execution Specification

Authority:
1. iPadOS requirements
2. iPadOS specification
3. iPadOS design
4. iPadOS editor rules
5. verified shared architecture
6. official Apple/Google/Kotlin documentation
7. engineering judgment

Only the first incomplete phase executes.

Every phase:
inspect → implement only this phase → build → automated tests → iPad Simulator/runtime → real iPad where available → viewport/input/accessibility/security QA → fix → rebuild/reinstall/retest → documentation/status → STOP.

Never claim unrun validation.

Shared KMP is authoritative for common business/data behavior. Do not create iPad-only sync, classification, search, rules, Action Engine, automation, AI or persistence engines.

UI may diverge substantially from iPhone and Android. Behavioral consistency is required; visual duplication is not.

The iPad layout must respond to available window size rather than a fixed device resolution. Use meaningful content-driven layout thresholds.

Optimize sustained responsiveness, memory, battery, thermal behavior and rendering. ProMotion/120 Hz does not guarantee 120 FPS.

Release requires real signing, entitlements, production OAuth, accurate privacy declarations, no debug bypasses and a clean archive. Never publish automatically.

## 2026-10-18 addendum — current design authority

The Android app (1.0.39 + in-main fixes) is the reference implementation for the design philosophy. `design.md` (rewritten 2026-10-18) and `editor-rules.md` (rewritten 2026-10-18) carry the current contract; where they disagree with an older phase file, they win. Each phase's "2026-10-18 — Current-design update" section lists the post-Phase-30 requirements for that phase.

