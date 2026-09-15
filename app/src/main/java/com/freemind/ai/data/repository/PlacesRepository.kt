package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.DeviceLocation
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.domain.model.TrendingSearch
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

/** A search's results, plus whether they came from the offline cache rather than a live call. */
data class PlaceSearchResult(val results: List<PlaceResult>, val isFromCache: Boolean)

/** A generated itinerary: the paragraph text, the real places it references (so the
 *  map can mark them), and the destination itself (so the map can recenter there).
 *  [places] and [destination] are empty/null when the text came from the
 *  `generate-itinerary` Edge Function, which returns free text with no structured
 *  place data - only the local [ItineraryComposer] fallback populates them. */
data class ItineraryResult(val text: String, val places: List<PlaceResult>, val destination: PlaceResult?)

interface PlacesRepository {
    /** [bias] is the device's position when it's known and permitted; null falls back
     *  to Google's own IP-based guess, which is what happens without the permission. */
    suspend fun searchPlaces(
        query: String,
        mode: PlaceSearchMode,
        bias: DeviceLocation? = null,
    ): AppResult<PlaceSearchResult>

    /** Turns a free-form trip-planning prompt into a single paragraph day-by-day itinerary. */
    suspend fun generateItinerary(prompt: String): AppResult<ItineraryResult>

    suspend fun recordSearch(userId: String, query: String, mode: PlaceSearchMode, topResult: PlaceResult?)
    suspend fun getSearchHistory(userId: String): Result<List<PlaceSearchHistoryEntry>>
    suspend fun deleteSearchHistoryEntry(id: String, userId: String): Result<Unit>
    suspend fun clearSearchHistory(userId: String): Result<Unit>

    /** Cross-user aggregate of recent searches - see the trending_searches security definer function. */
    suspend fun getTrendingSearches(limit: Int = 10): Result<List<TrendingSearch>>

    /** Place IDs with a favorite add/remove still waiting to reach the server. */
    val pendingFavoritePlaceIds: Flow<Set<String>>
    suspend fun getFavoritePlaces(userId: String): Result<List<FavoritePlace>>
    suspend fun addFavoritePlace(userId: String, place: PlaceResult): FavoriteWriteResult
    suspend fun removeFavoritePlace(userId: String, placeId: String): FavoriteWriteResult
    suspend fun clearFavoritePlaces(userId: String): Result<Unit>

    /** Replays any queued favorite writes - call when connectivity returns. */
    suspend fun flushPendingFavorites(userId: String)
}
