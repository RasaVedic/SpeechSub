package com.speechsub.ui.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speechsub.data.local.CaptionEntity
import com.speechsub.ui.theme.HindFontFamily
import com.speechsub.ui.theme.InterFontFamily
import com.speechsub.ui.theme.NunitoFontFamily

// ============================================================
// CAPTION COLOR PALETTE — shown in the color picker row
// ============================================================
private val captionColors = listOf(
    "#FFFFFF" to "White",
    "#FFE066" to "Yellow",
    "#64FFDA" to "Cyan",
    "#FF80AB" to "Pink",
    "#FFAB40" to "Orange",
    "#69F0AE" to "Green",
    "#FF5252" to "Red",
    "#82B1FF" to "Blue",
)

// ============================================================
// FONT OPTIONS — shown in the font picker
// ============================================================
private val fontOptions = listOf(
    "inter"        to "Inter",
    "nunito"       to "Nunito",
    "hind"         to "Hind (Hindi)",
    "roboto_mono"  to "Roboto Mono",
)

/**
 * CaptionEditorScreen — the main caption timeline editor.
 *
 * Layout:
 * ┌──────────────────────────────────────────┐
 * │  Top bar (back, title, export button)    │
 * ├──────────────────────────────────────────┤
 * │  Caption timeline list (scrollable)      │
 * │   ┌──────────────────────────────────┐   │
 * │   │  [00:00:01,000 → 00:00:04,500]   │   │
 * │   │  Caption text (tap to edit)      │   │
 * │   │  [Bold] [Italic] [Color] [Font]  │   │
 * │   │  [Split] [Merge] [Delete]        │   │
 * │   └──────────────────────────────────┘   │
 * └──────────────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionEditorScreen(
    projectId         : Long,
    onNavigateBack    : () -> Unit,
    onNavigateExport  : (Long) -> Unit,
    onNavigateSettings: () -> Unit = {},
    viewModel         : CaptionEditorViewModel = hiltViewModel()
) {
    val project          by viewModel.project.collectAsStateWithLifecycle()
    val captions         by viewModel.captions.collectAsStateWithLifecycle()
    val selectedId       by viewModel.selectedCaptionId.collectAsStateWithLifecycle()
    val snackbarMessage  by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState         = rememberLazyListState()

    // Show snackbar when message arrives
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project?.title ?: "Caption Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text  = "${captions.size} captions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export button
                    IconButton(onClick = { onNavigateExport(projectId) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    // Settings button
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { paddingValues ->

        // Full-screen gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D0D1A), Color(0xFF1A1040), Color(0xFF0D0D1A))
                    )
                )
                .padding(paddingValues)
        ) {
            if (captions.isEmpty()) {
                // Empty state
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ClosedCaption,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No captions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                        )
                        Text(
                            "Process the video to generate captions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.35f)
                        )
                    }
                }
            } else {
                // Caption timeline list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(captions, key = { _, c -> c.id }) { index, caption ->
                        CaptionCard(
                            caption     = caption,
                            index       = index,
                            isSelected  = selectedId == caption.id,
                            viewModel   = viewModel,
                            onSelect    = { viewModel.selectCaption(caption.id) },
                            onDeselect  = { viewModel.selectCaption(null) }
                        )
                    }
                    // Bottom padding so last card isn't behind nav bar
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ============================================================
// CAPTION CARD — one row in the timeline
// ============================================================

/**
 * CaptionCard — displays a single caption with inline editing.
 *
 * Collapsed view:   timestamp + first line of text
 * Expanded view:    editable text + styling controls
 */
@Composable
fun CaptionCard(
    caption   : CaptionEntity,
    index     : Int,
    isSelected: Boolean,
    viewModel : CaptionEditorViewModel,
    onSelect  : () -> Unit,
    onDeselect: () -> Unit,
) {
    // Local edit text state (synced to DB on save)
    var editText by remember(caption.id) { mutableStateOf(caption.text) }
    val focusRequester = remember { FocusRequester() }

    // Highlight color for selected card
    val cardColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable {
                if (isSelected) onDeselect() else onSelect()
            },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Row 1: Index + Timestamp ──────────────────
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Segment number badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Timestamp
                Text(
                    text  = "${viewModel.formatTimestamp(caption.startTimeMs)}  →  ${viewModel.formatTimestamp(caption.endTimeMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Row 2: Caption Text (editable when selected) ──
            if (isSelected) {
                // Inline text editor
                LaunchedEffect(isSelected) {
                    focusRequester.requestFocus()
                }

                BasicTextField(
                    value          = editText,
                    onValueChange  = { editText = it },
                    textStyle = TextStyle(
                        color      = parseHexColor(caption.textColor),
                        fontFamily = when (caption.fontFamily) {
                            "nunito" -> NunitoFontFamily
                            "hind"   -> HindFontFamily
                            else     -> InterFontFamily
                        },
                        fontWeight = if (caption.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle  = if (caption.isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontSize   = caption.fontSize.sp,
                        lineHeight = (caption.fontSize * 1.4f).sp
                    ),
                    cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )

                Spacer(Modifier.height(8.dp))

                // ── Save / Cancel row ──
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.updateCaptionText(caption, editText)
                            onDeselect()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            editText = caption.text // revert
                            onDeselect()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))

                // ── Styling Controls ──────────────────────────────────

                // Bold / Italic toggles
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = caption.isBold,
                        onClick  = { viewModel.toggleBold(caption) },
                        label    = { Text("B", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = caption.isItalic,
                        onClick  = { viewModel.toggleItalic(caption) },
                        label    = { Text("I", fontStyle = FontStyle.Italic) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Color picker row
                Text(
                    "Caption Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(captionColors) { (hex, name) ->
                        ColorDot(
                            hex      = hex,
                            name     = name,
                            selected = caption.textColor.equals(hex, ignoreCase = true),
                            onClick  = { viewModel.updateCaptionColor(caption, hex) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Font picker row
                Text(
                    "Font",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fontOptions) { (key, label) ->
                        FilterChip(
                            selected = caption.fontFamily == key,
                            onClick  = { viewModel.updateCaptionFont(caption, key) },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))

                // ── Timeline Actions ──────────────────────────────────
                Text(
                    "Actions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.splitCaption(caption) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Split", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { viewModel.mergeWithNext(caption) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.MergeType, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Merge", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { viewModel.deleteCaption(caption) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(MaterialTheme.colorScheme.error.copy(0.4f))
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelSmall)
                    }
                }

            } else {
                // Collapsed view — just show the text
                Text(
                    text  = caption.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = parseHexColor(caption.textColor).copy(alpha = 0.9f),
                    fontFamily = when (caption.fontFamily) {
                        "nunito" -> NunitoFontFamily
                        "hind"   -> HindFontFamily
                        else     -> InterFontFamily
                    },
                    fontWeight = if (caption.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle  = if (caption.isItalic) FontStyle.Italic else FontStyle.Normal,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================
// COLOR DOT — a tappable circle in the color picker
// ============================================================

@Composable
private fun ColorDot(hex: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val color = parseHexColor(hex)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected: $name",
                tint     = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ============================================================
// HELPER — parse a hex color string to a Compose Color
// ============================================================

internal fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val argb = when (cleaned.length) {
            6 -> "FF$cleaned".toLong(16).toInt()
            8 -> cleaned.toLong(16).toInt()
            else -> 0xFFFFFFFF.toInt()
        }
        Color(argb)
    } catch (_: Exception) {
        Color.White
    }
}
