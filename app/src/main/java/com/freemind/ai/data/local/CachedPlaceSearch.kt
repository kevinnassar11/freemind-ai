package com.freemind.ai.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * A search's last successful result set, keyed by (query, mode) so the same
 * search offline can serve stale-but-useful results instead of an error.
 * resultsJson is the JSON-encoded List<PlaceResult> - see PlacesRepositoryImpl
 * for the encode/decode side; keeping it a single Room column instead of a
 * child table is fine at this size and avoids a second entity for what's
 * really one cached blob per search.
 */
@Entity(tableName = "cached_place_search")
data class CachedPlaceSearchEntity(
    @PrimaryKey val cacheKey: String,
    val query: String,
    val mode: String,
    val resultsJson: String,
    val cachedAt: Long,
)

@Dao
interface PlaceSearchCacheDao {
    @Query("SELECT * FROM cached_place_search WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): CachedPlaceSearchEntity?

    @Upsert
    suspend fun upsert(entity: CachedPlaceSearchEntity)
}
