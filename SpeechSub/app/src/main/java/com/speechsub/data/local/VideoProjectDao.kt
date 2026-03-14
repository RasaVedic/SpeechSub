package com.speechsub.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * VideoProjectDao — Data Access Object for video projects.
 *
 * Room generates the SQL implementation automatically from these
 * annotated function signatures.
 */
@Dao
interface VideoProjectDao {

    /** Stream all projects ordered by most recently updated */
    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<VideoProjectEntity>>

    /** Get a single project by ID */
    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): VideoProjectEntity?

    /** Insert a new project and return the generated row ID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity): Long

    /** Update an existing project */
    @Update
    suspend fun updateProject(project: VideoProjectEntity)

    /** Delete a project (cascades to captions via foreign key) */
    @Delete
    suspend fun deleteProject(project: VideoProjectEntity)

    /** Mark project as processed after speech-to-text completes */
    @Query("UPDATE video_projects SET isProcessed = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsProcessed(id: Long, updatedAt: Long = System.currentTimeMillis())
}
