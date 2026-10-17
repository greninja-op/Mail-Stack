# macOS Visual & UX Contract

> **2026-10-18 update.** This contract now matches the CURRENT Mailstack UI as shipped on Android (1.0.39 + in-main fixes), adapted to macOS (Rust core + SwiftUI). The Android app is the reference implementation for the design philosophy: an editor reads this file (plus `editor-rules.md`) and can reproduce the UI without re-reading the Android code. Do not redesign from the pre-2026-10-18 plan docs — those predate the Expressive redesign, the logo pipeline, the category-group fixes, the scrollbar, and the import-flow rebuild. Where this file and an older phase file disagree, this file wins.

## Principle

macOS is a native desktop client, not a scaled mobile UI. **Rust owns product behavior; SwiftUI presents that behavior using native macOS conventions.** SwiftUI must consume Rust-owned models/use cases rather than reproduce business rules. UI state translation belongs at the boundary and must not create a second source of truth. The Rust core must stay reusable by Windows — no macOS-only assumptions in shared Rust modules.

## Design direction

The experience feels immediately familiar to a Gmail user without cloning Google's proprietary artwork, source code, or branding. The same UI philosophy as Android, rendered with SwiftUI and macOS conventions (multi-column workspace, menu bar, keyboard shortcuts, trackpad/mouse, multiple windows/scenes, SF Symbols for system glyphs with the same semantics as Android's Material Symbols).

Use native SwiftUI/macOS interaction patterns: adaptive split views, resizable windows, sidebar + list + detail, optional inspector, toolbar, menu commands, keyboard shortcuts, pointer/trackpad, context menus, selection semantics, multiple windows/scenes, VoiceOver and reduced motion. Never AI-generate functional UI artwork or icons.

## Brand & color

- App name: **Mailstack** everywhere user-facing.
- Brand violet: `#5238E8` (light) / `#B9A8FF` (dark).
- **Dark mode is built on dark gray, NEVER pure black:** deep violet-tinted charcoal `#131120` background, surfaces one step lighter, near-white primary text, soft-gray secondary text on solid surfaces. Light and dark mirror each other.
- Semantic category accents (from centralized tokens): Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. The accent colors the group icon and tints its leading circle.
- No gradients merely because they are fashionable. No photo backgrounds, no glass UI, no splash animation. Product identity stays restrained and consistent.

## Typography

Same roles and sizes as Android (sp → pt 1:1): display 64/57, title 22/17/16, body 16/14, label 14/12. Use SF Pro Text/Display. Support macOS accessibility text sizing; layout must not break at the largest sizes.

## Layout primitives

- **Mail rows are flat rows straight on the background** — no card, no border, no shadow — like Gmail.
- **Cards are reserved for grouped content** (category groups, settings groups): solid tonal fill, 28pt corner radius, and the tap highlight is clipped to the rounded shape (clip BEFORE the tap gesture — never a square highlight over a curved edge).
- Centralize spacing, typography, colors, shape, elevation/material treatment, iconography, and motion constants. Do not scatter magic values across views. Every screen uses the same tokens; do a consistency pass across all previously implemented screens before completing a UI phase.

## Mail row (the core row — identical on every list)

- 40pt avatar, sender + time on line 1, subject on line 2, snippet on line 3. Minimum 88pt height.
- **Company logo if the sender is a company; letter avatar if the sender is a person.** The logo pipeline is identical everywhere a mail row appears: bundled logo → asset catalog → sender's website favicon → letter fallback. Never fabricate a logo. A user-supplied logo is used as-is.
- Unified inbox: every row exposes the receiving Gmail account through a small circular account indicator. The indicator is the RECEIVING account, not the sender/company avatar.
- Star on the right for starred rows. Tapping star marks that email Starred; it stays in its original category and appears in the global Starred destination. Company pinning is independent from email starring.
- Unread = bold sender/subject + primary-colored time. No fabricated unread counts anywhere.

## Navigation (desktop)

- Multi-column workspace: sidebar + message list + detail + optional inspector. All Inbox, Primary, Promotional, Social, Spam, Starred are primary sidebar destinations. Companies are filters within the selected category — never top-level destinations.
- **Sidebar is stable and compact** (Gmail parity). It never reloads or flickers when switching destinations; selection is preserved across destination switches, window resizes, and restarts.
- **Account switching:** account menu in the toolbar + account section in the sidebar — current account, other accounts, Add another account, Settings. No unread counts on rows (no real source). Switching accounts preserves the mailbox context safely and never mixes account-owned data. The toolbar account avatar is uniform 40pt.
- **Settings** opens in its own window via the standard macOS Settings scene, with the same content structure: account section at top, options, then the Gmail-style footer. Non-important Settings entries are NOT buttons in the Settings list — they go to the very bottom as small text ("Privacy Policy • Terms of Service" + a centred About paragraph) or a paragraph about that thing.
- Menu bar commands mirror every primary action (including Refresh, Search, account switching). Keyboard shortcuts for all primary actions. Context menus on rows and sidebar items. Multiple windows/scenes with independent selection state and state restoration.

## Categories

- Groups are **single-cavity cards**: header + members inside ONE rounded card (no gaps/seams). Member rows are full-width inside the card.
- **Group header is vertically centred:** icon, text block, chevron all vertically centred (a horizontal line through the card centre passes through all three). Card hugs content — symmetric vertical padding, no dead space at the bottom.
- Leading circle is tinted with the group's semantic accent (14% alpha); the icon itself is the full accent color.
- **Category detail rows use the same row layout and the same logo pipeline as the Mail list** — the row must receive the detected sender domain; a missing domain falls back to the letter avatar and is a bug.
- Company grouping/filtering occurs inside the selected category. Company pinning is separate from email starring and moves a pinned company to the top of that category's company filter. Pinning does not move mail, does not create a sidebar destination, does not star messages.

