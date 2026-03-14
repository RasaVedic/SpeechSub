package com.speechsub.ui.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechsub.data.firebase.AuthService
import com.speechsub.data.local.CaptionEntity
import com.speechsub.data.local.VideoProjectEntity
import com.speechsub.data.repository.CaptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

/** Tracks the state of an export or copy operation */
sealed class ExportState {
    object Idle    : ExportState()
    object Loading : ExportState()
    data class Success(val message: String) : ExportState()
    data class Error(val message: String)   : ExportState()
}

/**
 * ExportViewModel — handles all export operations for a project.
 *
 * Supports:
 * - Copy full transcript to clipboard
 * - Export as SRT file (SubRip subtitle format)
 * - Export as plain TXT file
 * - Optional cloud sync to Firebase Firestore
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository : CaptionRepository,
    private val authService: AuthService,
    savedStateHandle       : SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    private val _project = MutableStateFlow<VideoProjectEntity?>(null)
    val project: StateFlow<VideoProjectEntity?> = _project.asStateFlow()

    private val _captions = MutableStateFlow<List<CaptionEntity>>(emptyList())
    val captions: StateFlow<List<CaptionEntity>> = _captions.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    /** True if user is logged in — enables cloud sync button */
    val isLoggedIn: Boolean get() = authService.isLoggedIn

    init {
        loadData()
    }

    private fun loadData() = viewModelScope.launch {
        _project.value   = repository.getProjectById(projectId)
        _captions.value  = repository.getCaptionsOnce(projectId)
    }

    // =================== COPY TO CLIPBOARD ===================

    /**
     * copyTranscript — copies the full plain-text transcript to clipboard.
     * Joins all caption texts with newlines.
     */
    fun copyTranscript() {
        viewModelScope.launch {
            val captions = repository.getCaptionsOnce(projectId)
            if (captions.isEmpty()) {
                _exportState.value = ExportState.Error("No captions to copy.")
                return@launch
            }
            val transcript = captions.joinToString("\n") { it.text }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("SpeechSub Transcript", transcript))
            _exportState.value = ExportState.Success("Transcript copied to clipboard!")
        }
    }

    // =================== EXPORT SRT ===================

    /**
     * exportAsSrt — writes a standard SRT subtitle file.
     *
     * SRT format:
     *   1
     *   00:00:01,000 --> 00:00:04,500
     *   Hello, world!
     *
     *   2
     *   00:00:05,000 --> ...
     */
    fun exportAsSrt() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val captions  = repository.getCaptionsOnce(projectId)
                val project   = repository.getProjectById(projectId)
                if (captions.isEmpty()) throw Exception("No captions to export.")

                val srtContent = buildSrt(captions)
                val fileName   = "${project?.title ?: "transcript"}.srt"

                saveToDownloads(fileName, "text/plain", srtContent.toByteArray(Charsets.UTF_8))
                _exportState.value = ExportState.Success("SRT saved to Downloads: $fileName")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
            }
        }
    }

    // =================== EXPORT TXT ===================

    /**
     * exportAsText — writes a plain-text transcript.
     *
     * Format:
     *   [00:00:01] Caption text here
     *   [00:00:05] Next caption...
     */
    fun exportAsText() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val captions = repository.getCaptionsOnce(projectId)
                val project  = repository.getProjectById(projectId)
                if (captions.isEmpty()) throw Exception("No captions to export.")

                val content  = buildPlainText(captions)
                val fileName = "${project?.title ?: "transcript"}.txt"

                saveToDownloads(fileName, "text/plain", content.toByteArray(Charsets.UTF_8))
                _exportState.value = ExportState.Success("Text file saved to Downloads: $fileName")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
            }
        }
    }

    // =================== CLOUD SYNC ===================

    /**
     * syncToCloud — uploads captions to Firestore under the current user's account.
     * Only available when logged in.
     */
    fun syncToCloud() {
        val userId = authService.currentUser?.uid ?: run {
            _exportState.value = ExportState.Error("Please log in to use cloud sync.")
            return
        }
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                repository.syncToCloud(projectId, userId)
                _exportState.value = ExportState.Success("Captions saved to cloud successfully!")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Cloud sync failed: ${e.message}")
            }
        }
    }

    /** Reset state after snackbar is shown */
    fun clearState() { _exportState.value = ExportState.Idle }

    // =================== HELPERS ===================

    /** Build SRT format string from captions list */
    private fun buildSrt(captions: List<CaptionEntity>): String {
        return captions.mapIndexed { i, caption ->
            val start = formatSrtTime(caption.startTimeMs)
            val end   = formatSrtTime(caption.endTimeMs)
            "${i + 1}\n$start --> $end\n${caption.text}\n"
        }.joinToString("\n")
    }

    /** Build plain text with timestamps */
    private fun buildPlainText(captions: List<CaptionEntity>): String {
        return captions.joinToString("\n") { caption ->
            "[${formatSrtTime(caption.startTimeMs)}] ${caption.text}"
        }
    }

    /**
     * formatSrtTime — converts milliseconds to SRT timestamp format.
     * Example: 3661234 → "01:01:01,234"
     */
    fun formatSrtTime(ms: Long): String {
        val hours   = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        val seconds = (ms % 60_000) / 1_000
        val millis  = ms % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    /**
     * saveToDownloads — writes bytes to the device Downloads folder.
     *
     * Uses MediaStore on Android 10+ (scoped storage).
     * Falls back to direct File I/O on older versions.
     */
    private fun saveToDownloads(fileName: String, mimeType: String, bytes: ByteArray) {
        val stream: OutputStream

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use MediaStore (no WRITE_EXTERNAL_STORAGE needed)
            val resolver = context.contentResolver
            val values   = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Could not create file in Downloads")

            stream = resolver.openOutputStream(uri)
                ?: throw Exception("Could not open output stream")

            stream.use { it.write(bytes) }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            // Android 9 and below — direct file access
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val file = java.io.File(downloadsDir, fileName)
            file.writeBytes(bytes)
        }
    }
}
