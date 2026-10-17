package com.greninjaop.mailorganizer.tablet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandViolet = Color(0xFF5238E8)
private val BrandVioletDark = Color(0xFFB9A8FF)
private val DarkBg = Color(0xFF131120)

@Composable
fun MailstackTabletTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(primary = BrandVioletDark, background = DarkBg, surface = Color(0xFF1C1A2E))
        } else {
            lightColorScheme(primary = BrandViolet)
        },
        content = content,
    )
}
