package com.speechsub.data.repository

import com.speechsub.data.local.CaptionDao
import com.speechsub.data.local.CaptionEntity
import com.speechsub.data.local.VideoProjectDao
import com.speechsub.data.local.VideoProjectEntity
import com.speechsub.data.firebase.FirestoreService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CaptionRepository — single source of truth for captions and projects.
 *
 * Follows the Repository pattern from Android Architecture Guidelines.
 * The ViewModel talks ONLY to this class — never directly to DAOs or Firebase.
 *
 * Data flow:
 *   ViewModel ──► CaptionRepository ──► Room (local)
 *                                  ──► FirestoreService (optional cloud sync)
 */
@Singleton
class CaptionRepository @Inject constructor(
    private val videoProjectDao: VideoProjectDao,
    private val captionDao: CaptionDao,
    private val firestoreService: FirestoreService,
) {

    // =================== VIDEO PROJECTS ===================

    /** Stream all projects — UI collects this Flow */
    fun getAllProjects(): Flow<List<VideoProjectEntity>> =
        videoProjectDao.getAllProjects()

    /** Get a single project */
    suspend fun getProjectById(id: Long): VideoProjectEntity? =
        videoProjectDao.getProjectById(id)

    /** Create a new video project and return its generated ID */
    suspend fun createProject(project: VideoProjectEntity): Long =
        videoProjectDao.insertProject(project)

    /** Update project metadata (title, language, etc.) */
    suspend fun updateProject(project: VideoProjectEntity) =
        videoProjectDao.updateProject(project)

    /** Delete project — captions are auto-deleted via FK cascade */
    suspend fun deleteProject(project: VideoProjectEntity) =
        videoProjectDao.deleteProject(project)

    /** Mark project as processed after speech-to-text completes */
    suspend fun markProjectAsProcessed(projectId: Long) =
        videoProjectDao.markAsProcessed(projectId)

    // =================== CAPTIONS ===================

    /** Stream captions for a project — used by the editor UI */
    fun getCaptionsForProject(projectId: Long): Flow<List<CaptionEntity>> =
        captionDao.getCaptionsForProject(projectId)

    /** One-shot fetch — used by the export function */
    suspend fun getCaptionsOnce(projectId: Long): List<CaptionEntity> =
        captionDao.getCaptionsForProjectOnce(projectId)

    /** Save a single caption edit */
    suspend fun updateCaption(caption: CaptionEntity) =
        captionDao.updateCaption(caption)

    /** Insert batch of captions after recognition */
    suspend fun insertCaptions(captions: List<CaptionEntity>) =
        captionDao.insertCaptions(captions)

    /** Insert a new caption (e.g. after user splits a segment) */
    suspend fun insertCaption(caption: CaptionEntity): Long =
        captionDao.insertCaption(caption)

    /** Delete a caption */
    suspend fun deleteCaption(caption: CaptionEntity) =
        captionDao.deleteCaption(caption)

    /** Clear all captions before re-processing */
    suspend fun deleteAllCaptionsForProject(projectId: Long) =
        captionDao.deleteAllCaptionsForProject(projectId)

    // =================== CLOUD SYNC (OPTIONAL) ===================

    /**
     * syncToCloud — uploads captions to Firestore.
     * Only called when user explicitly saves to cloud.
     */
    suspend fun syncToCloud(projectId: Long, userId: String) {
        val captions = captionDao.getCaptionsForProjectOnce(projectId)
        firestoreService.saveCaptions(userId, projectId, captions)
    }

    /** Load captions from Firestore (if user moves to new device) */
    suspend fun loadFromCloud(projectId: Long, userId: String): List<CaptionEntity> =
        firestoreService.loadCaptions(userId, projectId)
}
