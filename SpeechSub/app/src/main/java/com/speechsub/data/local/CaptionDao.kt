package com.speechsub.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * CaptionDao — Data Access Object for captions.
 */
@Dao
interface CaptionDao {

    /** Stream all captions for a project, ordered by timeline position */
    @Query("SELECT * FROM captions WHERE projectId = :projectId ORDER BY `index` ASC")
    fun getCaptionsForProject(projectId: Long): Flow<List<CaptionEntity>>

    /** One-shot fetch — used when exporting */
    @Query("SELECT * FROM captions WHERE projectId = :projectId ORDER BY `index` ASC")
    suspend fun getCaptionsForProjectOnce(projectId: Long): List<CaptionEntity>

    /** Insert a single caption */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaption(caption: CaptionEntity): Long

    /** Insert a batch of captions (after speech recognition) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaptions(captions: List<CaptionEntity>)

    /** Update an edited caption */
    @Update
    suspend fun updateCaption(caption: CaptionEntity)

    /** Delete a caption (e.g. user removes a segment) */
    @Delete
    suspend fun deleteCaption(caption: CaptionEntity)

    /** Delete ALL captions for a project (e.g. re-processing) */
    @Query("DELETE FROM captions WHERE projectId = :projectId")
    suspend fun deleteAllCaptionsForProject(projectId: Long)

    /** Reorder — updates the index of all captions for a project */
    @Query("UPDATE captions SET `index` = :newIndex WHERE id = :captionId")
    suspend fun updateCaptionIndex(captionId: Long, newIndex: Int)
}
