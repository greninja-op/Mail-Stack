package com.greninjaop.mailorganizer.ui.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Infinite-scroll prefetch trigger tests (2026-10-16, Gmail parity):
 * the next page starts loading once the user scrolls past the halfway
 * mark of the loaded list — no "Load more" button.
 */
class MailInfiniteScrollTest {

    @Test
    fun `no prefetch when there is no more mail`() {
        assertFalse(shouldPrefetchMore(lastVisibleIndex = 49, itemCount = 50, hasMore = false))
    }

    @Test
    fun `no prefetch at the top of the list`() {
        assertFalse(shouldPrefetchMore(lastVisibleIndex = 0, itemCount = 50, hasMore = true))
    }

    @Test
    fun `no prefetch just before the halfway mark`() {
        assertFalse(shouldPrefetchMore(lastVisibleIndex = 24, itemCount = 50, hasMore = true))
    }

    @Test
    fun `prefetch at exactly the halfway mark`() {
        assertTrue(shouldPrefetchMore(lastVisibleIndex = 25, itemCount = 50, hasMore = true))
    }

    @Test
    fun `prefetch past the halfway mark`() {
        assertTrue(shouldPrefetchMore(lastVisibleIndex = 40, itemCount = 50, hasMore = true))
    }

    @Test
    fun `no prefetch for an empty list or no visible item`() {
        assertFalse(shouldPrefetchMore(lastVisibleIndex = 0, itemCount = 0, hasMore = true))
        assertFalse(shouldPrefetchMore(lastVisibleIndex = -1, itemCount = 50, hasMore = true))
    }

    @Test
    fun `prefetch when a short list fits on screen and more exists`() {
        // A short filtered list shows everything at once; the next page
        // should still load so the filter has more rows to match.
        assertTrue(shouldPrefetchMore(lastVisibleIndex = 9, itemCount = 10, hasMore = true))
    }
}

/**
 * Pull-to-refresh drag math tests (2026-10-17): the pull is
 * deliberately hard — the list travels at [PULL_DRAG_RATE] of the
 * finger and must reach [PULL_THRESHOLD_DP] of travel before the
 * release arms a refresh. A scroll-to-top swipe never gets there.
 */
class PullToRefreshDragTest {

    @Test
    fun `pull grows at the drag rate, not 1 to 1`() {
        // 100 px of finger moves the list 50 px (drag rate 0.5).
        assertEquals(50f, applyPullDrag(0f, 100f), 0.001f)
    }

    @Test
    fun `pull accumulates across drag deltas`() {
        val afterFirst = applyPullDrag(0f, 100f)
        assertEquals(100f, applyPullDrag(afterFirst, 100f), 0.001f)
    }

    @Test
    fun `scrolling back up shrinks the pull, damped`() {
        assertEquals(25f, applyPullDrag(50f, -50f), 0.001f)
    }

    @Test
    fun `pull never goes below zero`() {
        assertEquals(0f, applyPullDrag(10f, -100f), 0.001f)
    }

    @Test
    fun `a scroll-to-top swipe stays below the threshold`() {
        // A fast 100 dp swipe (100 px at mdpi) moves the list 50 px —
        // below the 96 dp threshold, so it never arms a refresh.
        val thresholdPx = 96f // PULL_THRESHOLD_DP at mdpi
        assertFalse(isPullPastThreshold(applyPullDrag(0f, 100f), thresholdPx))
    }

    @Test
    fun `a deliberate 192 dp pull reaches the threshold`() {
        // 192 px of finger at the 0.5 drag rate = 96 px of list travel.
        val thresholdPx = 96f
        assertTrue(isPullPastThreshold(applyPullDrag(0f, 192f), thresholdPx))
    }

    @Test
    fun `threshold is inclusive - exactly at it arms the refresh`() {
        assertTrue(isPullPastThreshold(96f, 96f))
        assertFalse(isPullPastThreshold(95.9f, 96f))
    }
}
