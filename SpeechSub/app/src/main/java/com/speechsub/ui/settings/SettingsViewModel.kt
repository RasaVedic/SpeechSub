package com.speechsub.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechsub.data.firebase.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SettingsViewModel — manages user preferences.
 *
 * Preferences are stored in SharedPreferences (lightweight key-value store).
 * Firebase Auth is used for account management (sign out).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authService: AuthService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PREFS_NAME          = "speechsub_prefs"
        private const val KEY_DEFAULT_LANGUAGE = "default_language"
        private const val KEY_DEFAULT_FONT     = "default_font"
        private const val KEY_FONT_SIZE        = "default_font_size"
        private const val KEY_CLOUD_SYNC       = "cloud_sync_enabled"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Auth info ──────────────────────────────────────────
    val isLoggedIn: Boolean get() = authService.isLoggedIn
    val userEmail : String  get() = authService.currentUser?.email ?: "Guest"
    val userName  : String  get() = authService.currentUser?.displayName ?: "User"

    // ── Preferences state ──────────────────────────────────
    private val _defaultLanguage = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_LANGUAGE, "en-IN") ?: "en-IN"
    )
    val defaultLanguage: StateFlow<String> = _defaultLanguage.asStateFlow()

    private val _defaultFont = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_FONT, "inter") ?: "inter"
    )
    val defaultFont: StateFlow<String> = _defaultFont.asStateFlow()

    private val _defaultFontSize = MutableStateFlow(
        prefs.getFloat(KEY_FONT_SIZE, 16f)
    )
    val defaultFontSize: StateFlow<Float> = _defaultFontSize.asStateFlow()

    private val _cloudSyncEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_CLOUD_SYNC, false)
    )
    val cloudSyncEnabled: StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()

    // ── Snackbar ───────────────────────────────────────────
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // ── Signed out signal ──────────────────────────────────
    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    // =================== SETTERS ===================

    fun setDefaultLanguage(language: String) {
        _defaultLanguage.value = language
        prefs.edit().putString(KEY_DEFAULT_LANGUAGE, language).apply()
    }

    fun setDefaultFont(font: String) {
        _defaultFont.value = font
        prefs.edit().putString(KEY_DEFAULT_FONT, font).apply()
    }

    fun setDefaultFontSize(size: Float) {
        _defaultFontSize.value = size
        prefs.edit().putFloat(KEY_FONT_SIZE, size).apply()
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        _cloudSyncEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CLOUD_SYNC, enabled).apply()
    }

    // =================== ACCOUNT ===================

    /** Sign out from Firebase and trigger navigation back to Login */
    fun signOut() {
        authService.signOut()
        _signedOut.value = true
    }

    /** Clear snackbar */
    fun clearSnackbar() { _snackbarMessage.value = null }
}

// ============================================================
// LANGUAGE OPTIONS — shown in the settings picker
// ============================================================
val languageOptions = listOf(
    "en-IN"   to "English (India)",
    "en-US"   to "English (US)",
    "hi-IN"   to "Hindi",
    "hi-IN"   to "Hinglish (Auto-detect)",
)

// ============================================================
// FONT OPTIONS
// ============================================================
val fontOptions = listOf(
    "inter"       to "Inter",
    "nunito"      to "Nunito",
    "hind"        to "Hind (Hindi)",
    "roboto_mono" to "Roboto Mono",
)
