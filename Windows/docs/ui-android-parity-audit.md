# UI Android → Windows parity audit

> **Source of truth:** Android `ui/theme/*` + key screens (Mailstack Expressive, 2026-10-16+), plus `execution plan/Windows/design.md`.  
> **Do not redesign from pre-Expressive plan docs.** Where older phase docs disagree, this checklist + `design.md` win.  
> **Audited:** Windows egui shell (`Windows/src/tokens.rs`, `shell.rs`, `ui/*`, `navigation.rs`) vs Android Compose.  
> **Android tree was not modified.**

Mapping note: Android `dp` / `sp` → Windows logical px **1:1** at 100% scale (per `design.md`). Support Windows text scaling; do not hard-break layout at large sizes.

---

## 1. Exact color hex (Android `Color.kt`)

Fixed “Mailstack Expressive” palette. On Android 12+ the *runtime* may use Material You dynamic schemes (`Theme.kt`); **Windows must ship these fixed tokens** so the app looks the same on every machine (no wallpaper-derived primary).

### Brand (shared / light)

| Token | Hex |
|-------|-----|
| BrandPrimary | `#5238E8` |
| BrandPrimaryContainerLight | `#E3DCFF` |
| OnBrandPrimaryContainerLight | `#1D0F6E` |
| BrandSecondaryLight | `#6A5ACD` |
| BrandSecondaryContainerLight | `#E4E0FF` |
| OnBrandSecondaryContainerLight | `#241A5E` |
| BrandTertiaryLight | `#1E7A55` |
| BrandTertiaryContainerLight | `#B9F5D6` |
| OnBrandTertiaryContainerLight | `#002114` |
| onPrimary (light) | `#FFFFFF` |
| onSecondary / onTertiary (light) | `#FFFFFF` |

### Brand (dark)

| Token | Hex |
|-------|-----|
| BrandPrimaryDark | `#B9A8FF` |
| OnBrandPrimaryDark | `#2E1B8A` |
| BrandPrimaryContainerDark | `#4634A8` |
| BrandSecondaryDark | `#C9BEFF` |
| OnBrandSecondaryDark | `#382E78` |
| BrandSecondaryContainerDark | `#4D4096` |
| BrandTertiaryDark | `#7FE0B2` |
| OnBrandTertiaryDark | `#003828` |
| BrandTertiaryContainerDark | `#005741` |
| onPrimaryContainer (dark scheme) | `#E3DCFF` (= BrandPrimaryContainerLight) |
| onSecondaryContainer (dark) | `#E4E0FF` |
| onTertiaryContainer (dark) | `#B9F5D6` |

### Light surfaces

| Token | Hex |
|-------|-----|
| background | `#F6F3FF` |
| surface | `#FDFBFF` |
| onSurface / onBackground | `#1C1A26` |
| surfaceVariant | `#E7E1F5` |
| onSurfaceVariant | `#48455A` |
| surfaceContainerLowest | `#FFFFFF` |
| surfaceContainerLow | `#F7F4FF` |
| surfaceContainer | `#EFEAFB` |
| surfaceContainerHigh | `#E7E0F6` |
| surfaceContainerHighest | `#DCD3F0` |
| outline | `#7B7790` |
| outlineVariant | `#CBC5DE` |

### Dark surfaces (never pure black for app background)

| Token | Hex |
|-------|-----|
| background | `#131120` |
| surface | `#1A1826` |
| onSurface / onBackground | `#F0EDFB` |
| surfaceVariant | `#2E2A44` |
| onSurfaceVariant | `#CBC6DE` |
| surfaceContainerLowest | `#000000` |
| surfaceContainerLow | `#1F1C2E` |
| surfaceContainer | `#262340` |
| surfaceContainerHigh | `#322D4E` |
| surfaceContainerHighest | `#3D3859` |
| outline | `#948EA9` |
| outlineVariant | `#48445C` |

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
| error | (same as ErrorLight/Dark) | |

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

**Category group header accents** (from `CategoryGroups.kt`, not invent new ones):

| Group | Accent source | Light | Dark |
|-------|---------------|-------|------|
| Needs attention | security | `#B45309` | `#FBBF24` |
| Promotions | promotions | `#EA580C` | `#FB923C` |
| Updates | notifications | `#0284C7` | `#38BDF8` |
| Work & learning | career | `#1D4ED8` | `#60A5FA` |
| Everything else | important (brand) | `#5238E8` | `#B9A8FF` |

