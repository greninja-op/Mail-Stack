package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Centralized typography tokens — "Mailstack Expressive" (2026-10-16
 * redesign).
 *
 * Material 3 Expressive type: display and headline sizes grow so screen
 * headers, empty states, and big numbers carry the design — the Pixel
 * reference's oversized "ELENA / NICE / 07:30" moments. Body and label
 * sizes stay put: mail rows and settings stay compact and readable.
 *
 * Inter is the preferred typeface per the design contract. It is NOT
 * bundled: the GitHub push path available in this environment cannot
 * verifiably transport binary font files, so bundling them would risk a
 * corrupt or incomplete repository. The scale below therefore uses the
 * system sans-serif fallback, which the execution plan explicitly allows
 * ("with an appropriate system fallback").
 *
 * Per-screen type customization must be justified and added here, not inline.
 */
private val MoFontFamily: FontFamily = FontFamily.Default

val MoTypography = Typography(
    // Display — expressive moments: empty states, big headers, big numbers.
    displayLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 64.sp, lineHeight = 72.sp, letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = 0.sp,
    ),
    // Headlines — screen titles and section headers.
    headlineLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp,
    ),
    // Title
    titleLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.sp,
    ),
    // Section title
    titleMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    // Secondary text
    bodyMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    // Caption / labels
    labelLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
