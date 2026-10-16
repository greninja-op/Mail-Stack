package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.luminance
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * App theme — "Mailstack Expressive" (2026-10-16 redesign).
 *
 * Fixed brand palettes in both modes (see Color.kt). Dynamic color is
 * deliberately OFF: it pulls the palette from the wallpaper, so the app
 * would look different on every phone and light/dark would never match —
 * the opposite of the uniformity this redesign is built on.
 * [themeMode] may be a plain value or a [Flow] collected by the caller —
 * the overloads keep call sites simple.
 */
@Composable
fun MailOrganizerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) MoDarkColorScheme else MoLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoTypography,
        shapes = MoShapes,
        content = content,
    )
}

@Composable
fun MailOrganizerTheme(
    themeMode: Flow<ThemeMode>,
    content: @Composable () -> Unit,
) {
    val mode: State<ThemeMode> = produceState(initialValue = ThemeMode.SYSTEM, themeMode) {
        themeMode.collect { value = it }
    }
    MailOrganizerTheme(themeMode = mode.value, content = content)
}

/** True when the active scheme is dark (drives mode-dependent colors). */
@Composable
fun isMoDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f
