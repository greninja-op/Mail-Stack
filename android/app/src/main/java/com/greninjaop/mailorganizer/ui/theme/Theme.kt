package com.greninjaop.mailorganizer.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * App theme — "Mailstack Expressive" (2026-10-16 redesign).
 *
 * Material You dynamic color (2026-10-16, user request): on Android 12+
 * the palette is derived from the wallpaper, like Pixel UI — change the
 * wallpaper and the app's primary/surface colors follow, in both light
 * and dark mode. Below Android 12 the fixed violet palettes (Color.kt)
 * are the fallback. This reverses the 1.0.14 decision to keep dynamic
 * color off: the user explicitly asked for wallpaper adaptation.
 *
 * ThemeMode.SYSTEM follows the system: system-dark → dynamic dark
 * scheme, system-light → dynamic light scheme.
 *
 * The window background is synced to the active scheme's background
 * (see [SyncWindowBackground]): the platform theme's default window
 * background is light, and during any transition gap it used to show
 * through as a white flash — the 2026-10-12 Connect-screen incident and
 * the tab-switch flicker were both this same cause.
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
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MoDarkColorScheme
        else -> MoLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoTypography,
        shapes = MoShapes,
    ) {
        SyncWindowBackground(background = colorScheme.background)
        content()
    }
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

/**
 * Paints the Activity window with the active scheme's background so the
 * platform default (light) can never show through as a white flash
 * during transitions or before the first frame. No-op in previews/tests
 * where the context is not an Activity.
 */
@Composable
private fun SyncWindowBackground(background: androidx.compose.ui.graphics.Color) {
    val activity = LocalContext.current as? Activity
    SideEffect {
        activity?.window?.setBackgroundDrawable(ColorDrawable(background.toArgb()))
    }
}

/** True when the active scheme is dark (drives mode-dependent colors). */
@Composable
fun isMoDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f
