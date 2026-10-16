package com.greninjaop.mailorganizer.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jelly-bubble math tests (2026-10-16): the pill's target edges, its
 * squash, the icon-tint rule, and the segment geometry are pure
 * functions — the Compose animation itself just drives them.
 */
class JellyBubbleTest {

    @Test
    fun `bubble edges are centred on the target`() {
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
    fun `content tints when the bubble covers it`() {
        assertTrue(pillCoversIcon(pillCenterPx = 100f, iconCenterPx = 100f, halfWidthPx = 32f))
        assertTrue(pillCoversIcon(pillCenterPx = 120f, iconCenterPx = 100f, halfWidthPx = 32f))
        assertFalse(pillCoversIcon(pillCenterPx = 132f, iconCenterPx = 100f, halfWidthPx = 32f))
        assertFalse(pillCoversIcon(pillCenterPx = 0f, iconCenterPx = 100f, halfWidthPx = 32f))
    }

    @Test
    fun `segment centres split the container evenly`() {
        assertEquals(listOf(50f, 150f, 250f), segmentCenters(widthPx = 300f, count = 3))
        assertEquals(
            listOf(40f, 120f, 200f, 280f, 360f),
            segmentCenters(widthPx = 400f, count = 5),
        )
    }

    @Test
    fun `segment bubble leaves the inset on both sides`() {
        // 100px segment, 4px inset -> 92px bubble -> 46px half-width.
        assertEquals(46f, segmentPillHalfWidth(segmentWidthPx = 100f, insetPx = 4f), 0.001f)
    }
}