Leading circle fill = accent @ **14% alpha**; icon tint = full accent.

### Warning badge colors (`Cards.kt`)

| | Light | Dark |
|--|-------|------|
| container | `#FFEFC4` | `#4A3200` |
| content | `#7A4A00` | `#FFD88A` |

### Import flow hardcoded dark palette (`ImportScreens.kt` — both themes)

| Role | Hex |
|------|-----|
| App background | `#101418` |
| Frosted scrim | `#B30E1115` (alpha included) |
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

## 2. Typography sizes (Android `Type.kt`)

Preferred face: **Inter**; Windows: **Segoe UI Variable** (+ system fallback). Weights as listed.

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

**Mail row type mapping (Android `ThreadRow` / `MailRowLayout`):**

| Element | Style | Unread treatment |
|---------|-------|------------------|
| Sender | titleMedium (18) | Bold if unread |
| Time | labelMedium (12) | primary color if unread, else onSurfaceVariant |
| Subject | bodyMedium (14) | Bold if unread |
| Snippet | bodyMedium (14) | onSurfaceVariant |
| Files / count | labelSmall (11) | onSurfaceVariant |

> Note: `execution plan/Windows/design.md` typography line (“display 64/57, title 22/17/16”) is **stale** vs `Type.kt`. Prefer this table.

---

## 3. Shape radii (Android `Shapes.kt` / `Cards.kt`)

| Token | Value | Use |
|-------|-------|-----|
| MoRadii.small / extraSmall | **12dp** | Small controls |
| MoRadii.medium | **16dp** | Medium surfaces |
| MoRadii.large | **24dp** | Large surfaces / FAB-like |
| MoRadii.extraLarge / MoCardShape | **28dp** | Expressive **cards** |
| MoRadii.pill | **CircleShape** | Buttons, chips, nav indicators, account pills |

Rules:

- Cards: solid `surfaceContainer`, **28dp**, **no border, no shadow, tonalElevation 0**. Clip highlight **before** click (no square ripple over round corners).
- Mail rows: **not cards** — flat on `background`.
- Hierarchy = tonal steps (background → surfaceContainer → surfaceContainerHigh), never elevation chrome.

---

## 4. Spacing grid (Android `Spacing.kt`)

Strict **4dp** grid:

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

Row geometry:

| Token | dp |
|-------|----|
| rowMinHeight | **88** |
| rowAvatar | **40** |
| companyRowMinHeight | 64 |
| messageHeaderMinHeight | 48 |
| searchRowMinHeight | 72 |

---

## 5. Mail row anatomy (Android `MailRowLayout`)

Checklist — must match on every list (Mail, category detail, search, starred, company detail):

| Spec | Value |
|------|-------|
| Chrome | Flat on background — **no** card / border / shadow / divider |
| Min height | **88dp** |
| Horizontal padding | 16 (`md`) |
| Vertical padding | 8 (`xs`) |
| Avatar | **40dp** circle; company logo if domain known, else letter avatar |
| Avatar → text gap | 16 (`md`) |
| Lines | **3**: (1) sender + time, (2) subject (+ optional priority badge), (3) snippet (+ Files / count / conversation badge) |
| Unread | Bold sender + subject; time uses **primary** |
| Star | Right edge, tertiary tint, ~20dp icon; starred rows only |
| Unified inbox | Optional **receiving-account** avatar **left of** sender avatar (32dp = `xxl`), not a fourth text line |
| Account indicator | Receiving account — never confused with sender/company avatar |
| No Action Required chip | In mail list (removed on Android) |

---

## 6. Navigation destinations

### A. Primary tabs (Android bottom nav — `PrimaryDestination`)

Order: **Home → Mail → Categories → Companies → Actions**

Bar: `surfaceContainer`, height **80dp** (+ system inset). Jelly bubble pill: half-width 32, height 32, top offset 12; icon 24. Selected icon tint = `onSecondaryContainer` when pill covers icon.

### B. Mailbox destinations inside Mail (Android `MailboxDestination` / Windows sidebar)

