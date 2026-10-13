package com.greninjaop.mailorganizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.greninjaop.mailorganizer.ui.foundation.FoundationViewModel
import com.greninjaop.mailorganizer.ui.foundation.StartupErrorScreen
import com.greninjaop.mailorganizer.ui.navigation.AppNavGraph
import com.greninjaop.mailorganizer.ui.splash.SplashScreen
import com.greninjaop.mailorganizer.ui.theme.MailOrganizerTheme

class MainActivity : ComponentActivity() {

    /** Cold-launch splash guard: shown at most once per process. */
    private companion object {
        var splashShown = false
    }

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
            MailOrganizerTheme(themeMode = foundationViewModel.themeMode) {
                // Cold-launch splash (2026-10-14): shown only on a fresh
                // process with no restored state — never on resume from
                // recents, never on rotation — and only once per process.
                // The app content is composed underneath and loads during
                // the splash; the splash overlays it, then fades away.
                var splashVisible by remember {
                    mutableStateOf(savedInstanceState == null && !splashShown)
                }
                if (splashVisible) {
                    // Raw preference flow: its first emission is the real
                    // stored mode (the StateFlow's SYSTEM initial would be
                    // ambiguous). The splash holds until it arrives, so it
                    // can never flash the wrong card.
                    val splashMode by app.container.themePreferences.themeMode
                        .collectAsState(initial = null)
                    Box(Modifier.fillMaxSize()) {
                        AppNavGraph(container = app.container)
                        SplashScreen(
                            themeMode = splashMode,
                            onDone = {
                                splashShown = true
                                splashVisible = false
                            },
                        )
                    }
                } else {
                    AppNavGraph(container = app.container)
                }
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
