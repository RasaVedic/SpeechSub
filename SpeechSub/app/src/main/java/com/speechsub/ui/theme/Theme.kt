package com.speechsub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary         = Color(0xFF7B68EE),
    onPrimary       = Color(0xFF1A1A2E),
    primaryContainer= Color(0xFF3D3580),
    onPrimaryContainer = Color(0xFFD4CFFF),
    secondary       = Color(0xFF9B7FD4),
    onSecondary     = Color(0xFF1A1A2E),
    secondaryContainer = Color(0xFF3A2C6B),
    onSecondaryContainer = Color(0xFFDDD0FF),
    tertiary        = Color(0xFF64B5F6),
    onTertiary      = Color(0xFF001E3C),
    background      = Color(0xFF0D0D1A),
    onBackground    = Color(0xFFE8E6F0),
    surface         = Color(0xFF14142B),
    onSurface       = Color(0xFFE8E6F0),
    surfaceVariant  = Color(0xFF1E1E3A),
    onSurfaceVariant= Color(0xFFB8B5CC),
    outline         = Color(0xFF6B6880),
    error           = Color(0xFFCF6679),
    onError         = Color(0xFF3A0016),
)

private val LightColorScheme = lightColorScheme(
    primary         = Color(0xFF5B4FCF),
    onPrimary       = Color(0xFFFFFFFF),
    primaryContainer= Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1A0083),
    secondary       = Color(0xFF7B5EA7),
    onSecondary     = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECDCFF),
    onSecondaryContainer = Color(0xFF2D1160),
    tertiary        = Color(0xFF1A73E8),
    onTertiary      = Color(0xFFFFFFFF),
    background      = Color(0xFFF8F6FF),
    onBackground    = Color(0xFF1A1830),
    surface         = Color(0xFFFFFFFF),
    onSurface       = Color(0xFF1A1830),
    surfaceVariant  = Color(0xFFECE9F8),
    onSurfaceVariant= Color(0xFF49465A),
    outline         = Color(0xFF79768A),
    error           = Color(0xFFB00020),
    onError         = Color(0xFFFFFFFF),
)

@Composable
fun SpeechSubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = SpeechSubTypography,
        shapes       = SpeechSubShapes,
        content      = content
    )
}
