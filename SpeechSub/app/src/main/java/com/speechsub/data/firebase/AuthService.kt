package com.speechsub.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthService — Firebase Authentication wrapper.
 *
 * Provides sign-up, login, and auth state observation as Kotlin Flows.
 * The UI layer collects these flows and reacts to auth state changes.
 */
@Singleton
class AuthService @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()

    /** Observe auth state as a Flow — emits current user or null */
    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** The current user (snapshot, not reactive) */
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Is a user currently logged in? */
    val isLoggedIn: Boolean get() = auth.currentUser != null

    /**
     * signUpWithEmail — creates a new account.
     * Throws exception on failure (caught by ViewModel).
     */
    suspend fun signUpWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user ?: throw Exception("Sign-up failed: no user returned")
    }

    /**
     * signInWithEmail — logs in with existing credentials.
     */
    suspend fun signInWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw Exception("Login failed: no user returned")
    }

    /**
     * signInWithGoogle — signs in with a Google ID token.
     * Get the idToken from Google Sign-In Activity result.
     */
    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: throw Exception("Google Sign-In failed")
    }

    /**
     * sendPasswordReset — sends a password reset email.
     */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    /** Sign out the current user */
    fun signOut() = auth.signOut()
}
