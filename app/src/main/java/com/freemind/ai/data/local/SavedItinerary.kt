package com.freemind.ai.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A trip plan the user chose to keep. Stored on the device (not in Supabase) so it
 * opens offline without re-running any search; [planJson] is the JSON-encoded
 * Itinerary. Scoped by [userId] so two accounts on one phone don't see each other's.
 */
@Entity(tableName = "saved_itinerary")
data class SavedItineraryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val destination: String,
    val dayCount: Int,
    val planJson: String,
    val savedAt: Long,
)

@Dao
interface SavedItineraryDao {
    @Query("SELECT * FROM saved_itinerary WHERE userId = :userId ORDER BY savedAt DESC")
    fun observeAll(userId: String): Flow<List<SavedItineraryEntity>>

    @Query("SELECT * FROM saved_itinerary WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun get(id: String, userId: String): SavedItineraryEntity?

    @Upsert
    suspend fun upsert(entity: SavedItineraryEntity)

    @Query("DELETE FROM saved_itinerary WHERE id = :id AND userId = :userId")
    suspend fun delete(id: String, userId: String)
}
