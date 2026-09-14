package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import kotlinx.coroutines.flow.Flow

/** Outcome of a favorite add/remove - distinct from a plain Result so the UI can tell
 *  "written to the server", "saved locally, will retry", and "permanently failed" apart. */
sealed interface FavoriteWriteResult {
    data object Success : FavoriteWriteResult
    data object Queued : FavoriteWriteResult
    data class Failed(val error: AppError) : FavoriteWriteResult
}

interface PlacesRepository {
    suspend fun searchPlaces(query: String, mode: PlaceSearchMode): AppResult<List<PlaceResult>>

    suspend fun recordSearch(userId: String, query: String, mode: PlaceSearchMode, topResult: PlaceResult?)
    suspend fun getSearchHistory(userId: String): Result<List<PlaceSearchHistoryEntry>>
    suspend fun deleteSearchHistoryEntry(id: String, userId: String): Result<Unit>
    suspend fun clearSearchHistory(userId: String): Result<Unit>

    /** Place IDs with a favorite add/remove still waiting to reach the server. */
    val pendingFavoritePlaceIds: Flow<Set<String>>
    suspend fun getFavoritePlaces(userId: String): Result<List<FavoritePlace>>
    suspend fun addFavoritePlace(userId: String, place: PlaceResult): FavoriteWriteResult
    suspend fun removeFavoritePlace(userId: String, placeId: String): FavoriteWriteResult
    suspend fun clearFavoritePlaces(userId: String): Result<Unit>

    /** Replays any queued favorite writes - call when connectivity returns. */
    suspend fun flushPendingFavorites(userId: String)
}
