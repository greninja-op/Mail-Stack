package com.greninjaop.mailorganizer.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.auth.AuthErrorReason
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Connect Account screen (Phase 3 §21).
 *
 * Real flow:
 * 1. UI calls [createOAuthIntent] and starts it (system browser).
 * 2. Google redirects to the app; MainActivity forwards the redirect to
 *    [AuthRepository.handleOAuthResult].
 * 3. This ViewModel collects [AuthRepository.observeOAuthResults] and
 *    drives the connecting/success/error states.
 *
 * If the user backs out of the browser, no redirect arrives — the
 * Connecting state offers an explicit Cancel ([reset]). There is
 * deliberately NO timeout reset: the token exchange can take longer
 * than any fixed grace, and the old 2s timeout reset used to flip the
 * UI back to "Connect with Google" mid-exchange, inviting a second tap
 * that opened Chrome all over again.
 */
class ConnectAccountViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectAccountUiState>(ConnectAccountUiState.Idle)
    val uiState: StateFlow<ConnectAccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                authRepository.observeOAuthResults().collect { result ->
                    _uiState.value = when (result) {
                        is AuthResult.Success ->
                            ConnectAccountUiState.Success(result.accountId.value, result.email)
                        is AuthResult.Cancelled ->
                            ConnectAccountUiState.Idle
                        is AuthResult.Failure ->
                            ConnectAccountUiState.Error(
                                when (result.reason) {
                                    AuthErrorReason.USER_DENIED ->
                                        "Permission was denied. Mailstack needs Gmail read access to organize your mail — nothing is sent or modified."
                                    AuthErrorReason.NETWORK_ERROR ->
                                        "Network error. Check your connection and try again."
                                    AuthErrorReason.REVOKED ->
                                        "Google revoked the authorization. Please connect again."
                                    AuthErrorReason.REFRESH_FAILED ->
                                        "The session expired. Please connect again."
                                    AuthErrorReason.UNKNOWN ->
                                        "Connection failed. Please try again."
                                },
                            )
                    }
                    // Consume the result (2026-10-17 fix): the repository
                    // replays the last result to new subscribers, so a
                    // consumed result must be cleared — otherwise a
                    // LATER Connect session (e.g. adding a second
                    // account) would get this stale Success and
                    // auto-navigate to the wrong account's import.
                    authRepository.clearOAuthResult()
                }
            } catch (e: Exception) {
                MoLogger.w(TAG, "OAuth result collection ended: ${e.javaClass.simpleName}")
            }
        }
    }

    /**
     * Builds the OAuth intent and marks the flow in-flight. The caller
     * (Compose) starts the intent with the Activity context.
     */
    fun createOAuthIntent(): Intent {
        _uiState.value = ConnectAccountUiState.Connecting
        return authRepository.createOAuthIntent()
    }

    /** Cancel a pending connect (e.g. backed out of the browser). */
    fun reset() {
        _uiState.value = ConnectAccountUiState.Idle
    }

    companion object {
        private const val TAG = "ConnectAccountViewModel"
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
