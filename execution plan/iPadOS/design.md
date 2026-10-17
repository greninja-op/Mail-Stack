# Mail Organizer iPadOS — Visual & UX Contract

> **2026-10-18 update.** This contract now matches the CURRENT Mailstack UI as shipped on Android (1.0.39 + in-main fixes), adapted to the iPad workspace in SwiftUI. The Android app is the reference implementation for the design philosophy: an editor reads this file (plus `editor-rules.md`) and can reproduce the UI without re-reading the Android code. Do not redesign from the pre-2026-10-18 plan docs — those predate the Expressive redesign, the logo pipeline, the category-group fixes, the scrollbar, and the import-flow rebuild. Where this file and an older phase file disagree, this file wins.

## Platform

This is the iPadOS / iPad visual contract only. Do not use it to implement iPhone, Android tablet, macOS, Windows, or Linux layouts.

## Core principle

**THE IPAD IS NOT A LARGE IPHONE.** Use the viewport to expose more useful information simultaneously while preserving clarity and touchability. The iPad is a workspace: navigation context + list + detail + inspector can coexist. Same product identity, same tokens, same rules as iPhone/Android — expressed in a multi-column layout, not a stretched phone UI.

## Design direction

The experience feels immediately familiar to a Gmail user without cloning Google's proprietary artwork, source code, or branding. The same UI philosophy as Android/iPhone, rendered with SwiftUI and iPadOS conventions (NavigationSplitView, safe areas, Dynamic Type, SF Symbols for system glyphs with the same semantics as Android's Material Symbols).

Use native SwiftUI and Apple's platform conventions where they improve familiarity and accessibility. Shared product concepts remain consistent; layouts are genuinely adapted to iPad. Never AI-generate functional UI artwork or icons.

## Brand & color

- App name: **Mailstack** everywhere user-facing.
- Brand violet: `#5238E8` (light) / `#B9A8FF` (dark).
- **Dark mode is built on dark gray, NEVER pure black:** deep violet-tinted charcoal `#131120` background, surfaces one step lighter, near-white primary text, soft-gray secondary text on solid surfaces. Light and dark mirror each other.
- Semantic category accents (from centralized tokens): Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. The accent colors the group icon and tints its leading circle.
- No gradients merely because they are fashionable. No photo backgrounds, no glass UI, no splash animation. Product identity stays restrained and consistent. **Do not adopt Liquid Glass / glass materials as the design language** — the flat-row + tonal-card system is the identity. Public Apple visual materials may be used only where they do not change the design language, never as a redesign, never via private APIs. The import card's user-supplied frosted host is the only glass exception, carried over as-is.

## Typography

Same roles and sizes as Android/iPhone (sp → pt 1:1): display 64/57, title 22/17/16, body 16/14, label 14/12. Use SF Pro Text/Display. Support Dynamic Type including large accessibility sizes; layout must not break at the largest sizes. Wider columns may use the same type scale — density comes from columns and metadata, not smaller text.

## Layout — multi-column workspace

- At sufficient width prefer: **sidebar (navigation/categories) → message list + company/category context → selected message/conversation detail → optional contextual inspector/action region.**
- Columns appear/disappear according to ACTUAL available width, using content-driven thresholds — never device-name checks, never fixed-width assumptions.
- **Selection persists across columns:** picking a message in the list shows its detail without losing list context; resizing (including rapid resize during loading/sync/search/detail transitions) restores selection; navigating back preserves context.
- **Narrow widths** (Split View narrow, Slide Over, portrait on small iPads): fall back to iPhone-like navigation — a single column with a drawer/overlay sidebar, one column at a time. The fallback must behave exactly like the iPhone contract (same rows, same groups, same rules).
- **Windowing:** test portrait, landscape, narrow split, medium split, full screen, Slide Over, and Stage Manager window sizes. No layout may assume fixed width.
- **Bars:** scroll DOWN hides bars, scroll UP shows them; pure translate, 250ms; entering a tab/column always shows bars. Bars are overlaid, never padded/resized containers.

## Layout primitives

- **Mail rows are flat rows straight on the background** — no card, no border, no shadow — like Gmail.
- **Cards are reserved for grouped content** (category groups, settings groups, inspector groups): solid tonal fill, 28pt corner radius, and the tap highlight is clipped to the rounded shape (clip BEFORE the tap gesture — never a square highlight over a curved edge).
- Centralize spacing, typography, colors, shape, elevation/material treatment, iconography, and motion constants. Do not scatter magic values across views. Every screen uses the same tokens; do a consistency pass across all previously implemented screens before completing a UI phase.

