package com.speechsub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * AppDatabase — Room database definition.
 *
 * Lists all entities (tables) and defines the database version.
 * Increment version and add a Migration when changing the schema.
 */
@Database(
    entities = [
        VideoProjectEntity::class,
        CaptionEntity::class,
    ],
    version = 1,
    exportSchema = true   // Exports schema to schemas/ for version tracking
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoProjectDao(): VideoProjectDao
    abstract fun captionDao(): CaptionDao
}
