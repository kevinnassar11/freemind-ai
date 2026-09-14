package com.freemind.ai.data.remote.firebase

import com.freemind.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the `stamp-role` Edge Function so Supabase sees this user as
 * `authenticated` rather than `anon` (see supabase/functions/stamp-role and
 * SETUP.md). Every RLS policy in this app is `to authenticated`, so without
 * this every Supabase write - starting with the profile row itself - is
 * silently rejected.
 *
 * Same client shape as ChatEdgeFunctionClient: a thin call to a Supabase Edge
 * Function, authenticated with the caller's own Firebase ID token.
 */
@Singleton
class RoleStampClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
) {
    private val functionUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/stamp-role"

    /**
     * Returns true if the role claim is confirmed set (or already was) and
     * it's safe to force-refresh the ID token. False on any failure - the
     * caller should proceed anyway rather than block sign-in on this.
     */
    suspend fun stampRole(): Boolean = withContext(Dispatchers.IO) {
        val idToken = firebaseAuthDataSource.getIdToken(forceRefresh = false) ?: return@withContext false

        val request = Request.Builder()
            .url(functionUrl)
            .addHeader("Authorization", "Bearer $idToken")
            .post("".toRequestBody(null))
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
