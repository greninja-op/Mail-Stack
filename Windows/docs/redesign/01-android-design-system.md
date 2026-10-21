# 01 — Android Design System (Windows redesign source of truth)

> **Agent 1 audit.** Extracted from Android as shipped (Mailstack Expressive, 2026-10-16+), not invented.  
> **Do not modify `android/`.** Windows implements parity from this document + `execution plan/Windows/design.md`.  
> Mapping: Android `dp`/`sp` → Windows logical px **1:1** at 100% scale. Support OS text scaling.

---

## 0. Critical conflict — brief vs Android-as-shipped

| Source | Says |
|--------|------|
| Some user / redesign briefs | Pastel **blue / mint**, **photo backgrounds**, **glass** / frosted UI |
| Android `Color.kt` + `Cards.kt` | **Vivid violet** Expressive brand (`#5238E8` / `#B9A8FF`), solid tonal surfaces, **no photo BG**, **no glass cards** |
| `execution plan/Windows/design.md` | Same violet; **“No photo backgrounds, no glass UI”** |
| `Color.kt` header comment | Explicitly retired Breeze glassmorphism |

### Tertiary mint is not “pastel blue”

Android does use mint as **tertiary** (success / star tint / accents): light `#1E7A55` / container `#B9F5D6`; dark `#7FE0B2`. That is a **secondary accent**, not the brand primary. Brand primary is violet.

### Glass exception (only)

The **import frosted transfer host** (`ImportScreens.kt`) is the sole intentional “frosted” surface: hardcoded dark backdrop + scrim + card. It is **not** a global glass system. Do not generalize frosted/glass to mail lists, settings cards, or shell chrome.

### Photo backgrounds

- Design contracts (**forbid** app-wide photo / wallpaper backgrounds for product chrome).
- Assets that **exist** and may be used for **marketing / store / splash / about** only if product owners explicitly request them:
  - `banner/banner.png`
  - `logo/logo.png` (also copied as `Windows/resources/icons/app_logo.png`)
- **Recommendation for Windows redesign:** implement **Android-as-shipped tokens** for UI parity. Use photo BGs **only** if those assets are wired for a non-chrome surface (e.g. About / Store listing), never as the mail shell background.

### Windows color policy vs Android runtime

On Android 12+, `Theme.kt` may apply Material You **dynamic** color from wallpaper. **Windows must ship the fixed Expressive hex tables below** so every machine looks the same (matches `execution plan/Windows/design.md`).

---

## 1. Brand & color (exact hex from `Color.kt`)

### Brand

| Token | Light | Dark |
|-------|-------|------|
| primary | `#5238E8` | `#B9A8FF` |
| onPrimary | `#FFFFFF` | `#2E1B8A` |
| primaryContainer | `#E3DCFF` | `#4634A8` |
| onPrimaryContainer | `#1D0F6E` | `#E3DCFF` |
| secondary | `#6A5ACD` | `#C9BEFF` |
| onSecondary | `#FFFFFF` | `#382E78` |
| secondaryContainer | `#E4E0FF` | `#4D4096` |
| onSecondaryContainer | `#241A5E` | `#E4E0FF` |
| tertiary (mint) | `#1E7A55` | `#7FE0B2` |
| onTertiary | `#FFFFFF` | `#003828` |
| tertiaryContainer | `#B9F5D6` | `#005741` |
| onTertiaryContainer | `#002114` | `#B9F5D6` |
| inversePrimary | `#B9A8FF` | `#5238E8` |

### Surfaces (never pure black as app background)

| Token | Light | Dark |
|-------|-------|------|
| background | `#F6F3FF` | `#131120` |
| surface | `#FDFBFF` | `#1A1826` |
| onSurface / onBackground | `#1C1A26` | `#F0EDFB` |
| surfaceVariant | `#E7E1F5` | `#2E2A44` |
| onSurfaceVariant | `#48455A` | `#CBC6DE` |
| surfaceContainerLowest | `#FFFFFF` | `#000000` |
| surfaceContainerLow | `#F7F4FF` | `#1F1C2E` |
| surfaceContainer | `#EFEAFB` | `#262340` |
| surfaceContainerHigh | `#E7E0F6` | `#322D4E` |
| surfaceContainerHighest | `#DCD3F0` | `#3D3859` |
| outline | `#7B7790` | `#948EA9` |
| outlineVariant | `#CBC5DE` | `#48445C` |

Hierarchy = **tonal steps** (background → surfaceContainer → surfaceContainerHigh), not borders or elevation shadows.

### Errors

| Token | Light | Dark |
|-------|-------|------|
| error | `#B3261E` | `#FFB4AB` |
| onError | `#FFFFFF` | `#690005` |
| errorContainer | `#F9DEDC` | `#93000A` |
| onErrorContainer | `#410E0B` | `#FFDAD6` |

