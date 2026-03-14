package com.speechsub.ui.processing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ProcessingScreen — speech recognition progress view.
 *
 * Shows:
 * - Animated circular progress indicator
 * - Real-time progress percentage
 * - Last recognized caption text (as captions come in)
 * - Cancel button
 */
@Composable
fun ProcessingScreen(
    projectId           : Long,
    onProcessingComplete: () -> Unit,
    onCancel            : () -> Unit,
    viewModel           : ProcessingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Trigger processing on first composition
    LaunchedEffect(projectId) {
        viewModel.startProcessing(projectId)
    }

    // Navigate when complete
    LaunchedEffect(uiState) {
        if (uiState is ProcessingUiState.Complete) onProcessingComplete()
    }

    // Pulsing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D1A), Color(0xFF1A1040)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Animated processing icon
            Box(
                modifier = Modifier.scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress    = { (uiState as? ProcessingUiState.Processing)?.progress?.div(100f) ?: 0f },
                    modifier    = Modifier.size(120.dp),
                    strokeWidth = 8.dp,
                    color       = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text  = "${(uiState as? ProcessingUiState.Processing)?.progress ?: 0}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text  = "Recognizing Speech…",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text  = when (val s = uiState) {
                    is ProcessingUiState.Processing -> s.statusMessage
                    is ProcessingUiState.Error      -> "Error: ${s.message}"
                    else -> "Starting…"
                },
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            // Live caption preview
            val lastCaption = (uiState as? ProcessingUiState.Processing)?.lastCaption
            AnimatedVisibility(!lastCaption.isNullOrBlank()) {
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Live Caption",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = lastCaption ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // Progress bar
            LinearProgressIndicator(
                progress  = { (uiState as? ProcessingUiState.Processing)?.progress?.div(100f) ?: 0f },
                modifier  = Modifier.fillMaxWidth().height(6.dp),
                color     = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text  = "This may take a few minutes for long videos.\nDo not close the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = {
                    viewModel.cancelProcessing()
                    onCancel()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    }
}
