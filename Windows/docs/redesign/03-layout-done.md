# Agent 2 — Desktop shell Design A layout (done)

Design A structure on Design B / `ThemeTokens` colours (Agent 4 owns hex). Canvas is plain `theme.bg` — no wallpaper.

## Delivered

1. **Canvas:** `CentralPanel` + chrome/sidebar fill `theme.bg` (pastel/cool solid; no photo BG).
2. **Top bar (48px):** compact search pill + Refresh + Settings + avatar. **No** large content logo.
3. **Sidebar:** compact brand mark (`brand::upload_logo_texture`, 28px) + small wordmark; prominent **+ Compose** primary → `OverlayPane::Compose` → `compose::show_compose_window`; nav **Inbox / Starred / Spam** + **Categories**; selected pill uses `primary_container`.
4. **Central:** two rounded surface panels (list | reader) — `surface_container_lowest`, ~18px radius, 14px gap, soft hairline. Not flat full-bleed only.
5. **List panel:** Primary / Promotional / Social category tabs (inbox family) + `mail_list` host (Agent 3 owns row chrome).
6. **Reader panel:** `conversation` only (no categories dump).
7. **Splitter:** drag in panel gap updates `list_width`. Window width **< ~1100** collapses central area to list XOR reader (Back to list).
8. **Compose:** wired to existing `ui/compose.rs` (Agent 4).
9. **Stability:** bars always mounted; `apply_egui_visuals` only on theme mode change.
10. **`cargo check -p mailstack-windows`** — passes.

## Destinations

| Location | Items |
|----------|--------|
| Sidebar rail | Inbox (`AllInbox` family), Starred, Spam, Categories surface |
| List tabs | Primary, Promotional, Social |
| Full set retained | `NavDestination::ALL` for filters / shortcuts |

## Files touched

- `Windows/src/shell.rs`
- `Windows/src/navigation.rs`
- `Windows/src/workspace.rs`
- `Windows/src/brand.rs`
- `Windows/docs/redesign/03-layout-done.md`

Left for other agents: `tokens.rs` (Agent 4), deep `mail_list` / `conversation` row chrome (Agent 3). No `android/` edits. No push.
