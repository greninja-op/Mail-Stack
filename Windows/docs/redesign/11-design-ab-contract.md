# 11 — Design A / B + Android contract (Windows)

> **Agent 1.** Shared contract for the Design A + B Windows redesign.  
> **Doc only** — do not edit `android/`. Colours ship from Design B; type / spacing / motion / semantic *roles* from Android.  
> Companion: `10-implementation-map.md`.

---

## 1. Reference hierarchy

| Priority | Source | Owns on Windows |
|----------|--------|-----------------|
| 1 | **Design A** (Gmail-desktop mock) | Layout & anatomy: 3-pane shell, compose CTA, category tabs, list/reading toolbars, rounded white panels on soft canvas, opened-email presentation |
| 2 | **Design B** (pastel blue / white / mint) | Colour identity & atmosphere: brand blues, mint accents, `#F7F9FC` / `#151A22` canvas, white / navy surfaces, hover / selected tints |
| 3 | **Android** (`Type.kt`, `Spacing.kt`, `Motion.kt`, semantic patterns in `Color.kt`) | Type scale numbers, 4dp spacing + row geometry, radii habits, motion durations/easing, reduced-motion, semantic status *roles* (success / warning / error / info) — **not** Android violet brand hex |
| 4 | **Current Windows egui** | Framework, demo mail, search, settings, privacy, import, desktop-core bridge |

**Policy:** Design B **wins** for shell brand colours. Android Expressive violet (`#5238E8` / `#B9A8FF`) is documented for mobile parity only — **do not** apply it as the Windows primary. Typography, spacing, motion tiers, and semantic role *names* still follow Android.

---

## 2. Design B — exact light / dark hex (user brief)

Source: product redesign brief §4 (pastel-blue / mint / white identity). Map these into `ThemeTokens` field names (`bg`, `surface`, `brand`, `primary_container`, `tertiary`, `row_hover`, `row_selected`, …) — Agents 2–3 must not hard-code hex.

### Light

| Role | Hex | Notes |
|------|-----|-------|
| Main background (canvas) | `#F7F9FC` | Plain cool pastel — not violet-tinted |
| Surface (panels, cards) | `#FFFFFF` | Rounded list / reader panels |
| Primary interaction (brand) | `#4285F4` | Buttons, links, focus, unread time accent |
| Pastel primary | `#8AB4F8` | Softer blue accents, chips, highlights |
| Primary container | `#DCEBFF` | Selected nav pill, subtle fills |
| Selected navigation | `#DCEBFF` | Same as primary container |
| Mint / secondary accent | `#A8E6CF` | Soft mint surfaces / tints |
| Mint strong (optional CTA accent) | `#34C759` | Stronger mint where a vivid accent is needed |
| Primary text | `#172B4D` | Navy body / titles |
| Secondary text | `#64748B` | Snippets, timestamps (read), captions |
| Divider / border | `#E3EAF2` | Hairlines between panels / rows |
| Hover surface | `#F0F6FF` | Row / control hover |
| On primary (text on brand) | `#FFFFFF` | Compose CTA label, filled buttons |

### Dark

| Role | Hex | Notes |
|------|-----|-------|
| Main background | `#151A22` | Deep charcoal / navy canvas |
| Main surface | `#1D2632` | List / reader panel fill |
| Elevated surface | `#242F3D` | Dialogs, overlays, raised chrome |
| Primary blue (brand) | `#8AB4F8` | Interaction blue on dark |
| Primary container | `#2B405A` | Selected nav / container |
| Selected navigation | `#2B405A` | Same as primary container |
| Mint accent | `#83DCC0` | Tertiary / star / success-adjacent accents |
| Primary text | `#F1F5FA` | Near-white |
| Secondary text | `#A7B4C5` | Muted cool grey |
| Divider / border | `#344150` | Hairlines |
| Hover surface | `#293746` | Row / control hover |
| On primary | `#151A22` or `#0D1520` | Text on filled pastel-blue controls (ensure contrast) |

### Semantic roles (keep Android *roles*; hex may track Design B neutrals)

