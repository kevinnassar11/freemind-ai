package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    val userId: String?

    suspend fun signInWithGoogleIdToken(googleIdToken: String): Result<UserProfile>
    suspend fun idToken(forceRefresh: Boolean = false): String?
    fun signOut()
    suspend fun deleteAccount(): Result<Unit>
}
