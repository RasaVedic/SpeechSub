package com.speechsub.ui.splash

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechsub.data.firebase.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SplashUiState — represents the possible states during the splash screen.
 */
sealed class SplashUiState {
    object Loading          : SplashUiState()
    object NavigateToHome   : SplashUiState()
    object NavigateToLogin  : SplashUiState()
    data class UpdateAvailable(val latestVersion: String) : SplashUiState()
}

/**
 * SplashViewModel — handles auth check and optional version check.
 *
 * Steps:
 * 1. Wait for animation (1.5s)
 * 2. Check remote config for latest version
 * 3. If update available → show dialog
 * 4. Otherwise → route based on auth state
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authService: AuthService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    // Current installed version — read from BuildConfig
    private val currentVersion = "1.0.0"

    // Play Store URL — replace with your actual app package ID
    private val playStoreUrl = "https://play.google.com/store/apps/details?id=com.speechsub"

    init {
        checkVersionAndAuth()
    }

    private fun checkVersionAndAuth() = viewModelScope.launch {
        // Wait for splash animation to play
        delay(2000L)

        // Version check — in production, fetch from Firebase Remote Config
        // or a simple JSON endpoint you control
        val latestVersion = fetchLatestVersion()

        if (latestVersion != null && isNewerVersion(latestVersion, currentVersion)) {
            // Show update dialog — user can dismiss and continue
            _uiState.value = SplashUiState.UpdateAvailable(latestVersion)
            // Navigation continues after user interacts with the dialog
        } else {
            navigateBasedOnAuth()
        }
    }

    /**
     * Checks Firebase Remote Config (or a JSON endpoint) for the latest version.
     * Returns null if check fails (app continues normally).
     */
    private suspend fun fetchLatestVersion(): String? {
        return try {
            // TODO: Replace with actual Firebase Remote Config:
            //   val remoteConfig = Firebase.remoteConfig
            //   remoteConfig.fetchAndActivate().await()
            //   remoteConfig.getString("latest_version")
            null  // Return null = no update check for now
        } catch (e: Exception) {
            null  // Don't block on version check failure
        }
    }

    /** Returns true if latestVersion is strictly newer than currentVersion */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts  = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun navigateBasedOnAuth() {
        if (authService.isLoggedIn) {
            _uiState.value = SplashUiState.NavigateToHome
        } else {
            _uiState.value = SplashUiState.NavigateToLogin
        }
    }

    /** User tapped "Update Now" */
    fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** User dismissed the update dialog — continue to app */
    fun skipUpdate() = viewModelScope.launch {
        navigateBasedOnAuth()
    }
}