| Role | Intent | Prefer |
|------|--------|--------|
| success | Positive / connected / done | Mint family (`#34C759` / `#A8E6CF` light; `#83DCC0` dark) or Android success green |
| warning | Caution | Amber (Android: `#B45309` / `#FBBF24`) |
| error | Destructive / spam / delete risk | Accessible red (Android: `#B3261E` / `#FFB4AB`) |
| info | Informational | Blue (brief interaction `#4285F4` / pastel `#8AB4F8`) |

Category accent *roles* stay (action required, career, promotions, …); do not re-brand the shell with violet “important” as primary.

---

## 3. Android scales to keep (numbers)

Read from `android/.../ui/theme/Type.kt`, `Spacing.kt`, `Motion.kt`. Windows maps `dp`/`sp` → logical px **1:1** at 100% scale. Preferred face: Inter on Android (system fallback); Segoe UI Variable on Windows.

### 3.1 Type (`Type.kt`)

| Role | Size | Line height | Weight | Letter spacing |
|------|------|-------------|--------|----------------|
| displayLarge | 64 | 72 | Bold | −0.5 |
| displayMedium | 48 | 56 | Bold | 0 |
| displaySmall | 40 | 48 | Bold | 0 |
| headlineLarge | 36 | 44 | Bold | 0 |
| headlineMedium | 32 | 40 | SemiBold | 0 |
| headlineSmall | 28 | 36 | SemiBold | 0 |
| titleLarge | 24 | 30 | SemiBold | 0 |
| titleMedium | 18 | 26 | Medium | 0.15 |
| titleSmall | 14 | 20 | Medium | 0.1 |
| bodyLarge | 16 | 24 | Normal | 0.5 |
| bodyMedium | 14 | 20 | Normal | 0.25 |
| bodySmall | 12 | 16 | Normal | 0.4 |
| labelLarge | 14 | 20 | SemiBold | 0.1 |
| labelMedium | 12 | 16 | Medium | 0.5 |
| labelSmall | 11 | 16 | Medium | 0.5 |

Display / headline = empty states and big headers only. Mail rows and settings stay on body / title / label.

**Mail-row type map:** sender `titleMedium` (18); subject + snippet `bodyMedium` (14); time `labelMedium` (12); meta `labelSmall` (11). Unread → bold sender + subject; unread time uses brand primary.

### 3.2 Spacing (`Spacing.kt`) — strict 4dp grid

| Token | dp / px |
|-------|---------|
| xxs | 4 |
| xs | 8 |
| sm | 12 |
| md | 16 |
| lg | 20 |
| xl | 24 |
| xxl | 32 |
| xxxl | 40 |
| huge | 48 |
| giant | 64 |
| grid | 4 |

| Row geometry | dp / px |
|--------------|---------|
| rowMinHeight | **88** |
| rowAvatar | **40** |
| companyRowMinHeight | 64 |
| messageHeaderMinHeight | 48 |
| searchRowMinHeight | 72 |

### 3.3 Motion (`Motion.kt`) — adapt to egui; same character

| Spec | Duration / easing |
|------|-------------------|
| Fade-through exit | **200ms** FastOutSlowIn |
| Fade-through enter | **350ms** + **120ms** delay, FastOutSlowIn |
| Scale through | 1.0→0.97 out / 0.97→1.0 in (same timings) |
| Tab / category switch | **350ms** FastOutSlowIn, opaque preferred |
| Panel / overlay slide | **350ms** FastOutSlowIn |
| Top-bar reshape | **350ms** FastOutSlowIn |

Respect OS / app reduced-motion: prefer instant or minimal fades; never block mail interaction.

### 3.4 Radii habits (Android `Shapes.kt` / cards — keep proportions)

| Token | Value | Windows use |
|-------|-------|-------------|
| small | 12 | Chips, small controls |
| medium | **16** | Default panel corners (Design A ~16–20) |
| large | 24 | Large surfaces / FAB-like |
| extraLarge | 28 | Expressive cards only |
| pill | full round | Compose CTA, nav selection, search pill |

---

## 4. Design A — layout anatomy

Ship this structure on Design B colours. Do **not** copy Android bottom navigation.

