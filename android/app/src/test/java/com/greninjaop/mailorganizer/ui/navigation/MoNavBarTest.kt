package com.greninjaop.mailorganizer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jelly-bubble bottom-nav math tests (2026-10-16): the pill's target
 * edges, its squash, and the icon-tint rule are pure functions — the
 * Compose animation itself just drives them.
 */
class MoNavBarTest {

    @Test
    fun `bubble edges are centred on the item`() {
        val (left, right) = pillTargetEdges(centerPx = 100f, halfWidthPx = 32f)
        assertEquals(68f, left, 0.001f)
        assertEquals(132f, right, 0.001f)
    }

    @Test
    fun `bubble edges can start left of zero`() {
        val (left, right) = pillTargetEdges(centerPx = 0f, halfWidthPx = 32f)
        assertEquals(-32f, left, 0.001f)
        assertEquals(32f, right, 0.001f)
    }

    @Test
    fun `leading edge spring is stiffer than trailing edge spring`() {
        val leading = leadingEdgeSpring()
        val trailing = trailingEdgeSpring()
        assertTrue(leading.stiffness > trailing.stiffness)
        // Both underdamped (dampingRatio < 1) so the bubble overshoots
        // slightly and shakes into place instead of stopping dead.
        assertTrue(leading.dampingRatio < 1f)
        assertEquals(leading.dampingRatio, trailing.dampingRatio, 0.001f)
    }

    @Test
    fun `no squash at rest width`() {
        assertEquals(1f, pillSquashScaleY(widthPx = 64f, normalWidthPx = 64f), 0.001f)
    }

    @Test
    fun `stretched bubble squashes vertically`() {
        // 1.5x width -> 10% vertical squash.
        assertEquals(0.90f, pillSquashScaleY(widthPx = 96f, normalWidthPx = 64f), 0.001f)
    }

    @Test
    fun `squash is clamped so the bubble never flattens away`() {
        assertEquals(0.80f, pillSquashScaleY(widthPx = 192f, normalWidthPx = 64f), 0.001f)
        assertEquals(0.80f, pillSquashScaleY(widthPx = 640f, normalWidthPx = 64f), 0.001f)
    }

    @Test
    fun `narrower-than-normal bubble does not grow taller`() {
        assertEquals(1f, pillSquashScaleY(widthPx = 32f, normalWidthPx = 64f), 0.001f)
    }

    @Test
    fun `icon tints when the bubble covers it`() {
        assertTrue(pillCoversIcon(pillCenterPx = 100f, iconCenterPx = 100f, halfWidthPx = 32f))
        assertTrue(pillCoversIcon(pillCenterPx = 120f, iconCenterPx = 100f, halfWidthPx = 32f))
        assertFalse(pillCoversIcon(pillCenterPx = 132f, iconCenterPx = 100f, halfWidthPx = 32f))
        assertFalse(pillCoversIcon(pillCenterPx = 0f, iconCenterPx = 100f, halfWidthPx = 32f))
    }
}
