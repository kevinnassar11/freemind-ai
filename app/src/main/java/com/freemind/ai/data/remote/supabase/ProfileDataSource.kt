package com.freemind.ai.data.remote.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient,
) {
    private val table get() = supabaseClient.from("profiles")

    suspend fun upsertProfile(profile: ProfileDto) {
        table.upsert(profile)
    }

    suspend fun getProfile(userId: String): ProfileDto? =
        table.select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull()

    suspend fun updateThemePref(userId: String, themePref: String) {
        table.update({ set("theme_pref", themePref) }) {
            filter { eq("user_id", userId) }
        }
    }

    suspend fun updateHistoryEnabled(userId: String, enabled: Boolean) {
        table.update({ set("history_enabled", enabled) }) {
            filter { eq("user_id", userId) }
        }
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        table.update({ set("fcm_token", token) }) {
            filter { eq("user_id", userId) }
        }
    }

    suspend fun deleteProfile(userId: String) {
        table.delete {
            filter { eq("user_id", userId) }
        }
    }
}
