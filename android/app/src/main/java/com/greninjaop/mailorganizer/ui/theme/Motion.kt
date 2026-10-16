package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * Shared motion specs (2026-10-13): one easing + duration family so every
 * animation in the app feels like the same app.
 *
 * Route changes use a Material fade-through (redone 2026-10-13): the
 * outgoing screen fades out FIRST (fast, 200ms) and the incoming screen
 * fades in AFTER a short delay (350ms) with a subtle 0.97 → 1.0 scale, so
 * the two screens never blend into a washed-out double image. The previous
 * simultaneous crossfade drew both screens at half opacity at once, which
 * read as a flash / refresh with colours appearing from nowhere.
 *
 * 2026-10-16: the fade-through now applies ONLY to pushed routes
 * (thread, settings, ...). Primary tab switches use no transition at
 * all (see AppNavGraph) — the fade gap used to expose the platform
 * window background as a white/gray flash and read as a reload.
 *
 * 2026-10-16 (tab motion): primary tab switches now use a directional
 * slide — the incoming tab's content enters from the side the tab sits
 * on (a tab to the right slides in from the right, and vice versa), so
 * the motion matches the jelly bubble's travel. The slide is opaque
 * (no fade, no scale): both screens stay fully opaque throughout, so
 * the old flash cannot return. Its 350ms FastOutSlowIn matches the
 * bubble's settle and the search-pill reshape, so all three land
 * together and the new tab is fully rendered when the bubble stops.
 */
object MoMotion {
    /** Outgoing screen: fade out, 200ms. */
    val fadeThroughExit = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)

    /** Incoming screen: fade in after a 120ms delay, 350ms. */
    val fadeThroughEnter = tween<Float>(
        durationMillis = 350,
        delayMillis = 120,
        easing = FastOutSlowInEasing,
    )

    /** Outgoing screen: scale 1.0 → 0.97, 200ms. */
    val fadeThroughScaleOut = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)

    /** Incoming screen: scale 0.97 → 1.0 after a 120ms delay, 350ms. */
    val fadeThroughScaleIn = tween<Float>(
        durationMillis = 350,
        delayMillis = 120,
        easing = FastOutSlowInEasing,
    )

    /**
     * Primary tab-switch slide: 350ms FastOutSlowIn. Fast start, gentle
     * landing — the same character as the jelly bubble, so the page
     * slide and the bubble settle together.
     */
    val tabSwitchSlide = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)

    /**
     * Settings panel slide (2026-10-16): the full-screen Settings
     * panel slides in from the right and out to the right, 350ms
     * FastOutSlowIn — the same spec as [tabSwitchSlide], so it lands
     * with the same character.
     */
    val panelSlide = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)

    /**
     * Search-pill reshape + hamburger slide: 350ms FastOutSlowIn, the
     * same spec as [tabSwitchSlide], so the top bar and the page land
     * together on every tab switch.
     */
    val topBarReshape = tween<androidx.compose.ui.unit.Dp>(
        durationMillis = 350,
        easing = FastOutSlowInEasing,
    )
}