### Status (`MoStatusColors`)

| Role | Light | Dark |
|------|-------|------|
| success | `#1E7A55` | `#4ADE80` |
| warning | `#B45309` | `#FBBF24` |
| info | `#1D4ED8` | `#60A5FA` |
| error | `#B3261E` | `#FFB4AB` |

### Category accents (`MoCategoryColors`)

| Category | Light | Dark |
|----------|-------|------|
| actionRequired | `#B3261E` | `#FFB4AB` |
| important | `#5238E8` | `#B9A8FF` |
| career | `#1D4ED8` | `#60A5FA` |
| education | `#7C3AED` | `#C4B5FD` |
| receiptsOrders | `#1E7A55` | `#4ADE80` |
| security | `#B45309` | `#FBBF24` |
| notifications | `#0284C7` | `#38BDF8` |
| newsletters | `#0D9488` | `#5EEAD4` |
| promotions | `#EA580C` | `#FB923C` |
| lowValue / unclassified | `#6B7280` | `#9CA3AF` |

Color is never the only category indicator — always pair with label + icon / content description.

### Category **group** header accents (`CategoryGroups.kt`)

| Group | Accent source | Light | Dark |
|-------|---------------|-------|------|
| Needs attention | security | `#B45309` | `#FBBF24` |
| Promotions | promotions | `#EA580C` | `#FB923C` |
| Updates | notifications | `#0284C7` | `#38BDF8` |
| Work & learning | career | `#1D4ED8` | `#60A5FA` |
| Everything else | important (brand) | `#5238E8` | `#B9A8FF` |

Leading circle fill = accent @ **14% alpha**; icon tint = full accent.

### Warning badge (`Cards.kt` `MoWarningColors`)

| | Light | Dark |
|--|-------|------|
| container | `#FFEFC4` | `#4A3200` |
| content | `#7A4A00` | `#FFD88A` |

### Import-only hardcoded dark palette (`ImportScreens.kt` — both app themes)

| Role | Hex |
|------|-----|
| App background | `#101418` |
| Frosted scrim | `#B30E1115` (includes alpha) |
| Card surface | `#1C1F24` |
| On surface | `#E2E2E6` |
| Body | `#C4C6D0` |
| Muted | `#8E9099` |
| Connected blue | `#8AB4F8` |
| Primary pill blue | `#9CCAFF` |
| Avatar blue | `#1B5E8A` |
| On primary pill navy | `#003258` |
| Outline stroke | `#3C4043` |
| Timestamp blue | `#7FCFFF` |

---

## 2. Typography (`Type.kt`)

Preferred face on Android: **Inter** (not bundled → system sans). Windows: **Segoe UI Variable** (+ system fallback).

| Role | Size (sp/px) | Line height | Weight | Letter spacing |
|------|--------------|-------------|--------|----------------|
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

Display/headline are expressive (empty states, big headers). Body/label stay compact for mail rows and settings.

> Note: `execution plan/Windows/design.md` line “display 64/57, title 22/17/16” is **stale** vs `Type.kt`. Prefer this table.

### Mail-row type map (`MailRowLayout`)

| Element | Style | Unread |
|---------|-------|--------|
| Sender | titleMedium (18) | Bold |
| Time | labelMedium (12) | `primary` if unread, else `onSurfaceVariant` |
| Subject | bodyMedium (14) | Bold |
| Snippet | bodyMedium (14) | `onSurfaceVariant` |
| Files / count | labelSmall (11) | `onSurfaceVariant` |
| Star icon | ~20dp | `tertiary` tint |

---

## 3. Radii & cards (`Shapes.kt`, `Cards.kt`)

| Token | Value | Use |
|-------|-------|-----|
| small / extraSmall | **12** | Small controls |
| medium | **16** | Medium surfaces |
| large | **24** | Large / FAB-like |
| extraLarge / MoCardShape | **28** | Expressive cards |
| pill | CircleShape | Buttons, chips, nav indicators, account pills |

**Card rules**

- Solid `surfaceContainer`, **28** corner, **no border, no shadow**, `tonalElevation = 0`.
- Clip interaction highlight **before** click (no square ripple over round corners).
- Mail rows are **not** cards — flat on `background` (Gmail-like).
- Cards = grouped content only: category groups, settings sections, opened messages, rules, etc.

---

## 4. Spacing (`Spacing.kt`)

Strict **4dp** grid — no off-grid values without a documented call-site reason.

| Token | dp |
|-------|----|
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

### Row geometry (shared across Mail, company, thread, search, Companies)

| Token | dp | Notes |
|-------|----|-------|
| rowMinHeight | **88** | Mail / conversation row |
| rowAvatar | **40** | Sender / company avatar |
| companyRowMinHeight | 64 | Company list row |
| messageHeaderMinHeight | 48 | Thread message header |
| searchRowMinHeight | 72 | Search hit row |

