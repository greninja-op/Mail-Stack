# Mail Organizer Android Tablet — Visual & UX Contract

> **2026-10-18 update.** This contract now matches the CURRENT Mailstack UI as shipped on Android phone (1.0.39 + in-main fixes), adapted to the tablet large-screen workspace. The phone app is the reference implementation for the design philosophy: an editor reads this file (plus `editor-rules.md`) and can reproduce the UI without re-reading the phone code. Do not redesign from the pre-2026-10-18 plan docs — those predate the Expressive redesign, the logo pipeline, the category-group fixes, the scrollbar, and the import-flow rebuild. Where this file and an older phase file disagree, this file wins.

## Platform

This is the Android tablet visual contract only. Do not use it to implement iOS, iPadOS, macOS, Windows, or Linux layouts. Do not use it to re-derive the phone app.

## Core principle

**THE ANDROID TABLET IS NOT A LARGE PHONE** — but it is the same product. The tablet carries the phone's design philosophy (tokens, rows, cards, categories, import flow, motion) and adapts its *layout* to the viewport: more information visible simultaneously, persistent context, pointer/keyboard/mouse input. Never duplicate the phone UI and simply increase dimensions; never invent a second design language.

## Design direction

The experience feels immediately familiar to a Gmail user without cloning Google's proprietary artwork, source code, or branding. Use Jetpack Compose Material 3 / Material 3 Expressive, Android large-screen guidance, and familiar interaction patterns, then add Mail Organizer's own identity through restrained branding, organization-first information architecture, and email intelligence.

Never AI-generate functional UI artwork or icons. Use official Material Symbols / Android system icons with the same semantics as the phone app.

## Brand & color

- App name: **Mailstack** everywhere user-facing.
- Brand violet: `#5238E8` (light) / `#B9A8FF` (dark).
- **Dark mode is built on dark gray, NEVER pure black:** deep violet-tinted charcoal `#131120` background, surfaces one step lighter, near-white primary text, soft-gray secondary text on solid surfaces. Light and dark mirror each other. Material You dynamic color is ON (Android 12+ derives the palette from wallpaper; below 12, violet fallback).
- Semantic category accents (from centralized tokens): Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. The accent colors the group icon and tints its leading circle.
- No gradients merely because they are fashionable. No photo backgrounds, no glass UI, no splash animation. Product identity stays restrained and consistent.

## Typography

Same roles and sizes as the phone app (sp): display 64/57, title 22/17/16, body 16/14, label 14/12. Support large fonts / accessibility sizes; layout must not break at the largest sizes. Tablet density comes from layout (panes, columns), not from shrinking type.

## Layout progression (content-driven, not device-name checks)

Adapt through available width using WindowSizeClass / adaptive APIs:
- narrow: navigation → list → detail (phone-like navigation; modal drawer covering the top section)
- medium: navigation rail/sidebar + list, detail contextually
- wide: navigation + list + persistent detail
- very wide: navigation + list + detail + optional contextual inspector/actions

These are layout states, not fixed device categories. No phase is complete if it works only at one resolution. Test portrait, landscape, narrow split, medium width, full-screen tablet, and multi-window.

## Layout primitives

- **Mail rows are flat rows straight on the background** — no card, no border, no shadow — like Gmail. This holds in every pane.
- **Cards are reserved for grouped content** (category groups, settings groups): solid tonal fill, 28dp corner radius, and the tap highlight is clipped to the rounded shape (`clip(shape)` BEFORE `clickable` — never a square highlight over a curved edge).
- Centralize spacing, typography, colors, shape, elevation/material treatment, iconography, and motion constants. Do not scatter magic values across composables. Every screen uses the same tokens; do a consistency pass across all previously implemented screens before completing a UI phase.

## Mail row (the core row — identical on every list, every pane)

