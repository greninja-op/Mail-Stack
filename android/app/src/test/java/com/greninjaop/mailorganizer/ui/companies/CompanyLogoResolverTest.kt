package com.greninjaop.mailorganizer.ui.companies

import com.greninjaop.mailorganizer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyLogoResolverTest {

    @Test
    fun `bundledRes maps all known domains`() {
        assertEquals(R.drawable.company_adobe, CompanyLogoResolver.bundledRes("adobe.com"))
        assertEquals(R.drawable.company_amazon, CompanyLogoResolver.bundledRes("amazon.com"))
        assertEquals(
            R.drawable.company_amazon_web_services,
            CompanyLogoResolver.bundledRes("amazonaws.com"),
        )
        assertEquals(R.drawable.company_apple, CompanyLogoResolver.bundledRes("apple.com"))
        assertEquals(R.drawable.company_clickup, CompanyLogoResolver.bundledRes("clickup.com"))
        assertEquals(R.drawable.company_duolingo, CompanyLogoResolver.bundledRes("duolingo.com"))
        assertEquals(R.drawable.company_devpost, CompanyLogoResolver.bundledRes("devpost.com"))
        assertEquals(
            R.drawable.company_alibabacloud,
            CompanyLogoResolver.bundledRes("alibabacloud.com"),
        )
    }

    @Test
    fun `bundledRes strips www prefix and ignores case`() {
        assertEquals(R.drawable.company_adobe, CompanyLogoResolver.bundledRes("WWW.Adobe.COM"))
        assertEquals(R.drawable.company_clickup, CompanyLogoResolver.bundledRes("www.clickup.com"))
    }

    @Test
    fun `bundledRes returns null for unknown domains`() {
        assertNull(CompanyLogoResolver.bundledRes("example.com"))
        assertNull(CompanyLogoResolver.bundledRes("dnull.xyz"))
        assertNull(CompanyLogoResolver.bundledRes(""))
    }

    @Test
    fun `bundledRes maps the 2026-10-13 logo batch`() {
        assertEquals(R.drawable.company_canva, CompanyLogoResolver.bundledRes("canva.com"))
        assertEquals(R.drawable.company_dribbble, CompanyLogoResolver.bundledRes("dribbble.com"))
        assertEquals(R.drawable.company_google, CompanyLogoResolver.bundledRes("google.com"))
        assertEquals(R.drawable.company_youtube, CompanyLogoResolver.bundledRes("youtube.com"))
        assertEquals(R.drawable.company_youtube, CompanyLogoResolver.bundledRes("youtu.be"))
        assertEquals(R.drawable.company_spotify, CompanyLogoResolver.bundledRes("spotify.com"))
        assertEquals(R.drawable.company_reddit, CompanyLogoResolver.bundledRes("reddit.com"))
        assertEquals(R.drawable.company_netflix, CompanyLogoResolver.bundledRes("netflix.com"))
        assertEquals(R.drawable.company_linkedin, CompanyLogoResolver.bundledRes("linkedin.com"))
        assertEquals(R.drawable.company_glasp, CompanyLogoResolver.bundledRes("glasp.co"))
        assertEquals(R.drawable.company_openrouter, CompanyLogoResolver.bundledRes("openrouter.ai"))
        assertEquals(R.drawable.company_github, CompanyLogoResolver.bundledRes("github.com"))
        assertEquals(R.drawable.company_x, CompanyLogoResolver.bundledRes("x.com"))
        assertEquals(R.drawable.company_x, CompanyLogoResolver.bundledRes("twitter.com"))
        // Kick + Reddit's mail domain (notifications come from redditmail.com).
        assertEquals(R.drawable.company_kick, CompanyLogoResolver.bundledRes("kick.com"))
        assertEquals(R.drawable.company_reddit, CompanyLogoResolver.bundledRes("redditmail.com"))
    }

    @Test
    fun `bundledRes maps the 2026-10-17 Google product icons`() {
        // Integrations rows: the user-supplied Calendar / Tasks icons,
        // not the Google "G" favicon.
        assertEquals(
            R.drawable.ic_calendar_logo,
            CompanyLogoResolver.bundledRes("calendar.google.com"),
        )
        assertEquals(
            R.drawable.ic_tasks_logo,
            CompanyLogoResolver.bundledRes("tasks.google.com"),
        )
    }

    @Test
    fun `new batch logos are full-color, not monochrome`() {
        assertFalse(CompanyLogoResolver.isMonochrome(R.drawable.company_canva))
        assertFalse(CompanyLogoResolver.isMonochrome(R.drawable.company_github))
        assertFalse(CompanyLogoResolver.isMonochrome(R.drawable.company_x))
        assertFalse(CompanyLogoResolver.isMonochrome(R.drawable.company_kick))
    }

    @Test
    fun `isMonochrome flags only the glyph bundles`() {
        assertTrue(CompanyLogoResolver.isMonochrome(R.drawable.company_devpost))
        assertTrue(CompanyLogoResolver.isMonochrome(R.drawable.company_alibabacloud))
        assertFalse(CompanyLogoResolver.isMonochrome(R.drawable.company_adobe))
        assertFalse(CompanyLogoResolver.isMonochrome(R.drawable.company_duolingo))
    }

    @Test
    fun `remoteFile matches the exact base name`() {
        val names = setOf("adobe.png", "duolingo.png", "clickup.png")
        assertEquals("adobe.png", CompanyLogoResolver.remoteFile("adobe.com", names))
        assertEquals("duolingo.png", CompanyLogoResolver.remoteFile("duolingo.com", names))
    }

    @Test
    fun `remoteFile applies the amazonaws override`() {
        val names = setOf("amazon-web-services.png", "amazon-web-services-light.png")
        assertEquals(
            "amazon-web-services.png",
            CompanyLogoResolver.remoteFile("amazonaws.com", names),
        )
    }

    @Test
    fun `remoteFile prefers the base name over dark and light variants`() {
        val names = setOf("apple-light.png", "apple.png", "apple-dark.png")
        assertEquals("apple.png", CompanyLogoResolver.remoteFile("apple.com", names))
    }

    @Test
    fun `remoteFile returns null when only a variant exists`() {
        assertNull(CompanyLogoResolver.remoteFile("apple.com", setOf("apple-light.png")))
        assertNull(CompanyLogoResolver.remoteFile("apple.com", setOf("apple-dark.png")))
    }

    @Test
    fun `remoteFile strips www and subdomains`() {
        val names = setOf("adobe.png")
        assertEquals("adobe.png", CompanyLogoResolver.remoteFile("www.adobe.com", names))
        assertEquals("adobe.png", CompanyLogoResolver.remoteFile("mail.adobe.com", names))
    }

    @Test
    fun `remoteFile ignores case`() {
        assertEquals(
            "duolingo.png",
            CompanyLogoResolver.remoteFile("DuoLingo.COM", setOf("duolingo.png")),
        )
    }

    @Test
    fun `remoteFile returns null when nothing matches`() {
        val names = setOf("adobe.png", "duolingo.png")
        assertNull(CompanyLogoResolver.remoteFile("dnull.xyz", names))
        assertNull(CompanyLogoResolver.remoteFile("canva.com", names))
        assertNull(CompanyLogoResolver.remoteFile("notadomain", names))
        assertNull(CompanyLogoResolver.remoteFile("", names))
        assertNull(CompanyLogoResolver.remoteFile("adobe.com", emptySet()))
    }
}
