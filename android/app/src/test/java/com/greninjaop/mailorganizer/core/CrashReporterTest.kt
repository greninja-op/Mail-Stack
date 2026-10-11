package com.greninjaop.mailorganizer.core

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Crash-repair (2026-10-11): the reporter must never throw and must stay
 * small enough to copy into a chat message.
 */
class CrashReporterTest {

    @Test
    fun `formatReport includes thread, exception and top frames`() {
        val error = RuntimeException("boom")
        val report = CrashReporter.formatReport("main", error, epochMs = 0L)

        assertTrue(report.contains("Thread: main"))
        assertTrue(report.contains("java.lang.RuntimeException: boom"))
        assertTrue(report.contains("at "))
        assertTrue(report.startsWith("Mailstack crash report"))
    }

    @Test
    fun `formatReport includes causal chain`() {
        val root = IllegalStateException("root cause")
        val error = RuntimeException("top", root)
        val report = CrashReporter.formatReport("main", error)

        assertTrue(report.contains("Caused by: java.lang.IllegalStateException: root cause"))
    }

    @Test
    fun `formatReport is bounded`() {
        val deep = RuntimeException("x")
        // Build a deep stack by nesting.
        var t: Throwable = deep
        repeat(10) { t = RuntimeException("wrap $it", t) }
        val report = CrashReporter.formatReport("main", t)

        assertTrue(report.length <= CrashReporter.MAX_REPORT_CHARS + 20)
    }

    @Test
    fun `writeReport persists and readLatestReport reads back`() {
        val dir = Files.createTempDirectory("crash-reports").toFile()
        try {
            val written = CrashReporter.writeReport(dir, "report-body")
            assertTrue("writeReport returned null", written != null && written.exists())
            assertEquals("report-body", CrashReporter.readLatestReport(dir))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `writeReport prunes beyond max kept`() {
        val dir = Files.createTempDirectory("crash-reports").toFile()
        try {
            repeat(CrashReporter.MAX_KEPT_REPORTS + 3) { i ->
                // Ensure distinct timestamps for ordering.
                Thread.sleep(2)
                CrashReporter.writeReport(dir, "report-$i")
            }
            val kept = dir.listFiles { f -> f.isFile }?.size ?: 0
            assertEquals(CrashReporter.MAX_KEPT_REPORTS, kept)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `readLatestReport returns null when empty`() {
        val dir = Files.createTempDirectory("crash-reports").toFile()
        try {
            assertNull(CrashReporter.readLatestReport(dir))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `writeReport never throws on bad dir`() {
        // A path that cannot be created (a file, not a dir).
        val file = Files.createTempFile("not-a-dir", ".tmp").toFile()
        try {
            val result = CrashReporter.writeReport(File(file, "child"), "x")
            assertNull(result)
        } finally {
            file.delete()
        }
    }
}
