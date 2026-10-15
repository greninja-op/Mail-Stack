package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.R

/**
 * "Breeze" glassmorphism design system (2026-10-13, user-directed redesign).
 *
 * DELIBERATE DEPARTURE from the standing strict-Material-3 rule: the user
 * explicitly asked for glassmorphism across the whole app — translucent
 * cards with borders and soft shadows over the user-supplied Breeze
 * background artwork, frosted-glass search bar and bottom navigation.
 * Applied app-wide (every destination), not just the mail tab.
 *
 * The background artwork lives in drawable-nodpi (exact user-supplied
 * bytes); base64 backups are in `assets/backgrounds/` at the repo root.
 *
 * Dark/light is read from the active scheme's background luminance, so it
 * follows the user's theme-mode setting (Light / Dark / System) and works
 * with both dynamic color and the fallback palettes.
 */

/** True when the active scheme is dark (drives background + glass colors). */
@Composable
fun isBreezeDark(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Full-screen Breeze background artwork. Drawn once behind the NavHost. */
@Composable
fun BreezeBackground(modifier: Modifier = Modifier) {
    val dark = isBreezeDark()
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(
                if (dark) R.drawable.breeze_background_dark
                else R.drawable.breeze_background_light,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Glass palette — one set per mode. Cards/bars are translucent so the
 * Breeze background stays visible; borders + shadows carry the edges.
 */
data class GlassPalette(
    val card: Color,
    val cardBorder: Color,
    val bar: Color,
    val barBorder: Color,
    val drawer: Color,
    val warningContainer: Color,
    val warningContent: Color,
    val warningBorder: Color,
)

/** Light mode: translucent white over the pastel Breeze artwork. */
private val LightGlassPalette = GlassPalette(
    card = Color.White.copy(alpha = 0.62f),
    cardBorder = Color.White.copy(alpha = 0.75f),
    bar = Color.White.copy(alpha = 0.72f),
    barBorder = Color.White.copy(alpha = 0.6f),
    drawer = Color.White.copy(alpha = 0.85f),
    warningContainer = Color(0xFFFFF3D6).copy(alpha = 0.85f),
    warningContent = Color(0xFF8A5A00),
    warningBorder = Color(0xFFE8A100).copy(alpha = 0.45f),
)

/** Dark mode: translucent dark navy over the deep-navy Breeze artwork. */
private val DarkGlassPalette = GlassPalette(
    card = Color(0xFF1B2B4A).copy(alpha = 0.62f),
    cardBorder = Color.White.copy(alpha = 0.14f),
    bar = Color(0xFF16233D).copy(alpha = 0.72f),
    barBorder = Color.White.copy(alpha = 0.12f),
    drawer = Color(0xFF141F36).copy(alpha = 0.88f),
    warningContainer = Color(0xFF3A2A10).copy(alpha = 0.55f),
    warningContent = Color(0xFFFFC95C),
    warningBorder = Color(0xFFE8A100).copy(alpha = 0.45f),
)

@Composable
fun rememberGlassPalette(): GlassPalette {
    val dark = isBreezeDark()
    return remember(dark) { if (dark) DarkGlassPalette else LightGlassPalette }
}

/** Corner radius for glass cards (reference: softly rounded, ~24dp). */
val GlassCardShape: Shape = RoundedCornerShape(24.dp)

/**
 * A translucent glass card: tinted surface + 1dp border + soft shadow.
 * Screens pass their row content as [content]; [onClick] keeps the row's
 * tap behaviour (and ripple) on the card itself.
 *
 * Rendering (2026-10-15): explicit layers — shadow, fill, border — with
 * the SAME values as before (same fill, same 1dp border, same 24dp shape,
 * same 6dp shadow). The previous `Surface` implementation left the
 * content color unspecified for these custom translucent fills, so text
 * without an explicit color fell back to black — unreadable on the dark
 * card — and the combined Surface compositing produced bright stripe/line
 * artifacts inside cards on-device. The explicit [LocalContentColor]
 * below (theme `onSurface`) fixes the text; the explicit layers remove
 * the Surface compositing path. The look is unchanged.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = GlassCardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = rememberGlassPalette()
    Box(
        modifier = modifier
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(palette.card, shape)
            .border(BorderStroke(1.dp, palette.cardBorder), shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
            Column(content = content)
        }
    }
}
