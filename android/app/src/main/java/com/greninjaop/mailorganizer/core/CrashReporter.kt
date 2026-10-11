package com.greninjaop.mailorganizer.core

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Startup-crash diagnostics (crash-repair work, 2026-10-11).
 *
 * The app previously crashed immediately on launch for some users with no
 * way to see why: [MoLogger] only writes to logcat, which is unavailable
 * on user devices. This helper formats an uncaught exception into a short
 * human-readable report and persists it to the app's files directory, where
 * the startup-error screen can surface it (copyable) instead of the process
 * dying silently.
 *
 * Pure JVM (java.io only) so it is unit-testable without Robolectric.
 * Never throws: every entry point swallows its own failures, because a
 * crash reporter must never become a second crash.
 */
object CrashReporter {

    /** Keep at most this many persisted reports; older ones are pruned. */
    const val MAX_KEPT_REPORTS = 5

    /** Hard cap on a single report so a deep stack trace can't fill storage. */
    const val MAX_REPORT_CHARS = 24_000

    /**
     * Formats a one-screen report. Includes the thread, the exception chain
     * (3 levels max) and the top stack frames (20 max) — enough to identify
     * the crash site, small enough to copy into a chat message.
     */
    fun formatReport(
        threadName: String,
        throwable: Throwable,
        epochMs: Long = System.currentTimeMillis(),
    ): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(epochMs))
        val sb = StringBuilder()
        sb.appendLine("Mailstack crash report — $stamp")
        sb.appendLine("Thread: $threadName")
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 3) {
            if (depth > 0) sb.append("Caused by: ")
            sb.appendLine("${current.javaClass.name}: ${current.message}")
            current.stackTrace.take(20).forEach { frame ->
                sb.appendLine("    at $frame")
            }
            current = current.cause
            depth++
        }
        val full = sb.toString()
        return if (full.length > MAX_REPORT_CHARS) {
            full.take(MAX_REPORT_CHARS) + "\n…(truncated)"
        } else {
            full
        }
    }

    /**
     * Persists [report] under [reportsDir] (created if missing) and prunes
     * older reports beyond [MAX_KEPT_REPORTS]. Returns the written file, or
     * null if persistence failed (never throws).
     */
    fun writeReport(reportsDir: File, report: String): File? {
        return try {
            if (!reportsDir.exists() && !reportsDir.mkdirs()) return null
            val file = File(reportsDir, "crash-${System.currentTimeMillis()}.txt")
            file.writeText(report)
            pruneOldReports(reportsDir)
            file
        } catch (_: Throwable) {
            null
        }
    }

    /** Reads back the newest persisted report, or null if none exists. */
    fun readLatestReport(reportsDir: File): String? {
        return try {
            reportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") }
                ?.maxByOrNull { it.lastModified() }
                ?.readText()
        } catch (_: Throwable) {
            null
        }
    }

    private fun pruneOldReports(reportsDir: File) {
        try {
            val files = reportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            files.drop(MAX_KEPT_REPORTS).forEach { file ->
                try {
                    file.delete()
                } catch (_: Throwable) {
                    // Best effort.
                }
            }
        } catch (_: Throwable) {
            // Best effort.
        }
    }
}
