# 05 — Integration & QA report (Agent 5)

**Date:** 2026-10-21  
**Package:** `mailstack-windows` (egui interim shell)  
**MSVC link env:** VS 2017 BuildTools MSVC 14.16 + Windows Kits 10.0.26100 (`LIB` / `INCLUDE` / `link.exe` on `PATH`)

---

## Verdict

**PASS for redesign smoke criteria** (layout + theme + build/test). Product gaps below remain intentional or deferred — not redesign blockers.

**Exe (dev):**  
`C:\Users\Athira Aswin\AppData\Local\Temp\cursor-sandbox-cache\84b8b1492945e5896f3527c372710e04\cargo-target\debug\Mailstack.exe`  
(= `$env:CARGO_TARGET_DIR\debug\Mailstack.exe` when the Cursor sandbox cargo target is active.)

---

## Build / test

| Command | Result |
|---------|--------|
| `cargo check -p mailstack-windows` | Pass (warnings only — unused adapters / demo stubs) |
| `cargo test -p mailstack-windows --lib` | Pass — **42** unit tests |
| `cargo test -p mailstack-windows` | Pass (same suite via bin/lib) |
| `cargo build -p mailstack-windows` | Pass |
| `Start-Process Mailstack.exe` | Launched (non-blocking); process observed running |

### QA fix applied this pass

`cargo test -p mailstack-windows --lib` previously failed with `no library targets found` (bin-only package). Split modules into a lib so `--lib` works:

| File | Change |
|------|--------|
| `Windows/src/lib.rs` | **Added** — hosts all modules; `pub mod shell`; re-exports `app_identity` |
| `Windows/src/main.rs` | Thin GUI entry → `mailstack_windows::shell::run` |
| `Windows/Cargo.toml` | Added `[lib] name = "mailstack_windows"` |

No `android/` edits. No git push.

---

## Files changed summary (redesign + QA)

### Agent 3–4 redesign (pre-existing in tree)

| Area | Files |
|------|--------|
| Shell / chrome / columns | `shell.rs`, `workspace.rs`, `navigation.rs` |
| Tokens / theme | `tokens.rs` |
| Screens | `ui/mail_list.rs`, `ui/conversation.rs`, `ui/categories.rs`, `ui/settings.rs`, `ui/search.rs`, `ui/import_flow.rs`, `ui/privacy_center.rs` |
| State / brand | `app_state.rs`, `brand.rs` |
| Docs | `docs/redesign/00`–`04-*.md`, assets note |

### Agent 5 (this pass)

| File | Why |
|------|-----|
| `Windows/src/lib.rs` | Enable `--lib` tests |
| `Windows/src/main.rs` | Binary entry uses lib |
| `Windows/Cargo.toml` | `[lib]` target |
| `Windows/docs/redesign/05-qa-report.md` | This report |

---

## Smoke checks (visual + code)

| Criterion | Result | Evidence |
|-----------|--------|----------|
| No logo in content toolbar | **Pass** | Top chrome: Search / Refresh / Settings / avatar only; `upload_logo_texture` unused; OS icon via `brand::window_icon` |
| Categories separate sidebar item | **Pass** | Under **Organize**; `WorkspaceSurface::Categories`; not a mailbox dest |
| Conversation empty state — no category dump | **Pass** | “No conversation selected” card only; categories mount only on Categories surface |
| Light / Dark theme in Settings | **Pass** | Appearance chips Light / Dark / High contrast; Light switch applied violet-tint light surfaces immediately |
| Sidebar M3 pills | **Pass** | Selected “All Inbox” uses primary-container pill fill |

Aligned with Android Expressive violet SoT (`#5238E8` / `#B9A8FF`, dark bg `#131120`) — not pastel-blue shells.

---

## Remaining issues (honest)

1. **OAuth / live Gmail** — Still blocked / not wired for real account connect in this shell. Demo accounts + `desktop-core` stubs only; WinCred / browser adapters exist but are unused by UI.
2. **No photo / flowing backgrounds** — Confirmed absent in repo (`00-conflict-and-assets.md`). Solid tonal panels only; matches Android contract (no glass chrome for mail).
3. **Pastel-blue brief deferred** — Product brief pastel-blue primary deferred in favour of **Android violet SoT** (`01-android-design-system.md`). Mint remains tertiary only.
4. **WinUI 3 / MSIX packaging** — Out of scope; egui interim host remains.
5. **Companies / logo pipeline** — Letter avatars + domain stand-ins; bundled company PNGs not yet mirrored under `Windows/resources`.
6. **Dead-code / unused adapter warnings** — Credential, browser, notifications, interaction helpers compile but are largely unused by the redesign shell (expected for later phases).
7. **egui ScrollArea debug warning** — Observed once during smoke (“First/Second use of ScrollArea ID …”) while list + detail were both open. Sources already use distinct `id_salt`s; re-verify on a clean relaunch — not treated as a redesign fail.

---

## Decisions locked (from redesign docs)

- Keep **egui** for this pass (no WinUI rewrite).
- Tokens = Android `Color.kt` as shipped.
- Categories ≠ conversation appendix.
- No inventing wallpaper art.

---

## Suggested next steps (outside Agent 5)

- Wire OAuth + WinCred when product unblocks Google client config.
- Optional: silence unused-adapter warnings or gate behind feature flags.
- Company logo asset copy from Android `drawable-nodpi` when logo pipeline lands.
- Packaging / MSIX when WinAppSDK signing is available.
