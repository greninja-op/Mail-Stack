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
    val alpha = remember { Animatable(0f) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(themeMode) {
        if (themeMode != null && !started) {
            started = true
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(FADE_IN_MS, easing = FastOutSlowInEasing),
            )
            delay(HOLD_MS.toLong())
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(FADE_OUT_MS, easing = FastOutLinearInEasing),
            )
            onDone()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF05070F) else Color.White),
    ) {
        Image(
            painter = painterResource(
                if (dark) R.drawable.splash_dark else R.drawable.splash_light,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = alpha.value,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val FADE_IN_MS = 450
private const val HOLD_MS = 650
private const val FADE_OUT_MS = 450
