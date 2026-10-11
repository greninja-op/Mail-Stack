package com.greninjaop.mailorganizer.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Connect Gmail account screen (Phase 3 §21).
 *
 * Explains what permissions are requested and why (Phase 3 §13),
 * then initiates the Google OAuth flow.
 */
@Composable
fun ConnectAccountScreen(
    viewModel: ConnectAccountViewModel,
    onConnected: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
            text = "Permission requested: Read your Gmail messages (gmail.readonly). " +
                "Mailstack cannot send, delete, or modify your emails.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (val state = uiState) {
            is ConnectAccountUiState.Idle -> {
                Button(
                    onClick = { viewModel.startOAuthFlow() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Connect with Google")
                }
            }
            is ConnectAccountUiState.Connecting -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Connecting to Google…",
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
                    onClick = onConnected,
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
                    onClick = { viewModel.startOAuthFlow() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Try Again")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
}

/**
 * UI state for the Connect Account screen.
 */
sealed interface ConnectAccountUiState {
    data object Idle : ConnectAccountUiState
    data object Connecting : ConnectAccountUiState
    data class Success(val email: String) : ConnectAccountUiState
    data class Error(val message: String) : ConnectAccountUiState
}
