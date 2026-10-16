package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized shape tokens — "Mailstack Expressive" (2026-10-16 redesign).
 *
 * Material 3 Expressive radii: chunkier than the old 8/12/16/20 scale —
 * cards land at 28dp, buttons and chips are pills, FABs are 24dp. Use
 * [MoShapes] via MaterialTheme; reach for [MoRadii] only when a component
 * needs an explicit radius value.
 */
object MoRadii {
    val small: Dp = 12.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 28.dp

    /** Pill shape for buttons, chips, nav indicators, account pills. */
    val pill: Shape = CircleShape
}

val MoShapes = Shapes(
    extraSmall = RoundedCornerShape(MoRadii.small),
    small = RoundedCornerShape(MoRadii.small),
    medium = RoundedCornerShape(MoRadii.medium),
    large = RoundedCornerShape(MoRadii.large),
    extraLarge = RoundedCornerShape(MoRadii.extraLarge),
)
