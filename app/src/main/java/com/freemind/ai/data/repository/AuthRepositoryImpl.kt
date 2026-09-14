package com.freemind.ai.data.repository

import com.freemind.ai.data.remote.firebase.FirebaseAuthDataSource
import com.freemind.ai.domain.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
) : AuthRepository {

    override val currentUser: Flow<UserProfile?> =
        firebaseAuthDataSource.authState.map { it?.toUserProfile() }

    override val userId: String?
        get() = firebaseAuthDataSource.currentUser?.uid

    override suspend fun signInWithGoogleIdToken(googleIdToken: String): Result<UserProfile> =
        runCatching {
            firebaseAuthDataSource.signInWithGoogleIdToken(googleIdToken).toUserProfile()
        }

    override suspend fun idToken(forceRefresh: Boolean): String? =
        firebaseAuthDataSource.getIdToken(forceRefresh)

    override fun signOut() = firebaseAuthDataSource.signOut()

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        firebaseAuthDataSource.deleteAccount()
    }

    private fun FirebaseUser.toUserProfile() = UserProfile(
        userId = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
    )
}
