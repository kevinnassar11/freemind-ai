package com.freemind.ai.data.local

import androidx.room.ColumnInfo
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
    /** When this plan last matched the server; 0 if it has never been uploaded. Lets sync tell
     *  "never uploaded" (push it) from "uploaded, then deleted on another device" (drop it). */
    @ColumnInfo(defaultValue = "0") val syncedAt: Long = 0,
)

@Dao
interface SavedItineraryDao {
    @Query("SELECT * FROM saved_itinerary WHERE userId = :userId ORDER BY savedAt DESC")
    fun observeAll(userId: String): Flow<List<SavedItineraryEntity>>

    @Query("SELECT * FROM saved_itinerary WHERE userId = :userId")
    suspend fun getAll(userId: String): List<SavedItineraryEntity>

    @Query("DELETE FROM saved_itinerary WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("SELECT * FROM saved_itinerary WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun get(id: String, userId: String): SavedItineraryEntity?

    @Upsert
    suspend fun upsert(entity: SavedItineraryEntity)

    @Query("UPDATE saved_itinerary SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("DELETE FROM saved_itinerary WHERE id = :id AND userId = :userId")
    suspend fun delete(id: String, userId: String)
}

/**
 * A plan deleted on this device whose delete hasn't reached the server yet (offline).
 * Without this record the next sync would see the plan on the server and bring it back.
 */
@Entity(tableName = "pending_plan_delete")
data class PendingPlanDeleteEntity(
    @PrimaryKey val id: String,
    val userId: String,
)

@Dao
interface PendingPlanDeleteDao {
    @Query("SELECT * FROM pending_plan_delete WHERE userId = :userId")
    suspend fun getAll(userId: String): List<PendingPlanDeleteEntity>

    @Upsert
    suspend fun upsert(entity: PendingPlanDeleteEntity)

    @Query("DELETE FROM pending_plan_delete WHERE id = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM pending_plan_delete WHERE userId = :userId")
    suspend fun removeAll(userId: String)
}
