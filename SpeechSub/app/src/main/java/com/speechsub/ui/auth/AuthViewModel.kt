package com.speechsub.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechsub.data.firebase.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object PasswordResetSent : AuthUiState()
}

/**
 * AuthViewModel — handles login, sign-up, and password reset.
 *
 * Shared by both LoginScreen and SignUpScreen.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Sign in with email and password */
    fun signIn(email: String, password: String) {
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                authService.signInWithEmail(email.trim(), password)
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.message?.substringAfter(": ") ?: "Sign in failed. Please try again."
                )
            }
        }
    }

    /** Create a new account */
    fun signUp(email: String, password: String, displayName: String = "") {
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = authService.signUpWithEmail(email.trim(), password)
                // Update display name if provided
                if (displayName.isNotBlank()) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                    user.updateProfile(profileUpdates)
                }
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.message?.substringAfter(": ") ?: "Sign up failed. Please try again."
                )
            }
        }
    }

    /** Send password reset email */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email address first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                authService.sendPasswordReset(email.trim())
                _uiState.value = AuthUiState.PasswordResetSent
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Could not send reset email. Check the address and try again.")
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isBlank()           -> { _uiState.value = AuthUiState.Error("Email cannot be empty."); false }
            !email.contains("@")      -> { _uiState.value = AuthUiState.Error("Please enter a valid email."); false }
            password.length < 6       -> { _uiState.value = AuthUiState.Error("Password must be at least 6 characters."); false }
            else -> true
        }
    }
}
