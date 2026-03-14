package com.speechsub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.speechsub.ui.navigation.SpeechSubNavGraph
import com.speechsub.ui.theme.SpeechSubTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — the single activity host for the entire app.
 *
 * Uses Jetpack Navigation Compose with a single NavGraph, following the
 * single-activity MVVM architecture pattern.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Edge-to-edge layout — content draws behind status and navigation bars
        enableEdgeToEdge()

        setContent {
            SpeechSubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Main navigation graph — manages all screen transitions
                    SpeechSubNavGraph()
                }
            }
        }
    }
}