| ID | Label |
|----|-------|
| ALL_INBOX | All Inbox |
| PRIMARY | Primary |
| PROMOTIONAL | Promotional |
| SOCIAL | Social |
| SPAM | Spam |
| STARRED | Starred |

Companies are **never** sidebar destinations — only filters inside a category.

### C. Secondary / pushed routes (Android `AppDestinations`)

| Route | Title |
|-------|-------|
| search | Search |
| settings | Settings |
| privacy | Privacy Center |
| privacy-policy | Privacy Policy |
| terms | Terms of Service |
| accounts | Accounts (inline in Settings header) |
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

Settings rows (implemented-only): Privacy Center, Rules, Integrations, Cleanup & Newsletters, Analytics & Insights, AI Assistance, Advanced Automation, Appearance; footer = “Privacy Policy · Terms of Service” + About paragraph (not list buttons).

---

## 7. Categories UI (Android `CategoriesScreen`)

- Five groups (`CATEGORY_GROUPS`), each **one** `MoCard` (28dp).
- Header: vertically centred row — accent circle **40dp** @ 14% alpha + icon, title (`titleMedium`), “N messages” + subtitle (`bodySmall`), chevron.
- Expanded members: full-width rows **inside same card** (no inset, no nested cards).
- Member detail lists reuse **mail row** + logo pipeline.

---

## 8. Import flow (Android `ImportScreens`)

- Full-screen frosted host; card keeps **hardcoded dark** palette in both themes.
- Gmail mark left, Mailstack mark right — **real logos**, not text substitutes.
- Conduit arc = **pulse only** (not progress). **Only** Mailstack progress ring is the progress bar.
- Progress from real sync; never backwards; total from `messagesTotal` once; cap **99%** until Done = **100%**.
- Flying mails on arc; recent rows = 2 most recently inserted.
- Continue gated on Done; Back / “Not now” skips animation but may background-sync.
- Error keeps card chrome (Try again / Continue anyway).

---

## 9. Motion (Android `Motion.kt`)

| Spec | Duration / easing |
|------|-------------------|
| Fade-through exit | 200ms FastOutSlowIn |
| Fade-through enter | 350ms + 120ms delay |
| Tab switch slide | 350ms FastOutSlowIn (opaque) |
| Settings / panel slide | 350ms FastOutSlowIn |
| Panel fade | 350ms |
| Top bar reshape | 350ms |
| Scroll-hide bars | 250ms translate (design.md) |
| Message reveal | 350ms expand + fade |

Respect reduced motion.

---

## 10. What **must** differ on Windows desktop

| Android | Windows (intentional) |
|---------|------------------------|
| Bottom nav (5 primary tabs) + jelly bubble | Persistent **left sidebar** for mailbox destinations; primary app areas as sidebar sections or toolbar — **not** a phone bottom bar |
| Touch-first 48dp+ targets | Pointer: hover fill on flat rows (`row_hover`), selection tint, context menus, keyboard shortcuts |
| Full-screen Settings slide | Right **settings pane** (~360px), 350ms slide; content may shift ~1/12; OS window chrome stays |
| System nav / status insets | Win32 / WinUI window chrome, title bar, snap, multi-window, taskbar |
| Material You dynamic color (API 31+) | **Fixed** Expressive palette only |
| Scroll-hide top/bottom bars | Optional; if shipped, same 250ms semantics — must not thrash layout every frame |
| Native Material scrollbar | Prefer **native Windows** scrollbars |
| Single-column phone | Multi-pane: sidebar \| list \| detail \| optional inspector |
| Segue-style tab slide | In-place content swap; no fake “page reload” |

Do **not** differ: brand hex, card 28, mail row 40/88/3-line flat, category group accents, import honesty rules, copy/privacy rules.

---

## 11. Current Windows egui gaps vs this checklist

### Token / theme gaps

