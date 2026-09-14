package com.freemind.ai.di

import com.freemind.ai.BuildConfig
import com.freemind.ai.data.remote.firebase.FirebaseAuthDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

/**
 * Supabase trusts Firebase as a third-party auth provider (configured on the
 * Supabase project, see SETUP.md), so there's no separate Supabase login: the
 * Firebase ID token itself is handed to Postgrest as the access token, and RLS
 * policies read the Firebase UID out of it via `(select auth.jwt()->>'sub')`.
 * This is why Supabase's own Auth/GoTrue plugin is never installed here.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(firebaseAuthDataSource: FirebaseAuthDataSource): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            accessToken = { firebaseAuthDataSource.getIdToken(forceRefresh = false) }
            install(Postgrest)
        }
}