## Mail row (the core row — identical on every list)

- 40pt avatar, sender + time on line 1, subject on line 2, snippet on line 3. Minimum 88pt height.
- **Company logo if the sender is a company; letter avatar if the sender is a person.** The logo pipeline is identical everywhere a mail row appears: bundled logo → asset catalog → sender's website favicon → letter fallback. Never fabricate a logo. A user-supplied logo is used as-is.
- Unified inbox: every row exposes the receiving Gmail account through a small circular account indicator. The indicator is the RECEIVING account, not the sender/company avatar.
- Star on the right for starred rows. Tapping star marks that email Starred; it stays in its original category and appears in the global Starred destination. Company pinning is independent from email starring.
- Unread = bold sender/subject + primary-colored time. No fabricated unread counts anywhere.
- **Density on iPad:** where the list column is wide, rows may ALSO show category, company, priority, attachment indicator, and conversation state inline — more useful context, not clutter. The core row (avatar/sender/subject/snippet/time/star) never changes shape; extra metadata is additive.

## Navigation

- Sidebar destinations: All Inbox, Primary, Promotional, Social, Spam, Starred. Persistent at sufficient width; collapsible; overlay/drawer at narrow widths.
- **Selection changes never reload the page.** Content updates in place; no flicker, no reload.
- Account switching must be obvious and must never imply sender identity is the receiving account. Profile circle (DP) is uniform 40pt and is the account-switcher entry point.
- **Account switcher (Gmail-style):** current account, other accounts, Add another account, Settings. No unread counts on rows (no real source). Tapping an account switches the whole app, preserving mailbox context safely, never mixing account-owned data.
- **Settings** opens as a panel/column (slide in from the right, 350ms, underlying content slides slightly left 1/12; closing reverses it) at narrow widths, or as a dedicated column/pane at wide widths. Either way: full-height, bars overlaid.
- **Non-important Settings entries are NOT buttons in the Settings list** — they go to the very bottom as small text (Gmail-style footer: "Privacy Policy • Terms of Service" + a centred About paragraph) or a paragraph about that thing.

## Categories & company workspace

- Groups are **single-cavity cards**: header + members inside ONE rounded card (no gaps/seams). Member rows are full-width inside the card.
- **Group header is vertically centred:** icon, text block, and chevron all vertically centred (a horizontal line through the card centre passes through all three). Card hugs its content — symmetric vertical padding, no dead space at the bottom.
- Leading circle is tinted with the group's semantic accent (14% alpha); the icon itself is the full accent color.
- **Category detail rows use the same row layout and the same logo pipeline as the Mail list** — the row must receive the detected sender domain; a missing domain falls back to the letter avatar and is a bug.
- Company grouping/filtering occurs inside the selected category. Company pinning is separate from email starring and moves a pinned company to the top of that category's company filter. Pinning does not move mail, does not create a sidebar destination, does not star messages.
- **Company workspace:** at wide widths the company filter/context area can sit alongside the list as its own column/region; at narrow widths it collapses into the category view. The company context never becomes a top-level destination.

## Inspector / contextual region

- At wide widths, an optional inspector column shows contextual actions for the selected message: action cards, classification/priority explanations ("Why this category?", "Why this priority?"), conversation intelligence (timeline + reasons), and temporal items (meetings/deadlines).
- The inspector is contextual, never a parallel mailbox. It disappears at narrow widths; its content remains reachable from the detail view.

## Import flow (Gmail → Mailstack)

- When a newly connected account needs non-trivial sync time, show the dedicated import experience — never a blank screen. On iPad it presents as a centred card in the workspace (same visual language as iPhone), not a full-screen takeover.
- Gmail mark on the left, Mailstack mark on the right. Real logos (user-supplied, as-is) — never hand-drawn/AI substitutes.
- **The conduit arc is NOT a progress bar:** a pulse travels the arc (left → right). **ONLY the Mailstack progress ring moves as the progress bar.**
- Progress comes from the REAL sync only and never moves backwards. The total is fetched once (Gmail profile `messagesTotal`) before the sync and never revised. Progress caps at 99% while running; Done = 100%.
- Flying mails ride the arc and stay visible (re-seed whenever the list empties while progress < 100%).
- Rows show the 2 most recently INSERTED mails. Continue is gated on Done, not merely 100%. Back on the first card = "Not now". The error variant keeps card chrome (Try again / Continue anyway). The card keeps its hardcoded dark palette in both themes.
- Never fake a percentage or pretend all mail has completed when it has not.

