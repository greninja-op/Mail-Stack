package com.greninjaop.mailorganizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.greninjaop.mailorganizer.ui.foundation.FoundationViewModel
import com.greninjaop.mailorganizer.ui.foundation.StartupErrorScreen
import com.greninjaop.mailorganizer.ui.navigation.AppNavGraph
import com.greninjaop.mailorganizer.ui.theme.MailOrganizerTheme

class MainActivity : ComponentActivity() {

    private val foundationViewModel: FoundationViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as MailOrganizerApp
                return FoundationViewModel(
                    themePreferences = app.container.themePreferences,
                    dispatchers = app.container.dispatchers,
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MailOrganizerApp
        // Crash-repair (2026-10-11): if Application init failed, show the
        // error screen with copyable details instead of crashing again.
        // The viewModels delegate above is only touched on the happy path.
        val startupError = app.startupError
        if (startupError != null || app.containerOrNull() == null) {
            setContent {
                MailOrganizerTheme {
                    StartupErrorScreen(
                        error = startupError,
                        onRestart = { restartApp() },
                    )
                }
            }
            return
        }
        // OAuth cold start (2026-10-11 fix): if Android killed our process
        // while the browser was in front (common on MIUI), the redirect
        // intent arrives here in onCreate — NOT in onNewIntent. Handle it
        // the same way, otherwise consent completes in the browser and the
        // app silently does nothing ("authentication not working").
        handlePossibleOAuthRedirect(intent)
        setContent {
            // No launch animation (removed 2026-10-16 per user request):
            // the app opens straight into the content, light or dark.
            MailOrganizerTheme(themeMode = foundationViewModel.themeMode) {
                AppNavGraph(container = app.container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Null-safe: container may be absent when the startup error screen
        // is showing; never crash the error screen itself.
        (application as? MailOrganizerApp)?.containerOrNull()
            ?.syncScheduler?.onAppResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePossibleOAuthRedirect(intent)
    }

    /**
     * Phase 3: Google OAuth redirects back via the custom scheme
     * (singleTask → onNewIntent). Forwarded to the auth repository, which
     * validates state, exchanges the code, and emits the result for the
     * Connect UI. Non-OAuth intents are ignored.
     */
    private fun handlePossibleOAuthRedirect(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != com.greninjaop.mailorganizer.data.auth.GoogleAuthRepository.REDIRECT_SCHEME) {
            return
        }
        // Consume the redirect (2026-10-16): the authorization code is
        // single-use, and this Activity's stored intent survives
        // recreation — without consuming it, a later onCreate would
        // re-handle the same code, the re-exchange would fail, and the
        // UI would flip Success -> Error ("Try Again" -> back to
        // Chrome). The in-flight handling holds its own reference to
        // the intent, so clearing the stored one is safe. Non-OAuth
        // intents are never consumed (early returns above).
        setIntent(Intent())
        val container = (application as? MailOrganizerApp)?.containerOrNull() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                container.authRepository.handleOAuthResult(intent)
            } catch (e: Exception) {
                com.greninjaop.mailorganizer.core.MoLogger.w(
                    "MainActivity",
                    "OAuth redirect handling failed: ${e.javaClass.simpleName}",
                )
            }
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val restart = intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } ?: return
        startActivity(restart)
        finish()
        Runtime.getRuntime().exit(0)
    }
}
