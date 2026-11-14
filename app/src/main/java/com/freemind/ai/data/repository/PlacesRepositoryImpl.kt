package com.freemind.ai.data.repository

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.freemind.ai.data.local.CachedPlaceSearchEntity
import com.freemind.ai.data.local.PendingFavoriteAction
import com.freemind.ai.data.local.PendingFavoriteQueue
import com.freemind.ai.data.local.PlaceSearchCacheDao
import com.freemind.ai.data.local.SavedItineraryDao
import com.freemind.ai.data.local.SavedItineraryEntity
import com.freemind.ai.data.remote.places.PlacesDataSource
import com.freemind.ai.data.remote.supabase.NewFavoritePlaceDto
import com.freemind.ai.data.remote.supabase.NewPlaceSearchHistoryDto
import com.freemind.ai.data.remote.supabase.PlacesSupabaseDataSource
import com.freemind.ai.data.remote.supabase.toDomain
import com.freemind.ai.domain.model.DeviceLocation
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.PlaceDetails
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.domain.model.TrendingSearch
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import com.freemind.ai.util.isKnownCountryName
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val PHOTO_CACHE_ENTRIES = 24
private const val TAG = "FreeMindPlaces"
private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000

@Singleton
class PlacesRepositoryImpl @Inject constructor(
    private val placesDataSource: PlacesDataSource,
    private val placesSupabaseDataSource: PlacesSupabaseDataSource,
    private val pendingFavoriteQueue: PendingFavoriteQueue,
    private val placeSearchCacheDao: PlaceSearchCacheDao,
    private val savedItineraryDao: SavedItineraryDao,
    private val itineraryComposer: ItineraryComposer,
) : PlacesRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun searchPlaces(
        query: String,
        mode: PlaceSearchMode,
        bias: DeviceLocation?,
    ): AppResult<PlaceSearchResult> {
        val resolution = resolveQueryForMode(query, mode)
        if (resolution is QueryResolution.Invalid) {
            return AppResult.Failure(AppError.Unknown(resolution.message))
        }
        val effectiveQuery = (resolution as QueryResolution.Valid).query

        val key = cacheKey(query, mode, bias)
        val liveResult = placesDataSource.searchPlaces(effectiveQuery, mode.placeType, bias)
        return when (liveResult) {
            is AppResult.Success -> {
                placeSearchCacheDao.upsert(
                    CachedPlaceSearchEntity(
                        cacheKey = key,
                        query = query,
                        mode = mode.name,
                        resultsJson = json.encodeToString(liveResult.data),
                        cachedAt = System.currentTimeMillis(),
                    ),
                )
                AppResult.Success(PlaceSearchResult(liveResult.data, isFromCache = false))
            }
            is AppResult.Failure -> {
                // Only a connectivity failure falls back to cache - a rate limit,
                // an unauthorized key, etc. should still surface as a real error
                // rather than silently showing possibly-stale results.
                if (liveResult.error != AppError.Network) return liveResult
                val cached = placeSearchCacheDao.get(key)
                val isFresh = cached != null && System.currentTimeMillis() - cached.cachedAt < CACHE_TTL_MS
                if (cached == null || !isFresh) return liveResult
                val cachedResults = runCatching { json.decodeFromString<List<PlaceResult>>(cached.resultsJson) }
                    .getOrNull() ?: return liveResult
                AppResult.Success(PlaceSearchResult(cachedResults, isFromCache = true))
            }
        }
    }

    /**
     * The same words mean different results in different places ("coffee near me" in
     * Beirut vs. Paris), so the area is part of the key - rounded to about 11 km so
     * walking down the street doesn't miss the cache on every search.
     */
    private fun cacheKey(query: String, mode: PlaceSearchMode, bias: DeviceLocation?): String {
        val area = bias?.let { "|%.1f,%.1f".format(it.latitude, it.longitude) } ?: ""
        return "${mode.name}|${query.trim().lowercase()}$area"
    }

    override suspend fun generateItinerary(prompt: String): AppResult<Itinerary> =
        itineraryComposer.compose(prompt)

    override suspend fun getPlaceDetails(placeId: String): AppResult<PlaceDetails> =
        placesDataSource.getPlaceDetails(placeId)

    // A details sheet re-requests its photo every time it opens; a small in-memory LRU
    // keeps reopening the same place instant without needing a disk cache.
    private val photoCache = object : LruCache<String, Bitmap>(PHOTO_CACHE_ENTRIES) {}

    override suspend fun getPlacePhoto(photoReference: String): Bitmap? =
        photoCache.get(photoReference)
            ?: placesDataSource.fetchPhoto(photoReference)?.also { photoCache.put(photoReference, it) }

    override fun observeSavedItineraries(userId: String): Flow<List<SavedItinerarySummary>> =
        savedItineraryDao.observeAll(userId).map { rows ->
            rows.map { SavedItinerarySummary(it.id, it.destination, it.dayCount, it.savedAt) }
        }

    override suspend fun saveItinerary(userId: String, itinerary: Itinerary): String {
        // One saved row per (user, prompt): saving the same plan twice updates it.
        val id = UUID.nameUUIDFromBytes("$userId|${itinerary.prompt.trim().lowercase()}".toByteArray()).toString()
        savedItineraryDao.upsert(
            SavedItineraryEntity(
                id = id,
                userId = userId,
                destination = itinerary.destinationName,
                dayCount = itinerary.days.size,
                planJson = json.encodeToString(itinerary),
                savedAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    override suspend fun getSavedItinerary(userId: String, id: String): Itinerary? =
        savedItineraryDao.get(id, userId)?.let { row ->
            runCatching { json.decodeFromString<Itinerary>(row.planJson) }.getOrNull()
        }

    override suspend fun deleteSavedItinerary(userId: String, id: String) {
        savedItineraryDao.delete(id, userId)
    }

    /**
     * Cities/Countries/Places/Culture/Shopping each only make sense for one kind of
     * input (a city name, a country name, etc.). Whether the query IS a country is
     * checked against a static country list ([isKnownCountryName]), not Google's
     * Places `types` - Places Text Search is a business/POI index, not a geocoder,
     * so the bare word "Lebanon" doesn't even surface the country, just a small town
     * that happens to share the name; classifying by `types` alone would let exactly
     * the input this feature exists to reject sail through as a "locality". Whether
     * it's a *city* (only needed to decide whether Places mode should expand to
     * "every place in that city") still comes from Places `types`, since real,
     * prominent cities are reliably tagged "locality" there.
     */
    private suspend fun resolveQueryForMode(query: String, mode: PlaceSearchMode): QueryResolution {
        val isCountry = isKnownCountryName(query)

        return when (mode) {
            PlaceSearchMode.CITIES -> if (isCountry) {
                QueryResolution.Invalid("Cities only accepts city names, like Beirut, Paris, Frankfurt, or New York - not a country.")
            } else {
                QueryResolution.Valid(query)
            }
            PlaceSearchMode.COUNTRIES -> if (!isCountry) {
                QueryResolution.Invalid("Countries only accepts country names, like Lebanon, France, or Japan.")
            } else {
                QueryResolution.Valid(query)
            }
            PlaceSearchMode.PLACES -> if (isCountry) {
                QueryResolution.Invalid("Places doesn't accept country names - try a specific place, or a city to see all its places.")
            } else if (classifyAsCity(query)) {
                QueryResolution.Valid("points of interest in $query")
            } else {
                QueryResolution.Valid(query)
            }
            PlaceSearchMode.CULTURE -> if (isCountry || classifyAsCity(query)) {
                QueryResolution.Invalid("Culture only accepts specific museums, galleries, or landmarks - not a city or country name.")
            } else {
                QueryResolution.Valid(query)
            }
            PlaceSearchMode.SHOPPING -> if (isCountry || classifyAsCity(query)) {
                QueryResolution.Invalid("Shopping only accepts specific shopping spots - not a city or country name.")
            } else {
                QueryResolution.Valid(query)
            }
            else -> QueryResolution.Valid(query)
        }
    }

    private suspend fun classifyAsCity(query: String): Boolean {
        val result = placesDataSource.searchPlaces(query, placeType = null)
        val types = (result as? AppResult.Success)?.data?.firstOrNull()?.types ?: return false
        return types.contains("locality")
    }

    private sealed interface QueryResolution {
        data class Valid(val query: String) : QueryResolution
        data class Invalid(val message: String) : QueryResolution
    }

    override suspend fun recordSearch(
        userId: String,
        query: String,
        mode: PlaceSearchMode,
        topResult: PlaceResult?,
    ) {
        runCatching {
            placesSupabaseDataSource.recordSearch(
                NewPlaceSearchHistoryDto(
                    userId = userId,
                    query = query,
                    mode = mode.name.lowercase(),
                    resultPlaceId = topResult?.id,
                    resultTitle = topResult?.title,
                    latitude = topResult?.latitude,
                    longitude = topResult?.longitude,
                ),
            )
        }.onFailure { Log.w(TAG, "recordSearch FAILED for query=\"$query\"", it) }
    }

    override suspend fun getSearchHistory(userId: String): Result<List<PlaceSearchHistoryEntry>> = runCatching {
        placesSupabaseDataSource.getSearchHistory(userId).map { it.toDomain() }
    }

    override suspend fun deleteSearchHistoryEntry(id: String, userId: String): Result<Unit> = runCatching {
        placesSupabaseDataSource.deleteSearchHistoryEntry(id, userId)
    }

    override suspend fun clearSearchHistory(userId: String): Result<Unit> = runCatching {
        placesSupabaseDataSource.clearSearchHistory(userId)
    }

    override suspend fun getTrendingSearches(limit: Int): Result<List<TrendingSearch>> = runCatching {
        placesSupabaseDataSource.getTrendingSearches(limit).map { it.toDomain() }
    }.onFailure { Log.w(TAG, "getTrendingSearches FAILED", it) }

    override val pendingFavoritePlaceIds: Flow<Set<String>> =
        pendingFavoriteQueue.pendingActions.map { actions -> actions.map { it.placeId }.toSet() }

    override suspend fun getFavoritePlaces(userId: String): Result<List<FavoritePlace>> = runCatching {
        placesSupabaseDataSource.getFavoritePlaces(userId).map { it.toDomain() }
    }

    override suspend fun addFavoritePlace(userId: String, place: PlaceResult): FavoriteWriteResult {
        val result = runCatching {
            placesSupabaseDataSource.addFavorite(
                NewFavoritePlaceDto(
                    userId = userId,
                    placeId = place.id,
                    title = place.title,
                    description = place.description,
                    latitude = place.latitude,
                    longitude = place.longitude,
                ),
            )
        }
        return result.fold(
            onSuccess = { FavoriteWriteResult.Success },
            onFailure = { error ->
                Log.w(TAG, "addFavoritePlace FAILED for placeId=\"${place.id}\"", error)
                if (isRecoverable(error)) {
                    pendingFavoriteQueue.enqueue(
                        PendingFavoriteAction.Add(
                            placeId = place.id,
                            title = place.title,
                            description = place.description,
                            latitude = place.latitude,
                            longitude = place.longitude,
                        ),
                    )
                    FavoriteWriteResult.Queued
                } else {
                    FavoriteWriteResult.Failed(error.toAppError())
                }
            },
        )
    }

    override suspend fun removeFavoritePlace(userId: String, placeId: String): FavoriteWriteResult {
        val result = runCatching { placesSupabaseDataSource.removeFavorite(placeId, userId) }
        return result.fold(
            onSuccess = { FavoriteWriteResult.Success },
            onFailure = { error ->
                Log.w(TAG, "removeFavoritePlace FAILED for placeId=\"$placeId\"", error)
                if (isRecoverable(error)) {
                    pendingFavoriteQueue.enqueue(PendingFavoriteAction.Remove(placeId))
                    FavoriteWriteResult.Queued
                } else {
                    FavoriteWriteResult.Failed(error.toAppError())
                }
            },
        )
    }

    override suspend fun clearFavoritePlaces(userId: String): Result<Unit> = runCatching {
        placesSupabaseDataSource.clearFavorites(userId)
    }

    override suspend fun flushPendingFavorites(userId: String) {
        pendingFavoriteQueue.snapshot().forEach { action ->
            val result = when (action) {
                is PendingFavoriteAction.Add -> runCatching {
                    placesSupabaseDataSource.addFavorite(
                        NewFavoritePlaceDto(
                            userId = userId,
                            placeId = action.placeId,
                            title = action.title,
                            description = action.description,
                            latitude = action.latitude,
                            longitude = action.longitude,
                        ),
                    )
                }
                is PendingFavoriteAction.Remove -> runCatching {
                    placesSupabaseDataSource.removeFavorite(action.placeId, userId)
                }
            }
            result
                .onSuccess { pendingFavoriteQueue.remove(action.placeId) }
                .onFailure { Log.w(TAG, "flushPendingFavorites retry FAILED for placeId=\"${action.placeId}\"", it) }
        }
    }

    /** Network-shaped failures are queued for retry; anything else (RLS, malformed data) is permanent. */
    private fun isRecoverable(error: Throwable): Boolean = error is HttpRequestException || error is IOException

    private fun Throwable.toAppError(): AppError = when (this) {
        is HttpRequestException -> AppError.Network
        is IOException -> AppError.Network
        else -> AppError.Unknown(message)
    }
}