## Scrollbar

Use native iPadOS scroll indicators. If a custom scrollbar is ever built, it follows the Android Material scrollbar behavior: invisible until scroll; appears instantly on scroll (slide in from right + fade, 200ms); after 1.5s static it slides back out + fades (250ms); press-and-hold grows ONLY the thumb (4pt → 12pt); drag = fast-scroll; press on track jumps the thumb to the press first; no scrollbar when content fits. On iPad, pointer/trackpad hover may reveal the indicator without a scroll.

## Sync motion & honesty

- Determinate progress is shown only when a trustworthy denominator exists. Otherwise use an honest indeterminate state with meaningful stage text. Never fake percentages.
- The import animation is lightweight and truthful. On constrained conditions, simplify the moving elements and reduce effects while preserving meaning.
- Sync failures are classified honestly: per-second throttle (wait a minute, tap once) vs daily quota (resets at midnight Pacific, try tomorrow) vs permission vs offline. Never say "Reconnect" when the device is simply offline.

## Search, priority, action

- Search: structured query all the way down (UI never builds SQL), on-device index, 300ms-debounce equivalent, real filters. Keyboard-friendly: search field focusable, keyboard shortcuts, cancellation, pagination.
- Priority badges carry "why this priority?" explanations. Classification carries "Why this category?" explanations.
- **No "Action Required" chip/filter in the Mail list** (removed on Android). Action Required remains available as a destination/section where the plan defines it, but not as a Mail-list chip.

## Mail content rendering

- HTML is sanitized; entities are fully decoded (no raw `&bull;`, no zero-width-character walls); `<!DOCTYPE>`/processing instructions/CDATA never print as text.
- **Images inside mail bodies are NEVER fetched** (privacy: a fetch tells the sender the mail was opened). Blocked images render as nothing — no "Image not loaded" line per image.
- Message reveal: 350ms expand + fade, rotating chevron.
- Pull-to-refresh fires ONLY on a real finger drag — a fast fling back to the top must never trigger it.
- No JavaScript execution from email content. Safe links only.

## Input — pointer, keyboard, Pencil

- Support touch, pointer/trackpad hover, keyboard shortcuts, focus traversal, contextual menus, and drag & drop where genuinely useful.
- Hover states on rows/controls for pointer users; keyboard navigation reaches every interactive element; focus order is logical and visible.
- Apple Pencil is optional and never required. No feature may depend on Pencil.

## States

Every important screen needs intentional: loading, loaded, empty, offline, stale, syncing, authentication-required, permission-denied, error, and retry states as applicable. The error variant keeps the screen chrome (e.g. Try again / Continue anyway). States must hold across column changes and window resizes.

## Accessibility

Support: VoiceOver, Dynamic Type, sufficient contrast, Reduce Motion, larger accessibility sizes, logical focus/order, labels for non-text controls, hit targets appropriate to touch (44pt minimum), keyboard/pointer accessibility, focus stability across resizes.

## Motion

Centralized motion system; every screen uses the same motion language:
- Essential: short, low-cost state changes.
- Standard: normal navigation/list interactions.
- Expressive: hero/sync/important transitions only — never every click.
- Reduced Motion: minimal fades/position changes.

Respect Reduce Motion and Low Power Mode. No animation may block mail interaction or synchronization. Column appear/disappear animates; selection is preserved through it.

## Visual system

Centralize spacing, typography, colors, shape, elevation/material treatment, iconography, and motion constants. Do not scatter magic values across views.

## Adaptive performance

Do not equate display refresh rate with sustainable frame rate. Build a lightweight capability profile (tiers, not device names) selecting animation complexity, prefetching, image cache limits, background concurrency, and sync batch sizes. Never claim 120 FPS because hardware supports ProMotion/120 Hz. Reduce expensive work before the UI becomes janky. No continuous hardware benchmarking; re-evaluate when conditions change.

## Copy rules

- A supplied reference/screenshot/logo is the SPEC to copy exactly — "nothing more, nothing less."
- "Fix means remove": delete unwanted components, don't restyle them.
- When an element is removed, everything that depended on its position readjusts automatically, without being told.
- No app-created emojis anywhere. Only emojis inside real mail content render.
