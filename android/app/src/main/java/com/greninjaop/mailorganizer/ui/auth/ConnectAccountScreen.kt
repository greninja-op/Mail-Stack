package com.greninjaop.mailorganizer.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.greninjaop.mailorganizer.ui.theme.MoCard
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Connect Gmail account screen (Phase 3 §21).
 *
 * Explains what permissions are requested and why (Phase 3 §13), then
 * opens the Google consent page in the system browser. The OAuth
 * redirect returns to the app and the ViewModel updates this UI.
 */
@Composable
fun ConnectAccountScreen(
    viewModel: ConnectAccountViewModel,
    onConnected: (accountId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // If the user backs out of the browser, no redirect arrives — tell
    // the ViewModel we foregrounded so it can leave the spinner state.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun launchOAuth() {
        val intent = viewModel.createOAuthIntent()
        context.startActivity(intent)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        // Surface provides LocalContentColor = onBackground, so child Text
        // defaults stay readable in both light and dark themes (2026-10-12:
        // bare Column + background() left text black-on-dark).
        // 2026-10-16 (Expressive redesign): solid theme background.
        color = MaterialTheme.colorScheme.background,
    ) {
    // 2026-10-14 (Expressive redesign): centred content block in a
    // MoCard; solid background root + centering. Buttons untouched.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
    MoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
    Column(
        modifier = Modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Connect Gmail",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Mailstack needs permission to read your Gmail " +
                "so it can organize your mailbox on-device. " +
                "Your emails never leave your phone except to fetch them from Google.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Permission requested: read your Gmail messages (gmail.readonly). " +
                "Mailstack cannot send, delete, or modify your emails. " +
                "You can disconnect anytime from Settings → Accounts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (val state = uiState) {
            is ConnectAccountUiState.Idle -> {
                Button(
                    onClick = { launchOAuth() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Connect with Google")
                }
            }
            is ConnectAccountUiState.Connecting -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Waiting for Google… complete the sign-in in your browser.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is ConnectAccountUiState.Success -> {
                Text(
                    text = "Connected as ${state.email}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onConnected(state.accountId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue")
                }
            }
            is ConnectAccountUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { launchOAuth() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Try Again")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
    }
    }
    }
}

/**
 * UI state for the Connect Account screen.
 */
sealed interface ConnectAccountUiState {
    data object Idle : ConnectAccountUiState
    data object Connecting : ConnectAccountUiState
    /** accountId is needed to route new accounts to the import flow. */
    data class Success(val accountId: String, val email: String) : ConnectAccountUiState
    data class Error(val message: String) : ConnectAccountUiState
}