| # | Gap | Android | Windows now |
|---|-----|---------|-------------|
| T1 | Full M3 role set | secondary, tertiary, outline, all surfaceContainer* tiers, error* | Partial (`tokens.rs`): brand, bg, surface, elevated, high, primaryContainer, text — **missing** secondary/tertiary/outline/error/status/warning-badge |
| T2 | Category accents | Exact `MoCategoryColors` hex | Approximate `ACCENT_*` (`#F5A623`, `#F07D26`, …) ≠ Android group accents |
| T3 | Typography scale | Full Type.kt table | Truncated: DISPLAY=32, TITLE_LG=22, TITLE_MD=17, no 64/48/40/36/28/24/18/11 |
| T4 | Spacing names / completeness | 4–64 + row geometry tokens | Partial (`XS/SM/MD/LG/XL`); **MD=12** vs Android `md=16` naming collision risk |
| T5 | Pill | CircleShape | `PILL_RADIUS=20` (acceptable for indicators; document as intentional) |

### Screen / component gaps

| # | Gap | Detail |
|---|-----|--------|
| G1 | Mail row anatomy | Hover/selected OK; **no logo pipeline** (letter + brand tint only); account shown as **8px dot + 4th text line** instead of left 32dp receiving avatar; star beside time not right-edge tertiary; sender sized as BODY_LG(16) not titleMedium(18); snippet as LABEL_LG not bodyMedium |
| G2 | Category cards | Fixed 72px tinted slabs; not `surfaceContainer` 28dp single-cavity with expand, 40dp @14% accent circle, member rows, subtitle |
| G3 | App primary destinations | No Home / Categories / Companies / Actions surfaces — only mailbox sidebar (6 dests) |
| G4 | Settings pane | Theme + Privacy + Import stubs; **missing** expandable account switcher section, Rules, Integrations, Cleanup, Analytics, AI, Automation; **slide animation disabled** (instant open); footer links not navigable |
| G5 | Import flow | Centered demo window; wrong palette (`#1A1728` vs Android `#1C1F24` / `#101418`); text “Gmail → Mailstack” not real logos; **no conduit pulse / flying mails / recent rows**; Simulate +10% demo controls |
| G6 | Motion | Settings slide / scroll-hide effectively off to avoid shake; no 350ms panel character |
| G7 | Nav chrome | Jelly bubble N/A (OK); sidebar pills present but no secondaryContainer indicator language from Android nav |
| G8 | Cards elsewhere | Settings/Privacy not consistently MoCard tonal containers |

---

## 12. Top 10 gaps (priority for Windows match work)

1. **Ship full Color.kt token set** (surfaces, secondary/tertiary, outline, error, status, category, warning badges) — fixed palette, both modes.  
2. **Align category group accents** to Android hex (especially Needs attention = security amber `#B45309` / `#FBBF24`, not `#F5A623`).  
3. **Complete Type.kt scale** in egui (at least titleMedium 18 for mail sender; keep compact body/label for rows).  
4. **Fix mail row anatomy**: 40 avatar + logo pipeline, 3 lines only, min 88, flat; receiving-account avatar left; star right tertiary; unread bold + primary time.  
5. **Rebuild category groups** as single-cavity 28px `surfaceContainer` cards with 14% accent circle + expand members.  
6. **Add primary destinations** (Home, Categories, Companies, Actions) adapted to desktop chrome — not only mailbox sidebar.  
7. **Settings parity**: account expander, real settings rows, Gmail-style footer links; restore 350ms slide without layout thrash.  
8. **Import flow parity**: hardcoded Android import palette, real logos, conduit pulse ≠ progress, ring-only progress, honest % / Continue gate.  
9. **Spacing**: adopt Android 4dp grid values under clear names (`space::MD = 16`) to stop 12-vs-16 drift.  
10. **Motion / chrome**: re-enable intentional 350ms settings + 250ms scroll-hide under motion tiers; keep hover/selection as Windows-only affordances.

---

## Sources (read-only)

- `android/app/src/main/java/com/greninjaop/mailorganizer/ui/theme/{Color,Type,Shapes,Spacing,Motion,Cards,Theme}.kt`
- `android/.../ui/mail/ThreadRow.kt`, `ui/navigation/{MoNavBar,AppNavGraph}.kt`, `ui/categories/{CategoryGroups,CategoriesScreen}.kt`, `ui/settings/SettingsScreens.kt`, `ui/import/ImportScreens.kt`, `ui/mail/MailUiState.kt` (`MailboxDestination`)
- `execution plan/Windows/design.md`
- Windows: `Windows/src/tokens.rs`, `shell.rs`, `navigation.rs`, `ui/{mail_list,categories,settings,import_flow}.rs`
