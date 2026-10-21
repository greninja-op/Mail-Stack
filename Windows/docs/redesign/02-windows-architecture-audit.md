# 02 — Windows architecture audit (egui/eframe)

> **Agent 2 deliverable.** Inspected `Windows/src/{shell,app_state,tokens,navigation,workspace,brand,main}.rs` and `Windows/src/ui/*`.  
> **Scope of this pass:** document only — no product code changes.  
> **Companion:** `Windows/docs/ui-android-parity-audit.md` (token/screen checklist).  
> **Date:** 2026-10-21.

---

## 1. Framework decision — keep egui

| Layer | Current | Decision for redesign pass |
|-------|---------|----------------------------|
| Runtime | `eframe` 0.29 + `egui` 0.29 (`glow`), `#![windows_subsystem = "windows"]` | **Keep** |
| Entry | `main.rs` → `shell::run` | Unchanged |
| Packaging aspiration | WinUI 3 / Windows App SDK + Fluent 2 (MSIX stubs) | **Out of scope** — do not start a WinUI rewrite in Agents 3–4 |
| Shared logic | `desktop-core` via `bridge.rs` | **Preserve** — UI must not reimplement business rules |

**Rationale:** The interim shell already hosts multi-pane mail, overlays, and core bridge demos. Redesign work is presentation/layout/token parity with Android Expressive — not a host migration. WinUI remains the later packaging path (`CONTEXT.md`, `packaging/RELEASE_STATUS.md`); claiming or scaffolding it now would fork effort and break the egui shake mitigations already hard-won.

**Implication for Agents 3–4:** All layout/visual fixes stay inside the egui Immediate Mode API (`CentralPanel` / `SidePanel` / `TopBottomPanel` / `Area` / `Window`). Prefer fixed panel widths and mode-gated visual re-apply (see §3 shake lessons).

---

## 2. File ownership map (Agents 3–4)

Split by **layout/chrome vs screens/overlays**. Shared types may be read by both; only the owning agent should edit unless coordinated.

### Agent 3 — Shell, geometry, tokens, chrome

| File | Role | Ownership notes |
|------|------|-----------------|
| `src/shell.rs` | App loop, top chrome, sidebar, workspace host, account switcher window | Primary. Fix column host, chrome composition, panel mount order. |
| `src/workspace.rs` | `WorkspaceLayout`, `allocate_columns`, load states | Primary. Fix double-counted sidebar width (see §3). |
| `src/tokens.rs` | Theme tokens, spacing, type sizes, import palette, `apply_egui_visuals` | Primary. Close remaining Color.kt / Type.kt gaps; never re-apply visuals every frame. |
| `src/navigation.rs` | `NavDestination`, `OverlayPane`, settings progress | Shared read; Agent 3 owns destination/IA changes that affect sidebar/chrome. |
| `src/brand.rs` | Logo load + window icon + egui texture | Primary for logo placement policy (header vs taskbar-only). |
| `src/main.rs` | Module wiring, GUI subsystem | Touch only if modules rename; no feature work. |
| `src/app_state.rs` | Theme/motion/layout fields that chrome needs | Coordinate with Agent 4 for mail/search fields; Agent 3 owns `bars_visible`, theme/motion tick behavior. |
| `resources/icons/app_logo.png` | Shared Android mark | Agent 3 decides chrome usage; do not delete asset. |

### Agent 4 — Mail workspace screens & overlays

| File | Role | Ownership notes |
|------|------|-----------------|
| `src/ui/mail_list.rs` | Flat ThreadRow / MailRowLayout | Primary. Row anatomy, hover/selection, logo pipeline presentation. |
| `src/ui/conversation.rs` | Message detail, priority/temporal cards | Primary. Keep body sanitization via bridge; **stop hosting categories here** (coordinate with Agent 3 for where categories live). |
| `src/ui/categories.rs` | Category group MoCards | Primary. Relocate mount point out of conversation column (Agent 3 provides host). |
| `src/ui/settings.rs` | Right settings pane | Primary. Content/rows; width/slide policy owned with Agent 3 (fixed width — no animated panel width). |
| `src/ui/privacy_center.rs` | Privacy Center tabs | Primary. Preserve core privacy contracts (local purge, no Gmail wipe). |
| `src/ui/search.rs` | Search overlay | Primary. Keep structured `SearchQuery` + debounce; UI never builds SQL. |
| `src/ui/import_flow.rs` | Import demo window | Primary. Honest % (cap 99 until Done), import palette, Continue gate. |
| `src/ui/mod.rs` | Module exports | Shared; either agent if adding modules. |
| `src/app_state.rs` | `MailRowModel`, sample mail, filters, search, import %, privacy flags | Agent 4 owns mail/search/import/privacy fields; preserve demo seed. |

