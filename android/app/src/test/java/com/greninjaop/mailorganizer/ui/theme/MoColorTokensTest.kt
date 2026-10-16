package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the color tokens. 2026-10-16 (Expressive redesign): the spec
 * values changed deliberately — vivid violet brand, violet-tinted
 * surfaces, dark mode on deep violet-tinted charcoal (never pure black).
 * If the palette changes again, this test fails loudly on purpose.
 */
class MoColorTokensTest {

    @Test
    fun `light scheme uses the expressive primary tokens`() {
        assertEquals(Color(0xFF5238E8), MoLightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), MoLightColorScheme.onPrimary)
        assertEquals(Color(0xFFE3DCFF), MoLightColorScheme.primaryContainer)
    }

    @Test
    fun `light scheme uses the expressive surface tokens`() {
        assertEquals(Color(0xFFF6F3FF), MoLightColorScheme.background)
        assertEquals(Color(0xFFFDFBFF), MoLightColorScheme.surface)
    }

    @Test
    fun `dark scheme uses the expressive surface tokens`() {
        assertEquals(Color(0xFF131120), MoDarkColorScheme.background)
        assertEquals(Color(0xFF1A1826), MoDarkColorScheme.surface)
        assertEquals(Color(0xFF322D4E), MoDarkColorScheme.surfaceContainerHigh)
    }

    @Test
    fun `dark scheme uses the expressive primary`() {
        assertEquals(Color(0xFFB9A8FF), MoDarkColorScheme.primary)
    }

    @Test
    fun `light status colors match the expressive semantic tokens`() {
        assertEquals(Color(0xFF1E7A55), MoLightStatusColors.success)
        assertEquals(Color(0xFFB45309), MoLightStatusColors.warning)
        assertEquals(Color(0xFFB3261E), MoLightStatusColors.error)
        assertEquals(Color(0xFF1D4ED8), MoLightStatusColors.info)
        assertEquals(Color(0xFFB3261E), MoLightColorScheme.error)
    }
}
