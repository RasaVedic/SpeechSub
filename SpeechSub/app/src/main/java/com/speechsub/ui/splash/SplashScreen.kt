package com.speechsub.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speechsub.ui.theme.InterFontFamily

/**
 * SplashScreen — animated launch screen.
 *
 * Shows the logo with a fade+scale animation, then:
 * 1. Checks if a newer app version is available (shows update dialog if yes)
 * 2. Routes to Home (logged in) or Login (not logged in)
 */
@Composable
fun SplashScreen(
    onNavigateToHome : () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel        : SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Animate logo on entry
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue    = if (visible) 1f else 0f,
        animationSpec  = tween(800, easing = EaseOut),
        label          = "logoAlpha"
    )
    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "logoScale"
    )

    LaunchedEffect(Unit) { visible = true }

    // Navigate after animation + checks complete
    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashUiState.NavigateToHome  -> onNavigateToHome()
            is SplashUiState.NavigateToLogin -> onNavigateToLogin()
            else -> {}
        }
    }

    // Background with gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        Color(0xFF1A1040),
                        Color(0xFF0D0D1A),
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        ) {
            // App icon / logo text
            Text(
                text  = "🎬",
                fontSize = 72.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text       = "SpeechSub",
                style      = MaterialTheme.typography.displaySmall,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Video Captions, Made Easy",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(48.dp))

            // Loading indicator while checking auth/version
            if (uiState is SplashUiState.Loading) {
                CircularProgressIndicator(
                    color     = MaterialTheme.colorScheme.primary,
                    modifier  = Modifier.size(32.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        // Update available dialog
        if (uiState is SplashUiState.UpdateAvailable) {
            val state = uiState as SplashUiState.UpdateAvailable
            AlertDialog(
                onDismissRequest = { viewModel.skipUpdate() },
                title            = { Text("Update Available") },
                text             = {
                    Text(
                        "Version ${state.latestVersion} is available. " +
                        "Update for the latest features and bug fixes."
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.openPlayStore() }) {
                        Text("Update Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.skipUpdate() }) {
                        Text("Later")
                    }
                }
            )
        }
    }
}
