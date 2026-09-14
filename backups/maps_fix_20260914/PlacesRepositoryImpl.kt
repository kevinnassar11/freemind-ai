package com.freemind.ai.data.repository

import android.util.Log
import com.freemind.ai.data.remote.places.PlacesDataSource
import com.freemind.ai.data.remote.supabase.NewFavoritePlaceDto
import com.freemind.ai.data.remote.supabase.NewPlaceSearchHistoryDto
import com.freemind.ai.data.remote.supabase.PlacesSupabaseDataSource
import com.freemind.ai.data.remote.supabase.toDomain
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.util.AppResult
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FreeMindPlaces"

@Singleton
class PlacesRepositoryImpl @Inject constructor(
    private val placesDataSource: PlacesDataSource,
    private val placesSupabaseDataSource: PlacesSupabaseDataSource,
) : PlacesRepository {

    override suspend fun searchPlaces(query: String, mode: PlaceSearchMode): AppResult<List<PlaceResult>> =
        placesDataSource.searchPlaces(query, mode.placeType)

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

    override suspend fun getFavoritePlaces(userId: String): Result<List<FavoritePlace>> = runCatching {
        placesSupabaseDataSource.getFavoritePlaces(userId).map { it.toDomain() }
    }

    override suspend fun addFavoritePlace(userId: String, place: PlaceResult): Result<Unit> = runCatching {
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
    }.onFailure { Log.w(TAG, "addFavoritePlace FAILED for placeId=\"${place.id}\"", it) }

    override suspend fun removeFavoritePlace(userId: String, placeId: String): Result<Unit> = runCatching {
        placesSupabaseDataSource.removeFavorite(placeId, userId)
    }.onFailure { Log.w(TAG, "removeFavoritePlace FAILED for placeId=\"$placeId\"", it) }

    override suspend fun clearFavoritePlaces(userId: String): Result<Unit> = runCatching {
        placesSupabaseDataSource.clearFavorites(userId)
    }
}
