# Redesign — asset & palette conflict

## Supplied background images

Searched `banner/`, `logo/`, `Windows/resources/**`, and parent `mail stack/` for light/dark flowing-shape backgrounds.

**Result:** no dedicated light-mode or dark-mode wallpaper PNGs exist in the repo.

| Asset | Path | Role |
|-------|------|------|
| App logo | `Windows/resources/icons/app_logo.png` (+ `logo/logo.png`) | OS window icon / brand mark |
| Banner | `banner/banner.png` | Marketing banner — not an app wallpaper |

**Decision:** do **not** invent substitute photo art. Android `Color.kt` and `design.md` explicitly forbid photo backgrounds and glassmorphism for Mailstack Expressive. Surfaces stay solid tonal colours from the token set.

## Palette conflict (user brief vs Android SoT)

| Brief claim | Android `Color.kt` (shipped) |
|-------------|------------------------------|
| Primary pastel blue | Primary vivid violet `#5238E8` / dark `#B9A8FF` |
| Secondary white | Surfaces `#FDFBFF` / `#1A1826` (not pure white slabs) |
| Accent mint green | Tertiary `#1E7A55` / dark `#7FE0B2` — **matches** |
| Background flowing shapes | Solid `#F6F3FF` / `#131120` — **no photos** |
| Frosted glass | Explicitly removed (“Breeze glassmorphism is gone”) |

**Decision:** implement **Android-as-shipped** tokens so Windows matches the live Android product. Mint tertiary is preserved. Pastel-blue / photo-bg / glass are deferred until real assets and a product decision to diverge from Android.

## Desktop layout goals (this pass)

1. Compact toolbar — search, refresh, settings, account; **no** large content-header logo.
2. Sidebar — six mailbox destinations with M3 pill selection.
3. Mail list — flat Android-density rows.
4. Conversation — reading pane only; empty state without category dump.
5. Categories/companies — under Promotional/Social list column, not in the empty reader.
6. Central theme tokens + light/dark switch; no per-frame `apply_egui_visuals`.
