package com.freemind.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedPlaceSearchEntity::class,
        SavedItineraryEntity::class,
        CachedPlaceDetailsEntity::class,
        PendingPlanDeleteEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class FreeMindDatabase : RoomDatabase() {
    abstract fun placeSearchCacheDao(): PlaceSearchCacheDao
    abstract fun savedItineraryDao(): SavedItineraryDao
    abstract fun placeDetailsCacheDao(): PlaceDetailsCacheDao
    abstract fun pendingPlanDeleteDao(): PendingPlanDeleteDao
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

/** v2 -> v3: offline place details, the queue of plan deletes waiting to sync, and a
 *  sync marker on saved plans. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saved_itinerary` ADD COLUMN `syncedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cached_place_details` (" +
                "`placeId` TEXT NOT NULL, `detailsJson` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`placeId`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_plan_delete` (" +
                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}
