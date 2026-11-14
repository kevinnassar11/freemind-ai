package com.freemind.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedPlaceSearchEntity::class, SavedItineraryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class FreeMindDatabase : RoomDatabase() {
    abstract fun placeSearchCacheDao(): PlaceSearchCacheDao
    abstract fun savedItineraryDao(): SavedItineraryDao
}

/** v1 -> v2: adds saved trip plans. The search cache is untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_itinerary` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `destination` TEXT NOT NULL, " +
                "`dayCount` INTEGER NOT NULL, `planJson` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
    }
}
