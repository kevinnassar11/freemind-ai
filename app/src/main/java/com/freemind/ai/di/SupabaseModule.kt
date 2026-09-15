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
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit
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
            // Ktor's OkHttp engine defaults to a ~10s socket timeout, which a plain
            // insert (recordSearch) tripped on a slow connection even though the row
            // landed server-side moments later - the client just reported it as
            // failed. Matched to NetworkModule's OkHttpClient below rather than
            // guessed, so both HTTP clients this app makes give a slow-but-alive
            // connection the same amount of rope before giving up.
            httpEngine = OkHttp.create {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
        }
}