- 40dp avatar, sender + time on line 1, subject on line 2, snippet on line 3. Minimum 88dp height.
- **Company logo if the sender is a company; letter avatar if the sender is a person.** The logo pipeline is identical everywhere a mail row appears: bundled logo → ICONS-ASSETS → sender's website favicon → letter fallback. Never fabricate a logo. A user-supplied logo is used as-is.
- Unified inbox: every row exposes the receiving Gmail account through a small circular account indicator. The indicator is the RECEIVING account, not the sender/company avatar.
- Star on the right for starred rows. Tapping star marks that email Starred; it stays in its original category and appears in the global Starred destination. Company pinning is independent from email starring.
- Unread = bold sender/subject + primary-colored time. No fabricated unread counts anywhere.
- Where width permits, the list pane may surface the same row's existing metadata (attachment indicator, priority badge) inline — never a second, divergent row design.

## Navigation

- Destinations: All Inbox, Primary, Promotional, Social, Spam, Starred.
- Narrow widths: modal drawer covering the top section like Gmail (not below a hoisted top bar). Compact, stable. Company grouping belongs INSIDE the selected category, never as drawer destinations.
- Medium+ widths: persistent navigation rail (icons + labels) or sidebar. Same destinations, same order, same semantics as the drawer.
- Account switching must be obvious and must never imply sender identity is the receiving account. Top-bar profile circle (DP) is uniform 40dp on every tab and is the account-switcher entry point.
- **Account switcher (Gmail-style):** current account, other accounts, Add another account, Settings. No unread counts on rows (no real source). Tapping an account switches the whole app, preserving mailbox context safely, never mixing account-owned data. On tablet it may be an anchored dialog/panel rather than a bottom sheet; the rows and rules are identical.
- **Tab switches never reload the page.** Content slides in from the respective side; no flicker, no reload. Pane selection is preserved across tab switches and configuration changes.
- **Settings:** non-important entries are NOT buttons in the Settings list — they go to the very bottom as small text (Gmail-style footer: "Privacy Policy • Terms of Service" + a centred About paragraph) or a paragraph about that thing. On narrow widths Settings slides in from the right (350ms, underlying screen slides 1/12 left); on wide widths it opens as a detail pane with the same slide-in motion and the same content.
- **Scroll-hide bars:** scroll DOWN hides bars, scroll UP shows them; pure translate, 250ms; entering a tab always shows bars.

## Categories

- Groups are **single-cavity cards**: header + members inside ONE rounded card (no gaps/seams). Member rows are full-width inside the card.
- **Group header is vertically centred:** icon, text block, and chevron all vertically centred (a horizontal line through the card centre passes through all three). Card hugs its content — symmetric vertical padding, no dead space at the bottom.
- Leading circle is tinted with the group's semantic accent (14% alpha); the icon itself is the full accent color.
- **Category detail rows use the same row layout and the same logo pipeline as the Mail list** — the row must receive the detected sender domain; a missing domain falls back to the letter avatar and is a bug. The detail row model must carry the raw sender email + display name so the domain can be detected.
- Company grouping/filtering occurs inside the selected category. Company pinning is separate from email starring and moves a pinned company to the top of that category's company filter. Pinning does not move mail, does not create a drawer destination, does not star messages.

## Import flow (Gmail → Mailstack)

- When a newly connected account needs non-trivial sync time, show the dedicated import experience — never a blank screen. On wide screens the import card is centred with a max width; it is never stretched edge-to-edge.
- Gmail mark on the left, Mailstack mark on the right. Real logos (user-supplied, as-is) — never hand-drawn/AI substitutes.
- **The conduit arc is NOT a progress bar:** a pulse travels the arc (left → right). **ONLY the Mailstack progress ring moves as the progress bar.**
- Progress comes from the REAL sync only and never moves backwards. The total is fetched once (Gmail profile `messagesTotal`) before the sync and never revised. Progress caps at 99% while running; Done = 100%.
- Flying mails ride the arc and stay visible (re-seed whenever the list empties while progress < 100%).
- Rows show the 2 most recently INSERTED mails. Continue is gated on Done, not merely 100%. Back on the first card = "Not now". The error variant keeps card chrome (Try again / Continue anyway). The card keeps its hardcoded dark palette in both themes.
- Never fake a percentage or pretend all mail has completed when it has not.

