package com.freemind.ai.di

import android.content.Context
import androidx.room.Room
import com.freemind.ai.data.local.FreeMindDatabase
import com.freemind.ai.data.local.MIGRATION_1_2
import com.freemind.ai.data.local.PlaceSearchCacheDao
import com.freemind.ai.data.local.SavedItineraryDao
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
        Room.databaseBuilder(context, FreeMindDatabase::class.java, "freemind.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePlaceSearchCacheDao(database: FreeMindDatabase): PlaceSearchCacheDao =
        database.placeSearchCacheDao()

    @Provides
    fun provideSavedItineraryDao(database: FreeMindDatabase): SavedItineraryDao =
        database.savedItineraryDao()
}