```
┌─ Top toolbar (compact, ~48) ─────────────────────────────────────┐
│  [search pill]     [refresh] [settings] [avatar]                 │
├─ Sidebar ──┬─ Rounded white panel ─┬─ Rounded white panel ───────┤
│  logo+mark │  Category tabs        │  Reading toolbar            │
│  + Compose │  List mini-toolbar    │  Subject + sender header    │
│  Inbox     │  Mail rows (flat)     │  Message body               │
│  Starred   │                       │  Reply / Forward            │
│  Spam      │                       │                             │
│  Categories│                       │                             │
└────────────┴───────────────────────┴─────────────────────────────┘
         soft canvas `#F7F9FC` / `#151A22` behind panels
```

### 4.1 Canvas & panels

- App canvas = Design B `bg` only — plain, no photo / flowing-shape wallpaper behind list or reader.
- Central region = **two** rounded surface panels (list | reader), radius **~16–20**, gap **12–16**, soft hairline (`divider`).
- Collapse: below ~1100px prefer list-only or stacked reader; min window ~960×600.

### 4.2 Top toolbar

- Compact search pill, refresh, settings, account avatar.
- **No** large content-area logo or hero wordmark. Logo = OS icon + optional compact sidebar mark.

### 4.3 Sidebar + Compose CTA

- Compact brand mark + small wordmark.
- Prominent **+ Compose** primary button (brand `#4285F4` / dark `#8AB4F8`) → `OverlayPane::Compose`.
- Destinations that exist: Inbox (All), Starred, Spam, Categories surface. Category **tabs** (not duplicate full mailbox trees) live in the list pane.
- Selected nav = pill on `primary_container` (`#DCEBFF` / `#2B405A`).
- Do **not** invent Snoozed / Sent / Drafts / Trash without real data.

### 4.4 List pane (inbox)

- **Category tabs:** Primary | Promotional | Social (existing filters).
- **Mini-toolbar:** select-all stub, refresh stub, visible-row count (and supported bulk actions only).
- **Rows:** checkbox, 40 avatar, sender, subject + snippet, time, unread weight, priority badge, star; selected = `row_selected` + optional left brand accent bar; flat on panel (not floating cards); min height 88.

### 4.5 Reading pane

- **Reading toolbar:** Archive / Spam / Delete / Unread / Star (mutate `AppState` where possible).
- Subject + sender header + body on plain surface.
- Empty state card when nothing selected — **no** category dump in the reader.
- Categories / companies live under their own surface / list column, not the empty reader.

### 4.6 Compose

- Overlay compose: To / Subject / Body + Send / Discard.
- Honest UX: Send = local toast/log if no live SMTP; Discard clears draft fields.

---

## 5. What NOT to ship

| Do not | Why |
|--------|-----|
| **Violet Expressive shell** (`#5238E8`, `#B9A8FF`, `#F6F3FF`, `#131120` as Windows brand/canvas) | Design B owns Windows colour; violet remains Android-only |
| **Photo / flowing wallpaper behind mail list or reading pane** | Brief + Android contracts forbid decorative BG behind content; no substitute art |
| **Glassmorphism / frosted mail chrome** | Breeze glass retired; import frosted host is Android-only exception |
| **Android bottom navigation on desktop** | Desktop uses sidebar + category tabs |
| **Fake mailbox destinations** (Snoozed, Sent, Drafts, Trash, …) without data | Sidebar only real capabilities |
| **Large workspace logos / repeated decorative marks** | Compact toolbar + sidebar mark only |
| **Hard-coded hex in Agents 2–3 UI** | Consume `ThemeTokens` only |
| **Off-grid spacing** (13, 17, …) without call-site justification | Keep 4dp grid from `Spacing.kt` |
| **Neon / glow / purple-on-white AI defaults** | Pastel blue + mint + white / navy only |

---

## 6. Agent ownership (reminder)

| Agent | Owns |
|-------|------|
| 1 (this doc) | `Windows/docs/redesign/11-design-ab-contract.md` |
| 2 | `shell.rs`, `navigation.rs`, `workspace.rs`, `brand.rs` (toolbar/sidebar) |
| 3 | `ui/mail_list.rs`, `ui/conversation.rs`, `ui/categories.rs` |
| 4 | `tokens.rs`, `ui/settings.rs`, `ui/compose.rs`, compose fields in `app_state.rs` |
| 5 | Build / QA → `Windows/docs/redesign/12-qa-ab.md` |

Shared API: keep `ThemeTokens` field names stable so Agents 2–3 keep compiling while Agent 4 swaps violet → Design B hex.
