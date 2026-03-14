package com.speechsub.ui.home

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed class ImportState {
    object Idle    : ImportState()
    object Loading : ImportState()
    data class Ready(val projectId: Long) : ImportState()
    data class Error(val message: String)  : ImportState()
}

/**
 * HomeViewModel — manages project list and video import.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CaptionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Stream of all video projects for display in the list */
    val projects: StateFlow<List<VideoProjectEntity>> = repository
        .getAllProjects()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    /**
     * importVideo — reads metadata from the selected URI, creates a project,
     * then navigates to the processing screen.
     */
    fun importVideo(uri: Uri, language: String) = viewModelScope.launch {
        _importState.value = ImportState.Loading
        try {
            // Read video metadata from MediaStore
            val (title, durationMs) = getVideoMetadata(uri)

            // Create project in Room database
            val project = VideoProjectEntity(
                title      = title,
                filePath   = uri.toString(),
                durationMs = durationMs,
                language   = language,
            )
            val projectId = repository.createProject(project)

            _importState.value = ImportState.Ready(projectId)
        } catch (e: Exception) {
            _importState.value = ImportState.Error("Could not read video: ${e.message}")
        }
    }

    /** Delete a project and all its captions */
    fun deleteProject(project: VideoProjectEntity) = viewModelScope.launch {
        repository.deleteProject(project)
    }

    /**
     * getVideoMetadata — reads title and duration from MediaStore
     * using the content URI provided by the file picker.
     */
    private fun getVideoMetadata(uri: Uri): Pair<String, Long> {
        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx     = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val durationIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val name     = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Untitled" else "Untitled"
                val duration = if (durationIdx >= 0) cursor.getLong(durationIdx) else 0L
                // Strip file extension from display name
                val cleanName = name.substringBeforeLast(".")
                return cleanName to duration
            }
        }
        // Fallback: use the last segment of the URI path
        return (uri.lastPathSegment ?: "Untitled") to 0L
    }
}
