package com.greninjaop.mailorganizer.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.greninjaop.mailorganizer.R
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import kotlinx.coroutines.delay

/**
 * Cold-launch splash (2026-10-14).
 *
 * The user's final splash card, full-screen: light card in light mode,
 * dark card in dark mode. One plain fade — fade in, brief hold, fade out —
 * no slides, no gimmicks. Total ~1.55s: long enough to be seen, short
 * enough never to feel like waiting.
 *
 * The card artwork is pre-extended top/bottom (mirror extension, no
 * stretch) to 9:24, so [ContentScale.Crop] covers any phone without ever
 * distorting the logo/text. The app content is already composed (and
 * loading) underneath; the splash simply overlays it, then fades away.
 *
 * [themeMode] is the stored preference, or null while it is still
 * loading. The animation does not start until the stored mode is known,
 * so the splash can never flash the wrong card — while waiting it holds
 * at alpha 0 over a solid background.
 *
 * Fade structure (fixed 2026-10-14): the fade-in animates only the
 * artwork alpha, over the opaque solid placeholder — so the card fades
 * in from a clean background. The fade-out animates the WHOLE splash
 * (background + artwork) via [containerAlpha], dissolving into the app
 * underneath. Fading only the artwork on the way out would dissolve
 * the card into flat solid colour and then hard-cut to the app.
 */
@Composable
fun SplashScreen(
    themeMode: ThemeMode?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemDark = isSystemInDarkTheme()
    // Provisional while loading: the system value. Invisible at alpha 0,
    // replaced by the stored mode before the animation starts.
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM, null -> systemDark
    }
    val imageAlpha = remember { Animatable(0f) }
    // Whole-splash alpha, used only for the fade-out (see doc comment).
    val containerAlpha = remember { Animatable(1f) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(themeMode) {
        if (themeMode != null && !started) {
            started = true
            imageAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(FADE_IN_MS, easing = FastOutSlowInEasing),
            )
            delay(HOLD_MS.toLong())
            // Dissolve the whole splash into the app underneath.
            containerAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(FADE_OUT_MS, easing = FastOutLinearInEasing),
            )
            onDone()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = containerAlpha.value }
            .background(if (dark) Color(0xFF05070F) else Color.White),
    ) {
        Image(
            painter = painterResource(
                if (dark) R.drawable.splash_dark else R.drawable.splash_light,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = imageAlpha.value,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val FADE_IN_MS = 450
private const val HOLD_MS = 650
private const val FADE_OUT_MS = 450
