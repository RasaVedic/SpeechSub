package com.speechsub.ui.processing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechsub.data.local.SpeechProcessingService
import com.speechsub.data.repository.CaptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProcessingUiState {
    object Idle    : ProcessingUiState()
    data class Processing(
        val progress     : Int    = 0,
        val statusMessage: String = "Starting…",
        val lastCaption  : String = ""
    ) : ProcessingUiState()
    object Complete : ProcessingUiState()
    data class Error(val message: String) : ProcessingUiState()
}

/**
 * ProcessingViewModel — starts the foreground service and receives
 * broadcast updates from it.
 *
 * Uses Android BroadcastReceiver to get progress updates because
 * the processing runs in a separate Service context.
 */
@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val repository: CaptionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Idle)
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    // BroadcastReceiver — receives progress from SpeechProcessingService
    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                SpeechProcessingService.BROADCAST_PROGRESS -> {
                    val progress = intent.getIntExtra(SpeechProcessingService.EXTRA_PROGRESS, 0)
                    val text     = intent.getStringExtra(SpeechProcessingService.EXTRA_SEGMENT_TEXT) ?: ""
                    _uiState.value = ProcessingUiState.Processing(
                        progress      = progress,
                        statusMessage = "Processing audio…",
                        lastCaption   = text
                    )
                }
                SpeechProcessingService.BROADCAST_COMPLETE -> {
                    _uiState.value = ProcessingUiState.Complete
                    unregisterReceiver()
                }
                SpeechProcessingService.BROADCAST_ERROR -> {
                    val error = intent.getStringExtra(SpeechProcessingService.EXTRA_ERROR_MSG) ?: "Unknown error"
                    _uiState.value = ProcessingUiState.Error(error)
                    unregisterReceiver()
                }
            }
        }
    }

    private var receiverRegistered = false

    /**
     * startProcessing — registers broadcast receiver and launches
     * the foreground service with the given project details.
     */
    fun startProcessing(projectId: Long) = viewModelScope.launch {
        val project = repository.getProjectById(projectId) ?: return@launch

        _uiState.value = ProcessingUiState.Processing(progress = 0, statusMessage = "Starting…")

        // Register receiver for progress updates
        val filter = IntentFilter().apply {
            addAction(SpeechProcessingService.BROADCAST_PROGRESS)
            addAction(SpeechProcessingService.BROADCAST_COMPLETE)
            addAction(SpeechProcessingService.BROADCAST_ERROR)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(progressReceiver, filter)
        }
        receiverRegistered = true

        // Start the foreground service
        val serviceIntent = Intent(context, SpeechProcessingService::class.java).apply {
            action = SpeechProcessingService.ACTION_START_PROCESSING
            putExtra(SpeechProcessingService.EXTRA_PROJECT_ID, projectId)
            putExtra(SpeechProcessingService.EXTRA_VIDEO_URI,  project.filePath)
            putExtra(SpeechProcessingService.EXTRA_LANGUAGE,   project.language)
        }
        context.startForegroundService(serviceIntent)
    }

    /** Cancel ongoing processing */
    fun cancelProcessing() {
        val cancelIntent = Intent(context, SpeechProcessingService::class.java).apply {
            action = SpeechProcessingService.ACTION_CANCEL_PROCESSING
        }
        context.startService(cancelIntent)
        unregisterReceiver()
    }

    private fun unregisterReceiver() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(progressReceiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        unregisterReceiver()
    }
}
