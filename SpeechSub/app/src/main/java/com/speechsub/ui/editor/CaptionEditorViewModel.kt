package com.speechsub.ui.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechsub.data.local.CaptionEntity
import com.speechsub.data.local.VideoProjectEntity
import com.speechsub.data.repository.CaptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CaptionEditorViewModel — manages the caption timeline editor state.
 *
 * Responsibilities:
 * - Load the project and its captions from Room
 * - Handle text edits, color changes, font changes
 * - Handle split and merge operations
 * - Save changes back to Room in real time
 */
@HiltViewModel
class CaptionEditorViewModel @Inject constructor(
    private val repository: CaptionRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Project ID passed via navigation argument
    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    // =================== STATE ===================

    private val _project = MutableStateFlow<VideoProjectEntity?>(null)
    val project: StateFlow<VideoProjectEntity?> = _project.asStateFlow()

    /** Live stream of captions for this project — auto-updates on DB changes */
    val captions: StateFlow<List<CaptionEntity>> = repository
        .getCaptionsForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Which caption is currently selected / being edited */
    private val _selectedCaptionId = MutableStateFlow<Long?>(null)
    val selectedCaptionId: StateFlow<Long?> = _selectedCaptionId.asStateFlow()

    /** Snackbar message to show (null = nothing) */
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadProject()
    }

    private fun loadProject() = viewModelScope.launch {
        _project.value = repository.getProjectById(projectId)
    }

    // =================== CAPTION SELECTION ===================

    /** Select a caption for editing */
    fun selectCaption(captionId: Long?) {
        _selectedCaptionId.value = captionId
    }

    // =================== TEXT EDITING ===================

    /**
     * updateCaptionText — saves a new text value for a caption.
     *
     * Called when the user finishes typing in the inline editor.
     */
    fun updateCaptionText(caption: CaptionEntity, newText: String) {
        if (newText == caption.text) return  // No change, skip DB write
        viewModelScope.launch {
            repository.updateCaption(caption.copy(text = newText))
        }
    }

    // =================== TIMESTAMPS ===================

    /**
     * updateCaptionTimestamps — allows editing start/end times.
     */
    fun updateCaptionTimestamps(caption: CaptionEntity, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            repository.updateCaption(caption.copy(startTimeMs = startMs, endTimeMs = endMs))
        }
    }

    // =================== STYLING ===================

    /**
     * updateCaptionColor — changes the text color for a caption.
     *
     * @param hexColor  a hex color string like "#FF5252"
     */
    fun updateCaptionColor(caption: CaptionEntity, hexColor: String) {
        viewModelScope.launch {
            repository.updateCaption(caption.copy(textColor = hexColor))
        }
    }

    /**
     * updateCaptionFont — changes the font family.
     * Supported values: "inter", "nunito", "hind", "roboto_mono"
     */
    fun updateCaptionFont(caption: CaptionEntity, fontFamily: String) {
        viewModelScope.launch {
            repository.updateCaption(caption.copy(fontFamily = fontFamily))
        }
    }

    /** Toggle bold styling */
    fun toggleBold(caption: CaptionEntity) {
        viewModelScope.launch {
            repository.updateCaption(caption.copy(isBold = !caption.isBold))
        }
    }

    /** Toggle italic styling */
    fun toggleItalic(caption: CaptionEntity) {
        viewModelScope.launch {
            repository.updateCaption(caption.copy(isItalic = !caption.isItalic))
        }
    }

    /** Update font size */
    fun updateFontSize(caption: CaptionEntity, size: Float) {
        viewModelScope.launch {
            repository.updateCaption(caption.copy(fontSize = size))
        }
    }

    // =================== SPLIT & MERGE ===================

    /**
     * splitCaption — divides a caption into two halves at the midpoint.
     *
     * Example: "Hello world" at 0-4000ms becomes:
     *   "Hello"  at 0-2000ms
     *   "world"  at 2000-4000ms
     */
    fun splitCaption(caption: CaptionEntity) = viewModelScope.launch {
        val words = caption.text.trim().split(" ")
        if (words.size < 2) {
            _snackbarMessage.value = "Caption must have at least 2 words to split"
            return@launch
        }

        val midWord   = words.size / 2
        val firstText = words.take(midWord).joinToString(" ")
        val secondText= words.drop(midWord).joinToString(" ")
        val midTimeMs = caption.startTimeMs + (caption.endTimeMs - caption.startTimeMs) / 2

        val currentList = repository.getCaptionsOnce(projectId)

        // Delete original
        repository.deleteCaption(caption)

        // Rebuild list with the two new captions inserted at the right position
        val updatedList = currentList.toMutableList()
        val insertAt = updatedList.indexOfFirst { it.id == caption.id }
        updatedList.removeAll { it.id == caption.id }

        val firstCaption  = caption.copy(id = 0, text = firstText, endTimeMs = midTimeMs, index = insertAt)
        val secondCaption = caption.copy(id = 0, text = secondText, startTimeMs = midTimeMs, index = insertAt + 1)

        repository.insertCaption(firstCaption)
        repository.insertCaption(secondCaption)

        // Re-index all subsequent captions
        reIndexCaptions()
        _snackbarMessage.value = "Caption split into two"
    }

    /**
     * mergeWithNext — combines this caption with the next one.
     *
     * The combined text uses the start time of this caption and
     * the end time of the next.
     */
    fun mergeWithNext(caption: CaptionEntity) = viewModelScope.launch {
        val allCaptions = repository.getCaptionsOnce(projectId)
        val nextCaption = allCaptions.firstOrNull { it.index == caption.index + 1 }

        if (nextCaption == null) {
            _snackbarMessage.value = "No next caption to merge with"
            return@launch
        }

        val merged = caption.copy(
            text       = "${caption.text} ${nextCaption.text}".trim(),
            endTimeMs  = nextCaption.endTimeMs
        )

        repository.updateCaption(merged)
        repository.deleteCaption(nextCaption)
        reIndexCaptions()
        _snackbarMessage.value = "Captions merged"
    }

    // =================== DELETE ===================

    /** Delete a single caption */
    fun deleteCaption(caption: CaptionEntity) = viewModelScope.launch {
        repository.deleteCaption(caption)
        if (_selectedCaptionId.value == caption.id) {
            _selectedCaptionId.value = null
        }
        reIndexCaptions()
    }

    // =================== HELPERS ===================

    /**
     * reIndexCaptions — fixes the index field after splits, merges, or deletes.
     * Room keeps the rows but we need sequential 0-based indices.
     */
    private suspend fun reIndexCaptions() {
        val allCaptions = repository.getCaptionsOnce(projectId)
        allCaptions.forEachIndexed { newIndex, caption ->
            if (caption.index != newIndex) {
                repository.updateCaption(caption.copy(index = newIndex))
            }
        }
    }

    /** Clear snackbar message after it's been shown */
    fun clearSnackbar() { _snackbarMessage.value = null }

    /** Format milliseconds to SRT-style timestamp: HH:MM:SS,mmm */
    fun formatTimestamp(ms: Long): String {
        val hours   = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        val seconds = (ms % 60_000) / 1_000
        val millis  = ms % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }
}
