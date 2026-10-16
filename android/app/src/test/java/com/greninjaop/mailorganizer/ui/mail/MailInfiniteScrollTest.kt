package com.greninjaop.mailorganizer.ui.mail

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
