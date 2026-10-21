# Design A + B Windows redesign — implementation map

## Reference hierarchy

| Priority | Source | Owns |
|----------|--------|------|
| 1 | **Design A** (Gmail-desktop mock) | Layout: 3 panes, compose CTA, category tabs, list toolbar, reading toolbar, rounded white panels on soft blue canvas |
| 2 | **Design B** (pastel blue / white / mint) | Colour identity: primary blue `#4285F4` / pastel `#8AB4F8`, mint accents, `#F7F9FC` canvas, white surfaces |
| 3 | **Android** (`Color.kt` patterns, Type/Spacing/Motion) | Type scale, 4dp spacing, radii habits, motion tiers, reduced-motion, semantic status roles |
| 4 | **Current Windows egui** | Framework, demo mail, search, settings, privacy, import, desktop-core bridge |

**Policy change from prior violet pass:** Design B **wins** for brand colours on Windows desktop. Android violet is documented but not applied as shell primary. Typography/spacing/motion still follow Android scales.

## Framework

Keep **egui / eframe** (`Windows/`). No WinUI rewrite this pass.

## File ownership

| Owner | Files |
|-------|-------|
| Agent 1 (audit) | `Windows/docs/redesign/11-design-ab-contract.md` only |
| Agent 2 (shell) | `shell.rs`, `navigation.rs`, `workspace.rs`, `brand.rs` (toolbar/sidebar only) |
| Agent 3 (inbox/reader) | `ui/mail_list.rs`, `ui/conversation.rs`, `ui/categories.rs` |
| Agent 4 (theme/compose) | `tokens.rs`, `ui/settings.rs`, `ui/compose.rs` (new), `ui/mod.rs`, `app_state.rs` compose fields |
| Agent 5 (QA) | build/test/relaunch + `Windows/docs/redesign/12-qa-ab.md` |

Shared contract: `ThemeTokens` API stays (`bg`, `surface`, `brand`, `primary_container`, `tertiary`, `row_hover`, `row_selected`, …). Agent 4 may rename hex constants; Agents 2–3 must not hard-code colours.

## Destinations (existing capability only)

Sidebar: **Compose**, Inbox (`AllInbox`), Starred, Spam, Categories surface.  
Category **tabs** in list pane: Primary, Promotional, Social (existing filters).  
Do **not** invent Snoozed/Sent/Drafts/Trash without data.

## Compose

New overlay `OverlayPane::Compose` — To/Subject/Body + Send/Discard. Send = local toast/log feedback (no live SMTP); discard clears draft fields. Preserves honest non-mock UX.

## Backgrounds

Plain `#F7F9FC` / dark `#151A22` canvas. No wallpaper behind list/reader. Logo = OS icon + compact sidebar mark only.