### Do not own / touch in this redesign pass

| Path | Why |
|------|-----|
| `android/**` | Hard constraint — no Android edits |
| `desktop-core/**` | Preserve business logic; consume via `bridge.rs` only |
| `Windows/src/bridge.rs` | Facade only — extend only if a screen needs a new **thin** core call already present in desktop-core |
| `Windows/src/adapters/**` | Credential / browser / notifications — out of visual redesign |
| `Windows/packaging/**` | WinUI/MSIX — packaging agents later |
| WinUI / C# / XAML project | Explicitly **not** introduced |

### Suggested sequencing

1. **Agent 3:** fix `allocate_columns` + chrome (logo, empty space), token completeness.  
2. **Agent 4:** move categories off conversation, polish mail/settings/privacy/search/import against parity audit.  
3. Joint smoke: demo mail select → search → settings → privacy → import → theme toggle (no shake).

---

## 3. Current layout defects

### 3.1 Empty space (column math bug)

`shell::paint_columns` runs inside `CentralPanel` (sidebar already consumed by `SidePanel::left`). It still calls `WorkspaceLayout::allocate_columns(available)`, which **subtracts `sidebar_width` again**, then only draws `cols.list` + `cols.detail`.

Result: ~160–248 logical px of the central panel is reserved for a phantom sidebar and never painted → large unused strip / cramped list+detail.

**Fix direction (Agent 3):** either (a) allocate list|detail|inspector only from central width, or (b) pass `content_available` and set `sidebar = 0` for that path. Inspector checkbox in settings already exists but inspector column is not drawn in `paint_columns` today.

### 3.2 Purple / Expressive mismatch vs Android

Intentional brand is Android Expressive violet (`#5238E8` light / `#B9A8FF` dark) — **not** a random purple theme. Remaining mismatches:

| Area | Status |
|------|--------|
| Brand + background + surfaceContainer tiers | Mostly aligned in `tokens.rs` |
| Category group accents | Aligned to audit hex via `group_accent` |
| Secondary / tertiary containers, outline, error*, status*, warning badge | Still incomplete on `ThemeTokens` (parity audit T1) |
| Full Type.kt display/headline ladder | Truncated (`DISPLAY=32`; missing 64/48/40/36/28) |
| egui default widget fills | Can still read as “generic dark UI” where custom frames are not used |
| Default `theme_mode` | `Dark` — fine if charcoal `#131120`; wrong if pure black or non-Expressive brand bleeds in |

Agents 3–4 must match **fixed** Expressive hex (no Material You wallpaper dynamic color on Windows).

### 3.3 Logo in header

`draw_chrome` renders `app_logo.png` at 36×36 **plus** brand-colored product name in the top bar. Android mail workspace does not use a large app mark as persistent mail chrome; desktop should keep the asset for **window/taskbar icon** (`brand::window_icon`) and rethink in-header logo (wordmark-only, smaller mark, or destination-first chrome) so the first viewport is mailbox IA, not marketing chrome.

### 3.4 Categories stacked in the conversation pane

In `paint_columns` detail column:

```text
conversation::show_conversation(...)
categories::show_category_groups(...)   // same scroll stack
```

On Android, Categories is a **primary destination**, not an appendix under thread body. Desktop sidebar is mailbox-only by design (`NavDestination` × 6; comment: companies never in rail). Categories must move to a dedicated host (e.g. sidebar section, primary destination, or inspector) — not below the selected message.

### 3.5 Shake-related lessons (do not regress)

Already mitigated in code; redesign **must preserve**:

| Anti-pattern | Mitigation today |
|--------------|------------------|
| `apply_egui_visuals` every frame | Only when `theme.mode != applied_theme` |
| Animating `SidePanel` width (settings) | Fixed `SETTINGS_WIDTH`; instant open/close; `settings_progress` snap |
| Scroll-hide thrashing `TopBottomPanel` | `bars_visible = true` always; `on_list_scroll` no-op |
| Per-frame layout mutation | Comment in `update`: no progressive width animation |

Restoring 350ms settings slide / 250ms scroll-hide is allowed **only** if it does not change panel presence/width every frame (e.g. opacity/offset inside a fixed panel, or content-shift without remount). Prefer motion tiers + `reduced_motion`.

---

## 4. Functionality that must be preserved

