package com.freemind.ai.data.repository

import android.util.Log

import com.freemind.ai.data.remote.supabase.ProfileDataSource
import com.freemind.ai.data.remote.supabase.ProfileDto
import com.freemind.ai.domain.model.ThemeMode
import com.freemind.ai.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FreeMindSupabase"

/**
 * Supabase writes here are best-effort: DataStore is the source of truth for
 * settings, so a sync failure must not block the UI. That is why every call is
 * wrapped in runCatching - but a swallowed failure with no trace is how a
 * missing column went unnoticed for a whole session, so every failure is now
 * logged under [TAG].
 */
@Singleton
class SupabaseRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource,
) : SupabaseRepository {

    override suspend fun ensureProfile(profile: UserProfile): Result<Unit> = runCatching {
        profileDataSource.upsertProfile(
            ProfileDto(
                userId = profile.userId,
                email = profile.email,
                displayName = profile.displayName,
                photoUrl = profile.photoUrl,
            ),
        )
    }.onFailure {
        // Nothing works without this row - search history, favorites all key
        // off it - so this is an error, not a warning.
        Log.e(TAG, "ensureProfile FAILED for uid=${profile.userId} - no profile row was created", it)
    }

    override suspend fun setThemePref(userId: String, mode: ThemeMode): Result<Unit> = runCatching {
        profileDataSource.updateThemePref(userId, mode.name.lowercase())
    }

    override suspend fun setHistoryEnabled(userId: String, enabled: Boolean): Result<Unit> = runCatching {
        profileDataSource.updateHistoryEnabled(userId, enabled)
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> = runCatching {
        profileDataSource.updateFcmToken(userId, token)
    }

    override suspend fun deleteProfile(userId: String): Result<Unit> = runCatching {
        profileDataSource.deleteProfile(userId)
    }
}
