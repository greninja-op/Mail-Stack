package com.greninjaop.mailorganizer

import android.app.Application
import android.content.pm.ApplicationInfo
import com.greninjaop.mailorganizer.core.CrashReporter
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.di.AppContainer
import java.io.File

/**
 * Application entry point.
 *
 * Phase 0 wires a manual [AppContainer] (constructor injection). A DI framework
 * (Hilt/Koin) is deliberately deferred: the graph is small enough for manual
 * wiring, and the decision is revisited in Phase 1 if the graph justifies it.
 *
 * Crash-repair hardening (2026-10-11): [onCreate] must NEVER kill the
 * process. Previously any throwable during init was rethrown, so a single
 * failing initializer (e.g. disk I/O, FTS5 availability, scheduler start)
 * produced an instant "app keeps stopping" on every launch with no visible
 * diagnostics. Now:
 * - init failures are recorded in [startupError] and surfaced by
 *   MainActivity as a copyable error screen instead of a crash;
 * - the sync scheduler starts on a background thread (it touches disk);
 * - uncaught exceptions are also persisted to the app's files dir, so the
 *   error screen can show the *previous* run's crash even after a death.
 */
class MailOrganizerApp : Application() {

    private var _container: AppContainer? = null

    /**
     * The DI container. Only valid when [startupError] is null; MainActivity
     * checks [startupError] before touching this.
     */
    val container: AppContainer
        get() = _container
            ?: throw IllegalStateException("AppContainer not initialized (startupError=$startupError)")

    /** Null-safe accessor for best-effort calls (e.g. onResume). */
    fun containerOrNull(): AppContainer? = _container

    /** Set when [onCreate] init fails; MainActivity renders it instead of crashing. */
    @Volatile
    var startupError: Throwable? = null
        private set

    /** Directory holding persisted crash reports (see [CrashReporter]). */
    fun crashReportsDir(): File = File(filesDir, "crash-reports")

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        MoLogger.init(isDebug)
        try {
            val c = AppContainer(this)
            _container = c
            // Best-effort and disk-touching: never on the main thread, never fatal.
            // (Previously this ran inline on the main thread, so any failure in
            // the lazy init chain crashed the app before the first frame.)
            Thread({
                try {
                    c.syncScheduler.onAppStart()
                } catch (t: Throwable) {
                    MoLogger.e(TAG, "syncScheduler.onAppStart() failed (non-fatal)", t)
                }
            }, "mailstack-startup").apply { isDaemon = true }.start()
            MoLogger.i(TAG, "Mail Organizer foundation initialized")
        } catch (t: Throwable) {
            startupError = t
            persistCrash(t)
            MoLogger.e(TAG, "Failed to initialize AppContainer (non-fatal: error screen will show)", t)
            // Deliberately NOT rethrown: MainActivity shows StartupErrorScreen.
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                MoLogger.e(TAG, "Uncaught exception on thread=${thread.name}", throwable)
                persistCrash(throwable, thread.name)
            } catch (_: Throwable) {
                // Never let the crash reporter itself crash.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun persistCrash(throwable: Throwable, threadName: String = "main") {
        try {
            val report = CrashReporter.formatReport(threadName, throwable)
            CrashReporter.writeReport(crashReportsDir(), report)
        } catch (_: Throwable) {
            // Best effort only.
        }
    }

    private companion object {
        const val TAG = "MailOrganizerApp"
    }
}
