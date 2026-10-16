package com.greninjaop.mailorganizer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MoNavBar tests. The bubble physics tests moved with the bubble to
 * JellyBubbleTest (ui/components) when the bubble became shared.
 */
class MoNavBarTest {

    @Test
    fun `primary destinations are in display order with stable routes`() {
        assertEquals(
            listOf("home", "mail", "categories", "companies", "actions"),
            PrimaryDestination.entries.map { it.route },
        )
        assertEquals(
            listOf("Home", "Mail", "Categories", "Companies", "Actions"),
            PrimaryDestination.entries.map { it.label },
        )
    }
}
