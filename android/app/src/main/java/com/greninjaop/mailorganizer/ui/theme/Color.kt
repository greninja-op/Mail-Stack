package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Centralized color tokens — "Mailstack Expressive" (2026-10-16 redesign).
 *
 * The Breeze glassmorphism system is gone: no photo background, no
 * translucent cards, no dynamic color. Every surface is a solid tonal
 * color from one fixed palette per mode, so light and dark are uniform
 * mirrors of each other and text contrast never depends on what sits
 * behind a card.
 *
 * Brand: vivid violet. Light mode pairs it with a pale violet-tinted
 * background; dark mode is built on deep violet-tinted charcoal — never
 * pure black — with bright lavender accents, so text stands out on solid
 * surfaces in both modes.
 *
 * Dynamic color is OFF (see Theme.kt): the palette comes from the user's
 * wallpaper otherwise, which breaks uniformity — the same app would look
 * different on every phone and in every mode.
 *
 * Every screen must use these tokens — no ad-hoc colors.
 */

// ---- Brand ----
private val BrandPrimary = Color(0xFF5238E8) // vivid violet
private val BrandPrimaryContainerLight = Color(0xFFE3DCFF)
private val OnBrandPrimaryContainerLight = Color(0xFF1D0F6E)
private val BrandSecondaryLight = Color(0xFF6A5ACD) // softer violet
private val BrandSecondaryContainerLight = Color(0xFFE4E0FF)
private val OnBrandSecondaryContainerLight = Color(0xFF241A5E)
private val BrandTertiaryLight = Color(0xFF1E7A55) // mint accent
private val BrandTertiaryContainerLight = Color(0xFFB9F5D6)
private val OnBrandTertiaryContainerLight = Color(0xFF002114)

// ---- Light surfaces: pale violet-tinted, never stark white-on-white ----
private val LightBackground = Color(0xFFF6F3FF)
private val LightSurface = Color(0xFFFDFBFF)
private val LightOnSurface = Color(0xFF1C1A26) // near-black violet
private val LightSurfaceVariant = Color(0xFFE7E1F5)
private val LightOnSurfaceVariant = Color(0xFF48455A)
private val LightSurfaceContainerLow = Color(0xFFF7F4FF)
private val LightSurfaceContainer = Color(0xFFEFEAFB)
private val LightSurfaceContainerHigh = Color(0xFFE7E0F6)
private val LightSurfaceContainerHighest = Color(0xFFDCD3F0)
private val LightOutline = Color(0xFF7B7790)
private val LightOutlineVariant = Color(0xFFCBC5DE)

// ---- Dark surfaces: deep violet-tinted charcoal, never pure black ----
// Background is the darkest tone; each container tier lifts visibly
// above it so hierarchy reads without borders or shadows.
private val DarkBackground = Color(0xFF131120)
private val DarkSurface = Color(0xFF1A1826)
private val DarkOnSurface = Color(0xFFF0EDFB) // near-white lavender
private val DarkSurfaceVariant = Color(0xFF2E2A44)
private val DarkOnSurfaceVariant = Color(0xFFCBC6DE)
private val DarkSurfaceContainerLow = Color(0xFF1F1C2E)
private val DarkSurfaceContainer = Color(0xFF262340)
private val DarkSurfaceContainerHigh = Color(0xFF322D4E)
private val DarkSurfaceContainerHighest = Color(0xFF3D3859)
private val DarkOutline = Color(0xFF948EA9)
private val DarkOutlineVariant = Color(0xFF48445C)

// ---- Dark brand: bright lavender accents that pop on dark surfaces ----
private val BrandPrimaryDark = Color(0xFFB9A8FF)
private val OnBrandPrimaryDark = Color(0xFF2E1B8A)
private val BrandPrimaryContainerDark = Color(0xFF4634A8)
private val BrandSecondaryDark = Color(0xFFC9BEFF)
private val OnBrandSecondaryDark = Color(0xFF382E78)
private val BrandSecondaryContainerDark = Color(0xFF4D4096)
private val BrandTertiaryDark = Color(0xFF7FE0B2)
private val OnBrandTertiaryDark = Color(0xFF003828)
private val BrandTertiaryContainerDark = Color(0xFF005741)

// ---- Errors ----
private val ErrorLight = Color(0xFFB3261E)
private val ErrorContainerLight = Color(0xFFF9DEDC)
private val OnErrorContainerLight = Color(0xFF410E0B)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

val MoLightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = OnBrandPrimaryContainerLight,
    secondary = BrandSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondaryContainerLight,
    onSecondaryContainer = OnBrandSecondaryContainerLight,
    tertiary = BrandTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = BrandTertiaryContainerLight,
    onTertiaryContainer = OnBrandTertiaryContainerLight,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = LightOnSurface,
    inverseOnSurface = LightSurface,
    inversePrimary = BrandPrimaryDark,
)

val MoDarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = OnBrandPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandPrimaryContainerLight,
    secondary = BrandSecondaryDark,
    onSecondary = OnBrandSecondaryDark,
    secondaryContainer = BrandSecondaryContainerDark,
    onSecondaryContainer = BrandSecondaryContainerLight,
    tertiary = BrandTertiaryDark,
    onTertiary = OnBrandTertiaryDark,
    tertiaryContainer = BrandTertiaryContainerDark,
    onTertiaryContainer = BrandTertiaryContainerLight,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = DarkOnSurface,
    inverseOnSurface = DarkSurface,
    inversePrimary = BrandPrimary,
)

/**
 * Semantic status colors. Dark values are tonal equivalents chosen for
 * contrast on the dark surfaces above.
 */
data class MoStatusColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val error: Color,
)

val MoLightStatusColors = MoStatusColors(
    success = Color(0xFF1E7A55),
    warning = Color(0xFFB45309),
    info = Color(0xFF1D4ED8),
    error = ErrorLight,
)

val MoDarkStatusColors = MoStatusColors(
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    info = Color(0xFF60A5FA),
    error = ErrorDark,
)

/**
 * Category accent colors.
 *
 * Color is never the only category indicator — every usage pairs the
 * accent with a text label and a content description (see CategoryVisuals).
 */
data class MoCategoryColors(
    val actionRequired: Color,
    val important: Color,
    val career: Color,
    val education: Color,
    val receiptsOrders: Color,
    val security: Color,
    val notifications: Color,
    val newsletters: Color,
    val promotions: Color,
    val lowValue: Color,
    val unclassified: Color,
)

val MoLightCategoryColors = MoCategoryColors(
    actionRequired = ErrorLight,
    important = BrandPrimary,
    career = Color(0xFF1D4ED8),
    education = Color(0xFF7C3AED),
    receiptsOrders = Color(0xFF1E7A55),
    security = Color(0xFFB45309),
    notifications = Color(0xFF0284C7),
    newsletters = Color(0xFF0D9488),
    promotions = Color(0xFFEA580C),
    lowValue = Color(0xFF6B7280),
    unclassified = Color(0xFF6B7280),
)

val MoDarkCategoryColors = MoCategoryColors(
    actionRequired = ErrorDark,
    important = Color(0xFFB9A8FF),
    career = Color(0xFF60A5FA),
    education = Color(0xFFC4B5FD),
    receiptsOrders = Color(0xFF4ADE80),
    security = Color(0xFFFBBF24),
    notifications = Color(0xFF38BDF8),
    newsletters = Color(0xFF5EEAD4),
    promotions = Color(0xFFFB923C),
    lowValue = Color(0xFF9CA3AF),
    unclassified = Color(0xFF9CA3AF),
)
