package com.freemind.ai.data.repository

import android.util.Log
import com.freemind.ai.data.local.PendingFavoriteAction
import com.freemind.ai.data.local.PendingFavoriteQueue
import com.freemind.ai.data.remote.places.PlacesDataSource
import com.freemind.ai.data.remote.supabase.NewFavoritePlaceDto
import com.freemind.ai.data.remote.supabase.NewPlaceSearchHistoryDto
import com.freemind.ai.data.remote.supabase.PlacesSupabaseDataSource
import com.freemind.ai.data.remote.supabase.toDomain
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FreeMindPlaces"

@Singleton
class PlacesRepositoryImpl @Inject constructor(
    private val placesDataSource: PlacesDataSource,
    private val placesSupabaseDataSource: PlacesSupabaseDataSource,
    private val pendingFavoriteQueue: PendingFavoriteQueue,
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
