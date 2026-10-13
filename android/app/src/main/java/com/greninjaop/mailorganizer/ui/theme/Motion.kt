package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Shared motion specs (2026-10-13): one easing + duration family so every
 * animation in the app feels like the same app. Route changes crossfade
 * with [crossfade]; in/out use the same timing so neither screen pops or
 * flashes — the outgoing screen is still visible while the incoming one
 * fades in over the theme background.
 */
object MoMotion {
    /** Screen-to-screen crossfade: Material's standard easing, 300ms. */
    val crossfade = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
}