---

## 5. Motion (`Motion.kt` + design contracts)

### Motion tiers (design.md)

| Tier | Use |
|------|-----|
| Essential | Short, low-cost state changes |
| Standard | Normal navigation / list interactions |
| Expressive | Hero / sync / important transitions only — not every click |
| Reduced motion | Minimal fades / position changes (a11y / power) |

### Concrete Android specs

| Spec | Duration / easing |
|------|-------------------|
| Fade-through exit (pushed routes) | **200ms** FastOutSlowIn |
| Fade-through enter | **350ms** + **120ms** delay, FastOutSlowIn |
| Fade-through scale out/in | 1.0→0.97 / 0.97→1.0 (same timings as fade) |
| Primary tab switch slide | **350ms** FastOutSlowIn, **opaque** (no fade) |
| Settings / panel slide | **350ms** FastOutSlowIn |
| Panel bars fade | **350ms** FastOutSlowIn |
| Top bar reshape | **350ms** FastOutSlowIn |
| Scroll-hide bars (design.md) | **250ms** pure translate; down = hide, up = show; enter view = bars visible |
| Message reveal | **350ms** expand + fade |
| Import overlay enter/exit | ~260ms fade in / ~320ms fade out; finish delay ~340ms |

No animation may block mail interaction or synchronization. Respect reduced motion.

---

## 6. Mail-row anatomy (`ThreadRow` / `MailRowLayout`)

Identical on every list (Mail, category detail, search, starred, company detail):

```
[recv-acct?] [40 avatar]  sender ……………… time
              16 gap      subject …… [priority?]
                          snippet … [Files] [N] [conv?]
                                              [★]
```

| Spec | Value |
|------|-------|
| Chrome | Flat on background — **no** card / border / shadow / divider |
| Min height | **88** |
| Horizontal padding | 16 (`md`) |
| Vertical padding | 8 (`xs`) |
| Avatar | **40** circle; **company logo** if `senderDomain` known, else **letter** avatar |
| Logo pipeline | bundled company PNG → app assets → website favicon → letter; never fabricate |
| Avatar → text gap | 16 (`md`) |
| Lines | **3** only |
| Unread | Bold sender + subject; time uses **primary** |
| Star | Right edge, tertiary tint (~20 / `lg`); show when starred |
| Unified / All Inbox | Optional **receiving-account** avatar **left of** sender avatar (**32** = `xxl`), gap 12 (`sm`) — receiving account ≠ sender |
| No “Action Required” chip | Removed from Mail list on Android |

---

## 7. Navigation destinations

### A. Primary app tabs (`PrimaryDestination` — Android bottom nav)

Order: **Home → Mail → Categories → Companies → Actions**

Android bar: `surfaceContainer`, height **80** (+ inset); jelly bubble half-width 32, height 32, top offset 12; icon 24. Selected icon uses `onSecondaryContainer` when pill covers icon.

### B. Mailbox destinations inside Mail (`MailboxDestination`)

| ID | Label |
|----|-------|
| ALL_INBOX | All Inbox |
| PRIMARY | Primary |
| PROMOTIONAL | Promotional |
| SOCIAL | Social |
| SPAM | Spam |
| STARRED | Starred |

- Compact, stable drawer/sidebar — **not** a list of companies.
- Company grouping/filter lives **inside** the selected category.
- Spam: red unread/new indicator when applicable; counts must be real.

### C. Secondary / pushed routes (`AppDestinations`)

| Route | Title |
|-------|-------|
| search | Search |
| settings | Settings |
| privacy | Privacy Center |
| privacy-policy | Privacy Policy |
| terms | Terms of Service |
| accounts | Accounts (also inline in Settings) |
| connect | Connect account |
| import/{accountId} | Import |
| rules (+ editor) | Rules |
| integrations (+ detail) | Integrations |
| cleanup | Cleanup & Newsletters |
| analytics | Analytics & Insights |
| settings/ai | AI Assistance |
| automation (+ editor) | Advanced Automation |
| mail/thread/{id} | Thread |
| category/{name} | Category detail |
| company/{id} | Company detail |

### Settings content (implemented-only on Android)

Sections: expandable **Accounts**, **Appearance** (theme), then navigable rows for Privacy / Rules / Integrations / Cleanup / Analytics / AI / Automation as built. Non-important items: Gmail-style footer **“Privacy Policy · Terms of Service”** + centred About paragraph — not primary list buttons.

Settings opens as a slide-from-right panel (**350ms**); overlaid bars fade with the panel.

---

## 8. Categories UI (summary)

