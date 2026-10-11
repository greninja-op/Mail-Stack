package com.greninjaop.mailorganizer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.AuthResult
import com.greninjaop.mailorganizer.data.auth.GoogleAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Connect Account screen (Phase 3 §21).
 *
 * Coordinates the Google OAuth flow via [AuthRepository].
 * The Activity handles the Credential Manager UI; this ViewModel
 * processes the result.
 */
class ConnectAccountViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectAccountUiState>(ConnectAccountUiState.Idle)
    val uiState: StateFlow<ConnectAccountUiState> = _uiState.asStateFlow()

    /**
     * Signals the UI to launch the Google OAuth flow via Custom Tabs.
     * The Activity should call [AuthRepository.createOAuthIntent] and
     * start it, then handle the redirect in onNewIntent and call
     * [onOAuthRedirect].
     */
    fun startOAuthFlow() {
        _uiState.value = ConnectAccountUiState.Connecting
        // The Activity launches the OAuth Intent from
        // AuthRepository.createOAuthIntent().
        // This is a signal for the UI layer.
    }

    /**
     * Called by the Activity when the OAuth redirect is received.
     */
    fun onOAuthRedirect(data: android.content.Intent?) {
        viewModelScope.launch {
            _uiState.value = ConnectAccountUiState.Connecting

            when (val result = authRepository.handleOAuthResult(data)) {
                is AuthResult.Success -> {
                    _uiState.value = ConnectAccountUiState.Success(result.email)
                }
                is AuthResult.Cancelled -> {
                    _uiState.value = ConnectAccountUiState.Idle
                }
                is AuthResult.Failure -> {
                    _uiState.value = ConnectAccountUiState.Error(
                        when (result.reason) {
                            com.greninjaop.mailorganizer.data.auth.AuthErrorReason.USER_DENIED ->
                                "You denied the permission. Mailstack needs Gmail read access to organize your mail."
                            com.greninjaop.mailorganizer.data.auth.AuthErrorReason.NETWORK_ERROR ->
                                "Network error. Check your connection and try again."
                            else ->
                                "Connection failed. Please try again."
                        }
                    )
                }
            }
        }
    }

    /**
     * Called when the user cancels the sign-in flow.
     */
    fun onSignInCancelled() {
        _uiState.value = ConnectAccountUiState.Idle
    }

    fun reset() {
        _uiState.value = ConnectAccountUiState.Idle
    }
}

/**
 * Factory for [ConnectAccountViewModel].
 */
class ConnectAccountViewModelFactory(
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConnectAccountViewModel(authRepository) as T
    }
}
