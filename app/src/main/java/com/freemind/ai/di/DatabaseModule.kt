package com.freemind.ai.di

import android.content.Context
import androidx.room.Room
import com.freemind.ai.data.local.FreeMindDatabase
import com.freemind.ai.data.local.PlaceSearchCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFreeMindDatabase(@ApplicationContext context: Context): FreeMindDatabase =
        Room.databaseBuilder(context, FreeMindDatabase::class.java, "freemind.db").build()

    @Provides
    fun providePlaceSearchCacheDao(database: FreeMindDatabase): PlaceSearchCacheDao =
        database.placeSearchCacheDao()
}
