package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

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
}
