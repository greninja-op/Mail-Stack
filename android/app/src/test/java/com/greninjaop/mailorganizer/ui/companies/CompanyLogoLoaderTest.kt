package com.greninjaop.mailorganizer.ui.companies

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-function tests for the favicon fallback in [CompanyLogoLoader].
 * The network stages themselves are not unit-tested (no network in the
 * sandbox JVM); the URL shape and the default-globe detection are.
 */
class CompanyLogoLoaderTest {

    @Test
    fun `faviconUrl requests 128px for the domain`() {
        assertEquals(
            "https://www.google.com/s2/favicons?domain=acme.com&sz=128",
            CompanyLogoLoader.faviconUrl("acme.com"),
        )
    }

    @Test
    fun `isDefaultGlobe matches identical bytes`() {
        val globe = byteArrayOf(1, 2, 3, 4)
        assertTrue(CompanyLogoLoader.isDefaultGlobe(byteArrayOf(1, 2, 3, 4), globe))
    }

    @Test
    fun `isDefaultGlobe rejects different bytes`() {
        val globe = byteArrayOf(1, 2, 3, 4)
        assertFalse(CompanyLogoLoader.isDefaultGlobe(byteArrayOf(1, 2, 3, 5), globe))
        assertFalse(CompanyLogoLoader.isDefaultGlobe(byteArrayOf(1, 2, 3), globe))
        assertFalse(CompanyLogoLoader.isDefaultGlobe(byteArrayOf(), globe))
    }
}
