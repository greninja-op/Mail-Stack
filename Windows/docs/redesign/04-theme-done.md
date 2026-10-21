# Theme + motion — done (Design B)

## Tokens

`Windows/src/tokens.rs` ships **Design B** pastel blue / mint (not Android violet):

| Role | Light | Dark |
|------|-------|------|
| bg | `#F7F9FC` | `#151A22` |
| surface | `#FFFFFF` | `#1D2632` |
| elevated | white / low | `#242F3D` |
| brand | `#4285F4` | `#8AB4F8` |
| pastel / secondary | `#8AB4F8` | — |
| primary_container | `#DCEBFF` | `#2B405A` |
| tertiary (mint) | `#34C759` / container `#A8E6CF` | `#83DCC0` |
| text | `#172B4D` / `#64748B` | `#F1F5FA` / `#A7B4C5` |
| divider | `#E3EAF2` | `#344150` |
| row_hover / selected | `#F0F6FF` / `#DCEBFF` | `#2B405A` |

`ThemeTokens` field names unchanged for Agents 2–3 (`bg`, `surface`, `brand`, `primary_container`, `tertiary`, `row_hover`, `row_selected`, `text_*`, `divider`, status colours).

- Default `theme_mode`: **Light**
- `apply_egui_visuals` only on theme **mode** change
- Settings theme chips use `primary_container` / `row_hover` (Design B fills)

## Compose

`ui/compose.rs` + `OverlayPane::Compose`: To / Subject / Body, Send (local log + clear + status), Discard. No SMTP.

## Backgrounds

No wallpaper PNGs → solid Design B canvas only. See `00-conflict-and-assets.md` / `10-implementation-map.md`.

## Motion

- Settings panel: fixed width (no per-frame width animation / shake)
- Bars always mounted
- Hover/selected fills on sidebar pills and mail rows
- `reduced_motion` respected via `MotionTier::Reduced`