## Scrollbar

The Material scrollbar applies to every scrollable list pane: invisible until scroll; appears instantly on scroll (slide in from right + fade, 200ms FastOutSlowIn); after 1.5s static it slides back out to the right + fades (250ms LinearOutSlowIn); press-and-hold grows ONLY the thumb (4dp → 12dp, spring pop) — the 2dp track never grows; drag thumb = fast-scroll; press on the track jumps the thumb to the press first; no scrollbar when content fits. Pointer hover may reveal the scrollbar; hover must not change its behavior.

## Sync motion & honesty

- Determinate progress is shown only when a trustworthy denominator exists. Otherwise use an honest indeterminate state with meaningful stage text. Never fake percentages.
- The import animation is lightweight and truthful. On constrained conditions, simplify the moving elements and reduce effects while preserving meaning.
- Sync failures are classified honestly: per-second throttle (wait a minute, tap once) vs daily quota (resets at midnight Pacific, try tomorrow) vs permission vs offline. Never say "Reconnect" when the device is simply offline.

## Search, priority, action

- Search: structured query all the way down (UI never builds SQL), on-device index, 300ms debounce, real filters. Keyboard-friendly on tablet (focus, shortcuts, cancellation).
- Priority badges carry "why this priority?" explanations. Classification carries "Why this category?" explanations.
- **No "Action Required" chip/filter in the Mail list** (removed on phone). Action Required remains available as a destination/section where the plan defines it, but not as a Mail-list chip.

## Mail content rendering

- HTML is sanitized; entities are fully decoded (no raw `&bull;`, no zero-width-character walls); `<!DOCTYPE>`/processing instructions/CDATA never print as text.
- **Images inside mail bodies are NEVER fetched** (privacy: a fetch tells the sender the mail was opened). Blocked images render as nothing — no "Image not loaded" line per image.
- Message reveal: 350ms expand + fade, rotating chevron.
- Pull-to-refresh fires ONLY on a real finger drag — a fast fling back to the top must never trigger it. On tablet, mouse-wheel scroll must never trigger pull-to-refresh either.
- No JavaScript execution from email content. Safe links only.

## States

Every important screen needs intentional: loading, loaded, empty, offline, stale, syncing, authentication-required, permission-denied, error, and retry states as applicable. The error variant keeps the screen chrome (e.g. Try again / Continue anyway).

## Input

Support touch (first-class), stylus where genuinely useful, mouse/trackpad (hover, right-click context menus, wheel), keyboard focus/navigation/shortcuts, and useful drag & drop. Touch remains first-class. Multi-window / split-screen / resizing / configuration changes are supported; selection and scroll position survive them.

## Accessibility

TalkBack, large fonts, contrast, focus order, keyboard navigation, touch targets, and switch/accessibility interaction are first-class. Logical focus/order, labels for non-text controls, 48dp touch targets where applicable.

## Motion

Centralized motion system; every screen uses the same motion language:
- Essential: short, low-cost state changes.
- Standard: normal navigation/list interactions.
- Expressive: hero/sync/important transitions only — never every click.
- Reduced Motion: minimal fades/position changes.

Respect Reduce Motion and battery saver. No animation may block mail interaction or synchronization.

## Visual system

Centralize spacing, typography, colors, shape, elevation/material treatment, iconography, and motion constants. Do not scatter magic values across composables.

## Adaptive performance

Do not equate display refresh rate with sustainable frame rate. Build a lightweight capability profile (tiers, not device names) selecting animation complexity, prefetching, image cache limits, background concurrency, and sync batch sizes. Never claim 120 FPS because hardware supports 120 Hz. Reduce expensive work before the UI becomes janky. No continuous hardware benchmarking; re-evaluate when conditions change.

## Copy rules

- A supplied reference/screenshot/logo is the SPEC to copy exactly — "nothing more, nothing less."
- "Fix means remove": delete unwanted components, don't restyle them.
- When an element is removed, everything that depended on its position readjusts automatically, without being told.
- No app-created emojis anywhere. Only emojis inside real mail content render.
