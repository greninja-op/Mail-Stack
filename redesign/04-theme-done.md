# Agent 4 — Theme, backgrounds, motion (Windows)

**Status:** done for this pass  
**Owned files:** `Windows/src/tokens.rs`, `Windows/src/brand.rs`, theme toggles in `Windows/src/ui/settings.rs`  
**Avoided:** shell layout / resizable panes (Agent 3)

---

## Tokens = Android `Color.kt` exactly

`ThemeTokens` in `Windows/src/tokens.rs` mirrors shipped Android Expressive palette:

| Role | Light | Dark |
|------|-------|------|
| background | `#F6F3FF` | `#131120` |
| brand / primary | `#5238E8` | `#B9A8FF` |
| surface / containers / outlines | full Color.kt ladder | full Color.kt ladder |
| semantic status | green / amber / blue / error | tonal dark equivalents |
| category accents | MoCategoryColors | MoCategoryColors |

Theme switch (`Light` / `Dark` / `High contrast`) updates `AppState.theme_mode`. Every screen paints from `state.theme()`. `apply_egui_visuals` runs **only** when `theme.mode` changes (tracked in `MailstackApp.applied_theme`).

---

## Brief conflict: pastel blue / mint / glass

Some product notes asked for pastel blue / mint / glassmorphism.

**Decision:** prefer **Android shipped tokens** (vivid violet + solid tonal surfaces). Android `Color.kt` explicitly retired Breeze glass (no photo background, no translucent cards). Mint remains only as **tertiary** (`#1E7A55` / `#7FE0B2`), not as a glass wash.

Do not reintroduce glass or wallpaper-derived dynamic color on Windows.

---

## Background images — audit

Searched:

| Path | Result |
|------|--------|
| `Windows/resources/` | `icons/app_logo.png` + READMEs only |
| `Windows/resources/assets/` | README placeholder; no PNGs |
| `banner/banner.png` | marketing banner only (not a light/dark app bg) |
| `logo/logo.png` | app mark (same as `app_logo.png`) |
| `*/backgrounds/flowing_*.png` | **MISSING** |

**No dedicated light/dark flowing-shape backgrounds were found.**

Per brief: do **not** invent fake photo art claiming to be user-supplied. Screens use solid Android tones (`theme.bg`, `theme.surface`, optional `theme.tonal_panel()`). `brand::has_flowing_backgrounds()` returns `false` until real assets land under `Windows/resources/backgrounds/`.

---

## Motion

- **Hover / selected:** mail rows (`interaction_fill`), sidebar pills, settings theme chips — instant tonal fills; no layout tween.
- **Settings panel:** fixed `SETTINGS_WIDTH` (360); never animate SidePanel width every frame.
- **Reduced motion:** Settings checkbox → `MotionTier::Reduced` (zero slide / reveal durations; `allows_panel_animation() == false`).
- Scroll-hide of chrome remains disabled (was thrashing TopBottomPanel).

---

## Verify

```text
cargo check -p mailstack-windows
```