## Import flow (Gmail → Mailstack)

- When a newly connected account needs non-trivial sync time, show the dedicated import experience — never a blank screen.
- Gmail mark on the left, Mailstack mark on the right. Real logos (user-supplied, as-is) — never hand-drawn/AI substitutes.
- **The conduit arc is NOT a progress bar:** a pulse travels the arc (left → right). **ONLY the Mailstack progress ring moves as the progress bar.**
- Progress comes from the REAL sync only and never moves backwards. The total is fetched once (Gmail profile `messagesTotal`) before the sync and never revised. Progress caps at 99% while running; Done = 100%.
- Flying mails ride the arc and stay visible (re-seed whenever the list empties while progress < 100%).
- Rows show the 2 most recently INSERTED mails. Continue is gated on Done, not merely 100%. Back on the first card = "Not now". The error variant keeps card chrome (Try again / Continue anyway). The card keeps its hardcoded dark palette in both themes.
- Never fake a percentage or pretend all mail has completed when it has not.

## Scrollbar

Use native macOS scroll indicators (overlay scrollbars). If a custom scrollbar is ever built, it follows the Android Material scrollbar behavior: invisible until scroll; appears instantly on scroll (slide in from right + fade, 200ms); after 1.5s static it slides back out + fades (250ms); press-and-hold grows ONLY the thumb (4pt → 12pt); drag = fast-scroll; press on track jumps the thumb to the press first; no scrollbar when content fits.

## Sync motion & honesty

- Determinate progress is shown only when a trustworthy denominator exists. Otherwise use an honest indeterminate state with meaningful stage text. Never fake percentages.
- The import animation is lightweight and truthful. On constrained conditions, simplify the moving elements and reduce effects while preserving meaning.
- Sync failures are classified honestly: per-second throttle (wait a minute, tap once) vs daily quota (resets at midnight Pacific, try tomorrow) vs permission vs offline. Never say "Reconnect" when the device is simply offline.

## Search, priority, action

- Search: structured query all the way down (UI never builds SQL), on-device index, real filters, keyboard-first (Cmd+K / Cmd+F entry points, Esc clears).
- Priority badges carry "why this priority?" explanations. Classification carries "Why this category?" explanations.
- **No "Action Required" chip/filter in the Mail list** (removed on Android). Action Required remains available as a destination/section where the plan defines it, but not as a Mail-list chip.

## Mail content rendering

- HTML is sanitized; entities are fully decoded (no raw `&bull;`, no zero-width-character walls); `<!DOCTYPE>`/processing instructions/CDATA never print as text.
- **Images inside mail bodies are NEVER fetched** (privacy: a fetch tells the sender the mail was opened). Blocked images render as nothing — no "Image not loaded" line per image.
- Message reveal: 350ms expand + fade, rotating chevron.
- Refresh is an explicit user action (toolbar button / Cmd+R) — never auto-fire on scroll or fling.
- No JavaScript execution from email content. Safe links only.

## States

Every important screen needs intentional: loading, loaded, empty, offline, stale, syncing, authentication-required, permission-denied, error, and retry states as applicable. The error variant keeps the screen chrome (e.g. Try again / Continue anyway).

## Accessibility

Support: VoiceOver, macOS accessibility text sizing, sufficient contrast, Reduce Motion, larger accessibility sizes, logical focus/order, labels for non-text controls, Full Keyboard Access, hit targets appropriate to pointer/trackpad interaction.

## Motion

Centralized motion system; every screen uses the same motion language:
- Essential: short, low-cost state changes.
- Standard: normal navigation/list interactions.
- Expressive: hero/sync/important transitions only — never every click.
- Reduced Motion: minimal fades/position changes.

Respect Reduce Motion and Low Power Mode. No animation may block mail interaction or synchronization.

## Visual system

Centralize typography, spacing, surfaces, semantic colors, separators, icons, motion and state tokens. Support light/dark mode, contrast and accessibility sizing. Prefer native SF Symbols/system controls for functional UI.

## Adaptive performance

Do not equate display refresh rate with sustainable frame rate. Build a lightweight capability profile (tiers, not device names) selecting animation complexity, prefetching, image cache limits, background concurrency, and sync batch sizes. Never claim 120 FPS because hardware supports 120 Hz. Reduce expensive work before the UI becomes janky. No continuous hardware benchmarking; re-evaluate when conditions change.

## Copy rules

- A supplied reference/screenshot/logo is the SPEC to copy exactly — "nothing more, nothing less."
- "Fix means remove": delete unwanted components, don't restyle them.
- When an element is removed, everything that depended on its position readjusts automatically, without being told.
- No app-created emojis anywhere. Only emojis inside real mail content render.

## Rust/Swift boundary

SwiftUI must consume Rust-owned models/use cases rather than reproduce business rules. UI state translation belongs at the boundary and must not create a second source of truth.

## Motion and QA

Sync motion must represent real sync state; never fabricate percentages. Validate multiple window sizes, light/dark mode, accessibility text, VoiceOver, keyboard-only navigation, pointer interaction, offline/error/loading states, selection restoration and large-mailbox performance.
