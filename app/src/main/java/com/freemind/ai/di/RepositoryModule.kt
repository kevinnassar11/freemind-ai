package com.freemind.ai.di

import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.AuthRepositoryImpl
import com.freemind.ai.data.repository.PlacesRepository
import com.freemind.ai.data.repository.PlacesRepositoryImpl
import com.freemind.ai.data.repository.SettingsRepository
import com.freemind.ai.data.repository.SettingsRepositoryImpl
import com.freemind.ai.data.repository.SupabaseRepository
import com.freemind.ai.data.repository.SupabaseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSupabaseRepository(impl: SupabaseRepositoryImpl): SupabaseRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPlacesRepository(impl: PlacesRepositoryImpl): PlacesRepository
}
