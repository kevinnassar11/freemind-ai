package com.freemind.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CachedPlaceSearchEntity::class], version = 1, exportSchema = false)
abstract class FreeMindDatabase : RoomDatabase() {
    abstract fun placeSearchCacheDao(): PlaceSearchCacheDao
}
