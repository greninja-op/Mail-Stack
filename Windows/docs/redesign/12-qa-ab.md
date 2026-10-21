# Design A + B QA report

**Date:** 2026-10-21  
**Tests:** `cargo test -p mailstack-windows --lib` → **46 passed**  
**App:** Mailstack.exe launched; visually verified light theme

## Verified (screenshot / interaction)

| Check | Result |
|-------|--------|
| Design B pastel blue / white canvas | Pass |
| + Compose CTA + compose overlay (To/Subject/Body/Send/Discard) | Pass |
| Sidebar Inbox / Starred / Spam / Categories | Pass |
| Single category tab strip Primary / Promotional / Social | Pass (duplicate strip removed) |
| Rounded white list + reader panels | Pass |
| Reading toolbar Archive/Spam/Delete/Read/Star + body | Pass |
| Empty reader state when nothing selected | Pass |
| No content-header logo | Pass |
| Default theme Light | Pass |

## Not claimed / remaining

- Dark theme: tokens implemented; this QA pass focused on light (Design B reference)
- Send is **local demo only** — no SMTP / Gmail send
- Live OAuth / sync still blocked on Console setup
- Design A extras without data (Snoozed/Sent/Drafts/Trash) intentionally omitted
- Photo wallpaper assets still absent — plain canvas used by policy
- High-DPI / narrow two-pane collapse: coded; not exhaustively resized in this pass
