package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.DeviceLocation
import android.graphics.Bitmap
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.PlaceDetails
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

/** One row in the saved-plans list. */
data class SavedItinerarySummary(val id: String, val destination: String, val dayCount: Int, val savedAt: Long)

interface PlacesRepository {
    /** [bias] is the device's position when it's known and permitted; null falls back
     *  to Google's own IP-based guess, which is what happens without the permission. */
    suspend fun searchPlaces(
        query: String,
        mode: PlaceSearchMode,
        bias: DeviceLocation? = null,
    ): AppResult<PlaceSearchResult>

    /** Turns a free-form trip-planning prompt into a day-by-day plan built from real places. */
    suspend fun generateItinerary(prompt: String): AppResult<Itinerary>

    /** Phone, website and hours for one place - fetched on demand. */
    suspend fun getPlaceDetails(placeId: String): AppResult<PlaceDetails>

    /** A place photo, or null when unavailable. */
    suspend fun getPlacePhoto(photoReference: String): Bitmap?

    /** Plans the user chose to keep, newest first. */
    fun observeSavedItineraries(userId: String): Flow<List<SavedItinerarySummary>>
    suspend fun saveItinerary(userId: String, itinerary: Itinerary): String
    suspend fun getSavedItinerary(userId: String, id: String): Itinerary?
    suspend fun deleteSavedItinerary(userId: String, id: String)

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
