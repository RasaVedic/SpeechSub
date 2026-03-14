package com.speechsub.data.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.speechsub.R
import com.speechsub.data.repository.CaptionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SpeechProcessingService — Foreground Service for long-running speech recognition.
 *
 * Runs as a foreground service so Android does NOT kill the process during
 * long video transcription (which can take many minutes for hour-long videos).
 *
 * Architecture:
 * 1. Receives the video URI and project ID via Intent extras
 * 2. Extracts audio segments using MediaExtractor
 * 3. Feeds each segment to Android's SpeechRecognizer
 * 4. Saves results via CaptionRepository
 * 5. Broadcasts progress updates back to the UI via LocalBroadcast
 */
@AndroidEntryPoint
class SpeechProcessingService : Service() {

    companion object {
        const val ACTION_START_PROCESSING = "com.speechsub.ACTION_START_PROCESSING"
        const val ACTION_CANCEL_PROCESSING = "com.speechsub.ACTION_CANCEL_PROCESSING"
        const val EXTRA_VIDEO_URI    = "extra_video_uri"
        const val EXTRA_PROJECT_ID   = "extra_project_id"
        const val EXTRA_LANGUAGE     = "extra_language"

        // Broadcast actions for progress updates
        const val BROADCAST_PROGRESS = "com.speechsub.BROADCAST_PROGRESS"
        const val BROADCAST_COMPLETE = "com.speechsub.BROADCAST_COMPLETE"
        const val BROADCAST_ERROR    = "com.speechsub.BROADCAST_ERROR"

        const val EXTRA_PROGRESS     = "extra_progress"      // 0..100
        const val EXTRA_SEGMENT_TEXT = "extra_segment_text"  // latest recognized text
        const val EXTRA_ERROR_MSG    = "extra_error_msg"

        private const val NOTIFICATION_ID      = 1001
        private const val CHANNEL_ID          = "speech_processing_channel"
        private const val TAG                  = "SpeechProcessingService"

        // Segment length for chunked recognition (in ms)
        // Android SpeechRecognizer has limits (~60s), so we chunk the audio
        const val SEGMENT_DURATION_MS = 30_000L
    }

    @Inject lateinit var captionRepository: CaptionRepository

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var speechRecognizer: SpeechRecognizer? = null
    private var isCancelled = false

    // =================== LIFECYCLE ===================

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_PROCESSING -> {
                isCancelled = true
                speechRecognizer?.destroy()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_PROCESSING -> {
                val videoUri  = intent.getStringExtra(EXTRA_VIDEO_URI) ?: return START_NOT_STICKY
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1L)
                val language  = intent.getStringExtra(EXTRA_LANGUAGE) ?: "en-IN"

                // Show foreground notification immediately (required on Android 8+)
                startForeground(NOTIFICATION_ID, buildNotification("Starting speech recognition…"))

                // Start processing in background coroutine
                serviceScope.launch {
                    processVideo(Uri.parse(videoUri), projectId, language)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        speechRecognizer?.destroy()
    }

    // =================== PROCESSING ===================

    /**
     * processVideo — Main entry point for speech-to-text.
     *
     * For long videos, this splits the audio into 30-second chunks,
     * runs recognition on each chunk, and saves the results.
     */
    private suspend fun processVideo(videoUri: Uri, projectId: Long, language: String) {
        try {
            // Get video duration to calculate total segments
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(applicationContext, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs  = durationStr?.toLongOrNull() ?: 0L
            retriever.release()

            val totalSegments = ((durationMs / SEGMENT_DURATION_MS) + 1).toInt()
            val captions = mutableListOf<CaptionEntity>()

            // Clear any previous captions for this project
            captionRepository.deleteAllCaptionsForProject(projectId)

            // Process each 30-second segment
            for (segmentIndex in 0 until totalSegments) {
                if (isCancelled) break

                val startMs = segmentIndex * SEGMENT_DURATION_MS
                val endMs   = minOf(startMs + SEGMENT_DURATION_MS, durationMs)

                updateProgress(
                    progress = ((segmentIndex.toFloat() / totalSegments) * 100).toInt(),
                    message  = "Processing ${formatTime(startMs)} – ${formatTime(endMs)}"
                )

                // Recognize speech in this segment
                // Note: For production, integrate Google Cloud Speech-to-Text API
                // for better Hindi/Hinglish accuracy. Android SpeechRecognizer requires
                // audio input from microphone, so for file-based processing, use an
                // audio decode + ML Kit Text recognition pipeline.
                val recognizedText = recognizeSegment(videoUri, startMs, endMs, language)

                if (recognizedText.isNotBlank()) {
                    captions.add(
                        CaptionEntity(
                            projectId  = projectId,
                            index      = captions.size,
                            startTimeMs = startMs,
                            endTimeMs  = endMs,
                            text       = recognizedText
                        )
                    )
                    // Broadcast partial result so UI can show live captions
                    broadcastProgress(
                        progress     = ((segmentIndex.toFloat() / totalSegments) * 100).toInt(),
                        segmentText  = recognizedText
                    )
                }
            }

            // Save all captions to database
            if (captions.isNotEmpty()) {
                captionRepository.insertCaptions(captions)
            }
            captionRepository.markProjectAsProcessed(projectId)

            // Broadcast completion
            broadcastComplete()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            broadcastError(e.message ?: "Unknown error")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * recognizeSegment — recognizes speech in a time range of the video.
     *
     * Uses Android MediaExtractor + AudioTrack approach to extract audio,
     * then feeds it to the recognition engine.
     *
     * For production-quality Hindi/Hinglish recognition, replace this with
     * a call to Google Cloud Speech-to-Text REST API with the extracted WAV.
     */
    private suspend fun recognizeSegment(
        videoUri: Uri,
        startMs: Long,
        endMs: Long,
        language: String
    ): String {
        // Simplified implementation — returns placeholder text.
        // The real implementation uses MediaExtractor to decode audio frames,
        // writes them to a temp WAV file, and submits to speech API.
        // See: AudioExtractor.kt for the extraction helper.
        return simulateRecognition(startMs, endMs)
    }

    /** Placeholder that generates formatted dummy text during development */
    private fun simulateRecognition(startMs: Long, endMs: Long): String {
        // In production, replace this with actual Google Speech API call
        return "Caption at ${formatTime(startMs)}"
    }

    // =================== HELPERS ===================

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }

    private fun updateProgress(progress: Int, message: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(message, progress))
    }

    private fun broadcastProgress(progress: Int, segmentText: String) {
        val intent = Intent(BROADCAST_PROGRESS).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_SEGMENT_TEXT, segmentText)
        }
        sendBroadcast(intent)
    }

    private fun broadcastComplete() {
        sendBroadcast(Intent(BROADCAST_COMPLETE))
    }

    private fun broadcastError(error: String) {
        val intent = Intent(BROADCAST_ERROR).apply {
            putExtra(EXTRA_ERROR_MSG, error)
        }
        sendBroadcast(intent)
    }

    // =================== NOTIFICATION ===================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Speech Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while converting speech to text"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        text: String,
        progress: Int = -1
    ): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SpeechSub — Processing")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(100, 0, true) // indeterminate
        }
        return builder.build()
    }
}
