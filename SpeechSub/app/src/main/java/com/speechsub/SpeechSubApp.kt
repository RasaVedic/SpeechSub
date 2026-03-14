package com.speechsub

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * SpeechSubApp — Application class
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 * across the entire application. This is required for Hilt to work.
 */
@HiltAndroidApp
class SpeechSubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application-level initialization goes here
        // (e.g., logging libraries, crash reporting, etc.)
    }
}
