package com.freemind.ai.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) {
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogleIdToken(googleIdToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        return requireNotNull(result.user) { "Firebase sign-in succeeded but returned no user" }
    }

    /**
     * The Firebase ID token is used directly as the Supabase access token (see
     * SupabaseModule). Force-refresh once right after sign-in, after
     * RoleStampClient confirms the `role: authenticated` custom claim is set
     * (see supabase/functions/stamp-role) - otherwise Supabase RLS sees the
     * caller as `anon` and rejects every write.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? =
        firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun deleteAccount() {
        firebaseAuth.currentUser?.delete()?.await()
    }
}
