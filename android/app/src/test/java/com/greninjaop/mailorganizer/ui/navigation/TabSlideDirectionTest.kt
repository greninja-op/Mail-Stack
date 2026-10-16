package com.greninjaop.mailorganizer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Directional tab slides (2026-10-16): the incoming tab's content must
 * enter from the side that tab sits on in bottom-bar order
 * (HOME, MAIL, CATEGORIES, COMPANIES, ACTIONS — left to right).
 */
class TabSlideDirectionTest {

    @Test
    fun toRightTab_entersFromRight() {
        assertEquals(1, tabSlideDirection(AppDestinations.HOME, AppDestinations.MAIL))
        assertEquals(1, tabSlideDirection(AppDestinations.MAIL, AppDestinations.ACTIONS))
        assertEquals(1, tabSlideDirection(AppDestinations.CATEGORIES, AppDestinations.COMPANIES))
    }

    @Test
    fun toLeftTab_entersFromLeft() {
        assertEquals(-1, tabSlideDirection(AppDestinations.MAIL, AppDestinations.HOME))
        assertEquals(-1, tabSlideDirection(AppDestinations.ACTIONS, AppDestinations.MAIL))
        assertEquals(-1, tabSlideDirection(AppDestinations.COMPANIES, AppDestinations.CATEGORIES))
    }

    @Test
    fun sameOrNonTab_isZero() {
        assertEquals(0, tabSlideDirection(AppDestinations.MAIL, AppDestinations.MAIL))
        assertEquals(0, tabSlideDirection(AppDestinations.MAIL, AppDestinations.SETTINGS))
        assertEquals(0, tabSlideDirection(null, AppDestinations.MAIL))
        assertEquals(0, tabSlideDirection(AppDestinations.MAIL, null))
    }
}