These behaviors are demo/product contracts for the redesign; do not delete stubs.

| Capability | Where | Preserve |
|------------|-------|----------|
| **Demo mail** | `app_state::sample_messages`, list filter by destination/unified/search | Multi-account demo rows; select → detail; star toggle; priority explanation display; no Action Required chip in list |
| **Mailbox destinations** | Sidebar `NavDestination::ALL` | All Inbox, Primary, Promotional, Social, Spam, Starred; in-place content swap (no page reload) |
| **Search overlay** | `OverlayPane::Search` + `ui/search.rs` | Text → structured `SearchQuery`; debounce; FTS preview via core; UI never builds SQL |
| **Settings pane** | `ui/settings.rs` | Theme Light/Dark/HC; reduce motion; entry to Import + Privacy; inspector checkbox; close overlay |
| **Privacy Center** | `ui/privacy_center.rs` | Tabbed model from `desktop_core::privacy`; local purge request flag; copy about no remote image fetch / sanitized HTML |
| **Import overlay** | `ui/import_flow.rs` | Honest progress via bridge snapshot; **cap 99% until Done**; Continue enabled only when done; Not now dismiss; import dark palette independent of app theme |
| **Account switcher** | Chrome avatar → `OverlayPane::AccountSwitcher` | Switch `active_account_idx` among demo accounts |
| **Load states** | `WorkspaceLoadState` | Launching/Loading/Ready/Empty/Offline/AuthRequired/Error UI paths remain reachable |
| **Core bridge contracts** | `bridge.rs` consumers | Sanitize HTML; conversation assess; temporal extract; demo category groups; sync snapshot demo — no SQL/secrets in UI |
| **Interaction / a11y hooks** | `shell` + `interaction` / `capability_profile` | Shortcuts wiring stays; motion tier respects reduced motion / capability |

Optional polish (parity audit) may **extend** settings rows / import logos / category expand — without removing the above entry points.

---

## 5. Constraints

| Constraint | Rule |
|------------|------|
| **No `android/` edits** | Visual parity is copy-from-audit / read-only Android references. Never modify Compose sources in this pass. |
| **Preserve `desktop-core` business logic** | Priority, privacy purge semantics, search FTS building, sync honesty, category models, conversation assessment stay in core. Windows UI presents; bridge only. |
| **No WinUI rewrite this pass** | egui host stays; packaging docs may mention WinUI as future path only. |
| **No push / no drive-by commits** | Architecture doc only for Agent 2; Agents 3–4 land code separately. |
| **Companies ≠ sidebar destinations** | Company filters stay inside categories (existing sidebar copy + nav design). |
| **Privacy / import honesty** | Never fetch remote images for mail body; never treat conduit pulse as progress; never reverse import %. |

---

## 6. Architecture snapshot (as audited)

```text
main.rs
  └─ shell::run
       ├─ brand (icon + logo texture)
       ├─ tokens::apply_egui_visuals (mode-gated)
       ├─ TopBottomPanel  — chrome (logo, title, dest chip, Search/Settings, avatar)
       ├─ SidePanel left  — mailbox destinations
       ├─ SidePanel right — settings | privacy (overlays)
       ├─ CentralPanel    — list | detail(+categories BUG)
       ├─ Area/Window     — search, account switcher, import
       └─ CoreBridge      — desktop-core façade
```

**State:** `AppState` (nav, layout, theme, accounts, messages, search, import, privacy).  
**Geometry:** `WorkspaceLayout` (window size, list width, inspector flag) — needs central-only allocation fix.

---

## 7. Handoff checklist for Agents 3–4

- [ ] Agent 3: fix phantom sidebar empty space; decide logo-in-chrome policy; extend tokens without per-frame visual thrash.  
- [ ] Agent 4: relocate categories; keep demo mail / search / settings / privacy / import working.  
- [ ] Both: no `android/` or `desktop-core` logic rewrites; egui only.  
- [ ] Verify: theme toggle once → no continuous shake; settings open/close → no list reflow jitter.

---

## Sources (read-only)

- `Windows/src/shell.rs`, `app_state.rs`, `tokens.rs`, `navigation.rs`, `workspace.rs`, `brand.rs`, `main.rs`
- `Windows/src/ui/{mail_list,conversation,categories,settings,privacy_center,search,import_flow}.rs`
- `Windows/docs/ui-android-parity-audit.md`, `Windows/CONTEXT.md`, `Windows/docs/phase-03-shell.md`
- `Windows/Cargo.toml` (`eframe`/`egui` 0.29)
