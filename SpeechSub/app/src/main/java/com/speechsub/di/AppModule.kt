package com.speechsub.di

import android.content.Context
import androidx.room.Room
import com.speechsub.data.local.AppDatabase
import com.speechsub.data.local.CaptionDao
import com.speechsub.data.local.VideoProjectDao
import com.speechsub.data.firebase.AuthService
import com.speechsub.data.firebase.FirestoreService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule — Hilt dependency injection module.
 *
 * Provides singleton instances of the database, DAOs, and services.
 * Hilt automatically injects these wherever they are @Inject-annotated.
 */
@Module
@InstallIn(SingletonComponent::class)  // Lives for the entire app lifetime
object AppModule {

    /**
     * Provides the Room database singleton.
     * Database name: "speechsub.db"
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "speechsub.db"
        )
            .fallbackToDestructiveMigration()  // During development; use proper migrations in production
            .build()

    /** Provides the VideoProject DAO from the database */
    @Provides
    @Singleton
    fun provideVideoProjectDao(db: AppDatabase): VideoProjectDao = db.videoProjectDao()

    /** Provides the Caption DAO from the database */
    @Provides
    @Singleton
    fun provideCaptionDao(db: AppDatabase): CaptionDao = db.captionDao()

    /** Provides Firebase Auth service */
    @Provides
    @Singleton
    fun provideAuthService(): AuthService = AuthService()

    /** Provides Firestore service */
    @Provides
    @Singleton
    fun provideFirestoreService(): FirestoreService = FirestoreService()
}
