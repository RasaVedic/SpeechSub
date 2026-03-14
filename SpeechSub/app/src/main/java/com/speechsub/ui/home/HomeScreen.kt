package com.speechsub.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speechsub.data.local.VideoProjectEntity

/**
 * HomeScreen — shows recent projects and the video import button.
 *
 * Layout:
 * - Top bar with title and settings button
 * - Large "Import Video" FAB (floating action button)
 * - Scrollable list of recent projects
 * - Each project card shows title, duration, and language
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProjectSelected  : (Long) -> Unit,
    onStartProcessing  : (Long) -> Unit,
    onNavigateSettings : () -> Unit,
    viewModel          : HomeViewModel = hiltViewModel()
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var pendingVideoUri    by remember { mutableStateOf<Uri?>(null) }

    // File picker launcher — opens system video picker
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingVideoUri = it
            showLanguageDialog = true  // Ask which language before processing
        }
    }

    // Navigate to processing when import is ready
    LaunchedEffect(importState) {
        if (importState is ImportState.Ready) {
            onStartProcessing((importState as ImportState.Ready).projectId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SpeechSub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Video Captions", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            // Primary action — Import Video
            ExtendedFloatingActionButton(
                onClick = { videoPicker.launch("video/*") },
                icon    = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                text    = { Text("Import Video") },
                containerColor = MaterialTheme.colorScheme.primary,
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0D0D1A), Color(0xFF1A1040), Color(0xFF0D0D1A)))
                )
                .padding(paddingValues)
        ) {
            if (projects.isEmpty()) {
                // Empty state
                EmptyHomeState(
                    onImportClick = { videoPicker.launch("video/*") },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Recent Projects",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project   = project,
                            onClick   = { onProjectSelected(project.id) },
                            onDelete  = { viewModel.deleteProject(project) }
                        )
                    }
                    // Bottom padding for FAB
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // Loading overlay during import
            if (importState is ImportState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Reading video…")
                        }
                    }
                }
            }
        }
    }

    // Language selection dialog
    if (showLanguageDialog) {
        LanguagePickerDialog(
            onLanguageSelected = { language ->
                showLanguageDialog = false
                pendingVideoUri?.let { uri ->
                    viewModel.importVideo(uri, language)
                    pendingVideoUri = null
                }
            },
            onDismiss = {
                showLanguageDialog = false
                pendingVideoUri = null
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project : VideoProjectEntity,
    onClick : () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = formatDuration(project.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text  = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text  = project.language.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Status badge
            if (project.isProcessed) {
                AssistChip(
                    onClick = {},
                    label   = { Text("Done", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp)) },
                    colors  = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
            } else {
                AssistChip(
                    onClick = {},
                    label   = { Text("Draft", style = MaterialTheme.typography.labelSmall) },
                    colors  = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete Project?") },
            text   = { Text("This will permanently delete \"${project.title}\" and all its captions.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyHomeState(onImportClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎬", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "No projects yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Import a video to generate captions automatically",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onImportClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.VideoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text("Import Video")
        }
    }
}

@Composable
private fun LanguagePickerDialog(
    onLanguageSelected: (String) -> Unit,
    onDismiss         : () -> Unit
) {
    val languages = listOf(
        "en-IN"    to "🇬🇧 English",
        "hi-IN"    to "🇮🇳 Hindi",
        "hi-IN"    to "🔀 Hinglish (Hindi + English)",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title  = { Text("Select Language") },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Choose the primary language spoken in the video:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                languages.forEach { (code, label) ->
                    OutlinedButton(
                        onClick   = { onLanguageSelected(code) },
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp)
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
