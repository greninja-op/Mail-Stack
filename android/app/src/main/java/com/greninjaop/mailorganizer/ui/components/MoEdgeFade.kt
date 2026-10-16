package com.greninjaop.mailorganizer.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Subtle fading edges for a scrolling list (2026-10-16): content
 * dissolves gently at the top and bottom edges as it scrolls, instead
 * of hard-clipping against the background (which read as a black bar
 * cutting the rows off). The fade is short (28dp) and light — rows stay
 * fully readable until the last few dp, and the fading contrast stays
 * small by design.
 *
 * Each edge fades only when there is more content to scroll to in that
 * direction, so a short list that cannot scroll shows no fade at all,
 * and the fade appears/disappears as the user scrolls.
 */
fun Modifier.edgeFade(
    top: Boolean,
    bottom: Boolean,
    fadeHeight: Dp = 28.dp,
): Modifier {
    if (!top && !bottom) return this
    return this.then(
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fade = fadeHeight.toPx().coerceIn(0f, size.height / 2f)
                val fraction = if (size.height > 0f) fade / size.height else 0f
                val stops = buildList {
                    if (top) {
                        add(0f to Color.Transparent)
                        add(fraction to Color.Black)
                    } else {
                        add(0f to Color.Black)
                    }
                    if (bottom) {
                        add((1f - fraction) to Color.Black)
                        add(1f to Color.Transparent)
                    } else {
                        add(1f to Color.Black)
                    }
                }
                drawRect(
                    brush = Brush.verticalGradient(*stops.toTypedArray()),
                    blendMode = BlendMode.DstIn,
                )
            },
    )
}
