package com.speechsub.ui.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speechsub.data.local.CaptionEntity
import com.speechsub.ui.theme.InterFontFamily

/**
 * ExportScreen — lets users copy, export, and sync their captions.
 *
 * Export options:
 * 1. Copy full transcript to clipboard
 * 2. Export as SRT file (saved to Downloads)
 * 3. Export as plain TXT (saved to Downloads)
 * 4. Save to Firebase cloud (if logged in)
 *
 * Also shows a preview of the captions below the action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId     : Long,
    onNavigateBack: () -> Unit,
    viewModel     : ExportViewModel = hiltViewModel()
) {
    val project     by viewModel.project.collectAsStateWithLifecycle()
    val captions    by viewModel.captions.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on success or error
    LaunchedEffect(exportState) {
        when (val s = exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.clearState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar("⚠ ${s.message}")
                viewModel.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Export Captions", fontWeight = FontWeight.Bold)
                        Text(
                            project?.title ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost  = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D0D1A), Color(0xFF1A1040), Color(0xFF0D0D1A))
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // ── Stats card ──────────────────────────────────
                item {
                    StatsCard(
                        captionCount = captions.size,
                        totalDuration = if (captions.isNotEmpty()) captions.last().endTimeMs else 0L,
                        viewModel     = viewModel
                    )
                }

                // ── Export action cards ──────────────────────────
                item {
                    Text(
                        "Export Options",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.7f)
                    )
                }

                item {
                    val isLoading = exportState is ExportState.Loading

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportOptionCard(
                            icon        = Icons.Outlined.ContentCopy,
                            title       = "Copy Transcript",
                            description = "Copy full text to clipboard — paste anywhere",
                            enabled     = !isLoading,
                            onClick     = viewModel::copyTranscript
                        )
                        ExportOptionCard(
                            icon        = Icons.Outlined.Subtitles,
                            title       = "Export as SRT",
                            description = "Save .srt subtitle file to Downloads folder",
                            enabled     = !isLoading,
                            onClick     = viewModel::exportAsSrt
                        )
                        ExportOptionCard(
                            icon        = Icons.Outlined.TextSnippet,
                            title       = "Export as Text",
                            description = "Save .txt transcript file to Downloads folder",
                            enabled     = !isLoading,
                            onClick     = viewModel::exportAsText
                        )
                        ExportOptionCard(
                            icon        = Icons.Outlined.CloudUpload,
                            title       = "Save to Cloud",
                            description = if (viewModel.isLoggedIn)
                                "Sync captions to your Firebase account"
                            else
                                "Log in to enable cloud backup",
                            enabled     = !isLoading && viewModel.isLoggedIn,
                            onClick     = viewModel::syncToCloud,
                            tint        = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Loading indicator
                item {
                    AnimatedVisibility(
                        visible = exportState is ExportState.Loading,
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // ── Caption Preview ──────────────────────────────
                if (captions.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Caption Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.7f)
                        )
                    }

                    items(captions.take(20)) { caption ->
                        CaptionPreviewRow(caption = caption, viewModel = viewModel)
                    }

                    if (captions.size > 20) {
                        item {
                            Text(
                                "… and ${captions.size - 20} more captions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ============================================================
// STATS CARD
// ============================================================

@Composable
private fun StatsCard(captionCount: Int, totalDuration: Long, viewModel: ExportViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.25f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Captions", value = captionCount.toString())
            StatItem(label = "Duration", value = viewModel.formatSrtTime(totalDuration).substringBefore(","))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
    }
}

// ============================================================
// EXPORT OPTION CARD
// ============================================================

@Composable
private fun ExportOptionCard(
    icon       : ImageVector,
    title      : String,
    description: String,
    enabled    : Boolean,
    onClick    : () -> Unit,
    tint       : Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        onClick    = onClick,
        enabled    = enabled,
        modifier   = Modifier.fillMaxWidth(),
        shape      = RoundedCornerShape(16.dp),
        colors     = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
        ),
        elevation  = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(tint.copy(0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.65f))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                       else MaterialTheme.colorScheme.onSurface.copy(0.2f)
            )
        }
    }
}

// ============================================================
// CAPTION PREVIEW ROW
// ============================================================

@Composable
private fun CaptionPreviewRow(caption: CaptionEntity, viewModel: ExportViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text  = viewModel.formatSrtTime(caption.startTimeMs).substringBefore(","),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = InterFontFamily,
            color = MaterialTheme.colorScheme.primary.copy(0.7f),
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text  = caption.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}
