package com.greninjaop.mailorganizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
        setContent {
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