- Five groups (`CATEGORY_GROUPS`), each **one** `MoCard` (28).
- Header: vertically centred — accent circle **40** @ 14% alpha + icon, title (`titleMedium`), “N messages” + subtitle (`bodySmall`), chevron.
- Expanded members: full-width rows **inside same card** (no nested cards / seams).
- Member detail lists reuse mail-row + logo pipeline.

---

## 9. Import flow (summary)

- Frosted host exception (see §0); card keeps hardcoded dark palette in both themes.
- Gmail mark left, Mailstack mark right — **real logos** (`ic_gmail_logo.png`, `ic_mailstack_logo.png`).
- Conduit arc = **pulse only** (not progress). **Only** Mailstack progress ring is the progress bar.
- Progress from real sync; never backwards; total from `messagesTotal` once; cap **99%** until Done = **100%**.
- Flying mails on arc; recent rows = 2 most recently inserted.
- Continue gated on Done; Back / “Not now” skips animation but may background-sync.
- Error keeps card chrome (Try again / Continue anyway).

---

## 10. Desktop adaptations (Windows)

Intentional differences — shared product concepts stay Android-aligned:

| Android | Windows |
|---------|---------|
| Bottom nav (5 tabs) + jelly bubble | Persistent **left chrome**: mailbox destinations as sidebar; primary areas (Home / Categories / Companies / Actions) as sidebar sections or equivalent desktop IA — **not** a phone bottom bar |
| Touch 48+ targets | Pointer: hover on flat rows, selection tint, context menus, keyboard shortcuts |
| Full-screen Settings slide | Right **settings pane** (~360px), 350ms slide; content may shift ~1/12 |
| System insets | Win32 / WinUI window chrome, title bar, snap, multi-window, taskbar |
| Material You dynamic color | **Fixed** Expressive palette only |
| Material scrollbar | Prefer **native Windows** scrollbars |
| Single-column phone | Multi-pane: sidebar \| list \| detail \| optional inspector |
| Tab slide between primaries | In-place content swap; **navigation never fakes a page reload** |

**Do not differ:** brand hex, card 28, mail row 40/88/3-line flat, category group accents, import honesty, privacy (no remote image fetch in bodies), copy rules.

---

## 11. PNG asset inventory

### Product / brand

| Path | Role |
|------|------|
| `logo/logo.png` | Canonical Mailstack logo (~1.5 MB) |
| `Windows/resources/icons/app_logo.png` | Windows app icon copy of logo |
| `banner/banner.png` | Marketing / store banner (~98 KB) — **not** shell chrome background |
| `android/.../drawable-nodpi/ic_mailstack_logo.png` | In-app Mailstack mark (import, etc.) |
| `android/.../drawable-nodpi/ic_gmail_logo.png` | Gmail mark for import / connect |
| `android/.../drawable-nodpi/ic_calendar_logo.png` | Calendar integration mark |
| `android/.../drawable-nodpi/ic_tasks_logo.png` | Tasks integration mark |

### Bundled company logos (`android/.../drawable-nodpi/`)

`company_adobe.png`, `company_alibabacloud.png`, `company_amazon.png`, `company_amazon_web_services.png`, `company_apple.png`, `company_canva.png`, `company_clickup.png`, `company_devpost.png`, `company_dribbble.png`, `company_duolingo.png`, `company_github.png`, `company_glasp.png`, `company_google.png`, `company_kick.png`, `company_linkedin.png`, `company_netflix.png`, `company_openrouter.png`, `company_reddit.png`, `company_spotify.png`, `company_x.png`, `company_youtube.png`

Windows should mirror this set under its own resources when implementing the logo pipeline (do not invent replacements).

---

## 12. Sources (read-only)

- `android/app/src/main/java/com/greninjaop/mailorganizer/ui/theme/{Color,Type,Shapes,Spacing,Motion,Cards,Theme}.kt`
- `android/.../ui/mail/{ThreadRow,MailUiState,MailDrawer,MailScreen}.kt`
- `android/.../ui/navigation/{MoNavBar,AppNavGraph}.kt`
- `android/.../ui/categories/{CategoryGroups,CategoriesScreen}.kt`
- `android/.../ui/settings/SettingsScreens.kt`
- `android/.../ui/import/ImportScreens.kt`
- Token guards: `MoColorTokensTest.kt`, `MoDesignTokensTest.kt`
- `execution plan/android/design.md`
- `execution plan/Windows/design.md`
- Related checklist: `Windows/docs/ui-android-parity-audit.md`

---

## 13. Recommendation (Agent 1 verdict)

1. **Ship Android Expressive violet tokens** as the Windows redesign baseline — not pastel-blue/mint shells.  
2. **No photo or glass chrome** for mail/settings; import frosted host only.  
3. **`banner/` / `logo/`** exist for marketing/icon use; do not paint them behind the inbox.  
4. Treat this file as the token + anatomy contract for subsequent redesign docs (`02-…`).
