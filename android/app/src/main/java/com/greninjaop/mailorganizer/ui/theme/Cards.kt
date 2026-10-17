package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * "Mailstack Expressive" card system (2026-10-16 redesign).
 *
 * Replaces the Breeze glass cards: [MoCard] is a solid tonal container
 * (surfaceContainer) with a 28dp expressive radius — no translucency, no
 * border, no shadow. Hierarchy comes from the tonal steps between
 * background → surfaceContainer → surfaceContainerHigh, never from
 * borders or elevation.
 *
 * Mail rows are NOT cards in this system — they are flat rows straight on
 * the background (see ThreadRow), like Gmail. Cards are for genuinely
 * grouped content: Home summaries, Settings sections, opened messages,
 * rule/automation entries, and similar.
 */

/** Corner radius for expressive cards. */
val MoCardShape: Shape = RoundedCornerShape(28.dp)

@Composable
fun MoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MoCardShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.then(
            // Clip BEFORE clickable (2026-10-17 fix): the clickable's
            // ripple is drawn by the Surface's own modifier, so the
            // Surface's shape does NOT clip it — the ripple drew as a
            // square over the rounded corners (visible on the "Needs
            // attention" card). clip(shape) first makes the ripple
            // follow the rounded shape.
            if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier,
        ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

/** Solid amber warning colors for priority badges and filter chips. */
data class MoWarningColors(
    val container: Color,
    val content: Color,
)

private val LightWarningColors = MoWarningColors(
    container = Color(0xFFFFEFC4),
    content = Color(0xFF7A4A00),
)

private val DarkWarningColors = MoWarningColors(
    container = Color(0xFF4A3200),
    content = Color(0xFFFFD88A),
)

@Composable
fun rememberMoWarningColors(): MoWarningColors {
    val dark = isMoDarkTheme()
    return remember(dark) { if (dark) DarkWarningColors else LightWarningColors }
}
