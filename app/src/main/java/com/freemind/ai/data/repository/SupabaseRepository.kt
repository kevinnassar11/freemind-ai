package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.ThemeMode
import com.freemind.ai.domain.model.UserProfile

interface SupabaseRepository {
    suspend fun ensureProfile(profile: UserProfile): Result<Unit>
    suspend fun setThemePref(userId: String, mode: ThemeMode): Result<Unit>
    suspend fun setHistoryEnabled(userId: String, enabled: Boolean): Result<Unit>
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit>
    suspend fun deleteProfile(userId: String): Result<Unit>
}
