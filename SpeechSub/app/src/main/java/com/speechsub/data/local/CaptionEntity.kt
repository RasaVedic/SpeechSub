package com.speechsub.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * VideoProject — represents an imported video with its metadata.
 *
 * Stored in Room local database so users can resume editing
 * previously imported videos.
 */
@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                   // Display name (usually filename)
    val filePath: String,                // Local URI to the video file
    val durationMs: Long,                // Total video duration in milliseconds
    val language: String,                // Detected/selected language code
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false,    // Has speech-to-text been run?
    val thumbnailPath: String? = null,   // Optional cached thumbnail
)

/**
 * CaptionEntity — a single caption block (segment) with timestamp.
 *
 * Foreign key to VideoProjectEntity. Cascades delete so captions
 * are automatically removed when their project is deleted.
 */
@Entity(
    tableName = "captions",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class CaptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,                 // Parent video project
    val index: Int,                      // Order in the timeline (0-based)
    val startTimeMs: Long,               // Caption start time in ms
    val endTimeMs: Long,                 // Caption end time in ms
    val text: String,                    // The caption text (editable)
    val textColor: String = "#FFFFFF",   // Hex color for the caption text
    val fontFamily: String = "inter",    // Font to use: inter, nunito, hind
    val fontSize: Float = 16f,           // Font size in sp
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
)
