package com.speechsub.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.speechsub.data.local.CaptionEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirestoreService — optional cloud backup for captions.
 *
 * Firestore structure:
 *   /users/{userId}/projects/{projectId}/captions/{captionId}
 *
 * This is fully optional — the app works 100% offline with Room.
 * Cloud sync only happens when the user explicitly requests it.
 */
@Singleton
class FirestoreService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * saveCaptions — writes all captions for a project to Firestore.
     * Uses a Firestore batch write for atomicity (all or nothing).
     */
    suspend fun saveCaptions(
        userId: String,
        projectId: Long,
        captions: List<CaptionEntity>
    ) {
        val batch = db.batch()
        val projectRef = db
            .collection("users").document(userId)
            .collection("projects").document(projectId.toString())
            .collection("captions")

        captions.forEach { caption ->
            val docRef = projectRef.document(caption.id.toString())
            batch.set(docRef, caption.toMap())
        }
        batch.commit().await()
    }

    /**
     * loadCaptions — reads captions from Firestore and maps to local entities.
     */
    suspend fun loadCaptions(userId: String, projectId: Long): List<CaptionEntity> {
        val snapshot = db
            .collection("users").document(userId)
            .collection("projects").document(projectId.toString())
            .collection("captions")
            .orderBy("index")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                CaptionEntity(
                    id          = doc.getLong("id") ?: 0L,
                    projectId   = projectId,
                    index       = (doc.getLong("index") ?: 0L).toInt(),
                    startTimeMs = doc.getLong("startTimeMs") ?: 0L,
                    endTimeMs   = doc.getLong("endTimeMs") ?: 0L,
                    text        = doc.getString("text") ?: "",
                    textColor   = doc.getString("textColor") ?: "#FFFFFF",
                    fontFamily  = doc.getString("fontFamily") ?: "inter",
                    fontSize    = (doc.getDouble("fontSize") ?: 16.0).toFloat(),
                    isBold      = doc.getBoolean("isBold") ?: false,
                    isItalic    = doc.getBoolean("isItalic") ?: false,
                )
            } catch (e: Exception) { null }
        }
    }

    /** Helper extension — converts CaptionEntity to Firestore-friendly Map */
    private fun CaptionEntity.toMap(): Map<String, Any?> = mapOf(
        "id"          to id,
        "projectId"   to projectId,
        "index"       to index,
        "startTimeMs" to startTimeMs,
        "endTimeMs"   to endTimeMs,
        "text"        to text,
        "textColor"   to textColor,
        "fontFamily"  to fontFamily,
        "fontSize"    to fontSize,
        "isBold"      to isBold,
        "isItalic"    to isItalic,
    )
}
